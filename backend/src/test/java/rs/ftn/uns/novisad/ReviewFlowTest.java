package rs.ftn.uns.novisad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.ftn.uns.novisad.model.Role;
import rs.ftn.uns.novisad.model.User;
import rs.ftn.uns.novisad.repository.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pokriva [K5] ostavljanje utiska i [K3] srednju ocenu mesta.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReviewFlowTest {

    private static final String ADMIN_EMAIL = "admin@test.rs";
    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String USER_EMAIL = "gost@example.com";
    private static final String OTHER_EMAIL = "gost2@example.com";
    private static final String PASSWORD = "Lozinka123";

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private ManagesRepository managesRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private long locationId;
    private long pastRegularEventId;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        commentRepository.deleteAll();
        reviewRepository.deleteAll();
        eventRepository.deleteAll();
        managesRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.findByEmailIgnoreCase(USER_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase(OTHER_EMAIL).ifPresent(userRepository::delete);

        createUser(USER_EMAIL, "Gost", "Prvi");
        createUser(OTHER_EMAIL, "Gost", "Drugi");

        adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        userToken = loginAndGetToken(USER_EMAIL, PASSWORD);

        locationId = createLocation(adminToken, "Studio M");
        pastRegularEventId = createEvent("Karaoke vece", LocalDateTime.now().minusDays(7), true);
    }

    @Test
    @DisplayName("K5: korisnik ostavlja utisak sa ocenama i komentarom")
    void createsReviewWithRatesAndComment() throws Exception {
        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "performance": 9, "soundAndLight": 8,
                                 "space": 7, "overall": 10, "comment": "Odlicna atmosfera."}
                                """.formatted(pastRegularEventId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rates.PERFORMANCE").value(9))
                .andExpect(jsonPath("$.rates.SOUND_AND_LIGHT").value(8))
                .andExpect(jsonPath("$.rates.SPACE").value(7))
                .andExpect(jsonPath("$.rates.OVERALL").value(10))
                .andExpect(jsonPath("$.averageRate").value(8.5))
                .andExpect(jsonPath("$.comment").value("Odlicna atmosfera."));
    }

    @Test
    @DisplayName("K5: nije neophodno oceniti svaku stavku")
    void partialRatingIsAllowed() throws Exception {
        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "overall": 6}
                                """.formatted(pastRegularEventId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rates.OVERALL").value(6))
                .andExpect(jsonPath("$.rates.PERFORMANCE").doesNotExist())
                .andExpect(jsonPath("$.averageRate").value(6.0));
    }

    @Test
    @DisplayName("K5: komentar je opcion")
    void commentIsOptional() throws Exception {
        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "space": 5}
                                """.formatted(pastRegularEventId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").doesNotExist());
    }

    @Test
    @DisplayName("K5: bar jedna stavka mora biti ocenjena")
    void requiresAtLeastOneRate() throws Exception {
        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "comment": "Bez ocena"}
                                """.formatted(pastRegularEventId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("K5: ocena van skale 1-10 se odbija")
    void rejectsRateOutOfRange() throws Exception {
        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "overall": 11}
                                """.formatted(pastRegularEventId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.overall").exists());
    }

    @Test
    @DisplayName("K5: utisak nije moguc na dogadjaj koji nije redovan")
    void rejectsNonRegularEvent() throws Exception {
        long nonRegular = createEvent("Jednokratni koncert", LocalDateTime.now().minusDays(3), false);

        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "overall": 8}
                                """.formatted(nonRegular)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("K5: utisak nije moguc na dogadjaj koji se jos nije odrzao")
    void rejectsFutureEvent() throws Exception {
        long future = createEvent("Buduci nastup", LocalDateTime.now().plusDays(5), true);

        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "overall": 8}
                                """.formatted(future)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("K5: dogadjaj sa drugog mesta se odbija")
    void rejectsEventFromAnotherLocation() throws Exception {
        long otherLocation = createLocation(adminToken, "Drugo mesto");
        long eventElsewhere = createEvent("Tudji dogadjaj", LocalDateTime.now().minusDays(2), true, otherLocation);

        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "overall": 8}
                                """.formatted(eventElsewhere)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("K5: isti korisnik ne moze dva puta oceniti isti dogadjaj")
    void rejectsDuplicateReview() throws Exception {
        leaveReview(userToken, pastRegularEventId, 8);

        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "overall": 9}
                                """.formatted(pastRegularEventId)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("K5: na utisku stoji koliko se puta dogadjaj odrzao u tom trenutku")
    void storesTimesHeldSnapshot() throws Exception {
        // Jos dve prosle pojave istog redovnog dogadjaja - ukupno tri.
        createEvent("Karaoke vece", LocalDateTime.now().minusDays(21), true);
        createEvent("Karaoke vece", LocalDateTime.now().minusDays(14), true);

        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "overall": 7}
                                """.formatted(pastRegularEventId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timesHeldAtReview").value(3));
    }

    @Test
    @DisplayName("K5: nudi se samo redovan dogadjaj koji se odrzao i jos nije ocenjen")
    void listsReviewableEvents() throws Exception {
        createEvent("Jednokratni", LocalDateTime.now().minusDays(3), false);
        createEvent("Buduci", LocalDateTime.now().plusDays(3), true);

        mockMvc.perform(get("/api/locations/" + locationId + "/reviewable-events")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Karaoke vece"));

        leaveReview(userToken, pastRegularEventId, 8);

        // Nakon ostavljenog utiska taj dogadjaj vise nije u ponudi.
        mockMvc.perform(get("/api/locations/" + locationId + "/reviewable-events")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("K3: srednja ocena mesta je prosek svih datih ocena")
    void locationShowsAverageRating() throws Exception {
        mockMvc.perform(get("/api/locations/" + locationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.averageRating").doesNotExist())
                .andExpect(jsonPath("$.reviewCount").value(0));

        // Prvi korisnik: 10 i 8  -> zbir 18
        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": %d, "overall": 10, "space": 8}
                                """.formatted(pastRegularEventId)))
                .andExpect(status().isCreated());

        // Drugi korisnik: 6 -> ukupno (10 + 8 + 6) / 3 = 8.0
        String otherToken = loginAndGetToken(OTHER_EMAIL, PASSWORD);
        leaveReview(otherToken, pastRegularEventId, 6);

        mockMvc.perform(get("/api/locations/" + locationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(8.0))
                .andExpect(jsonPath("$.reviewCount").value(2))
                .andExpect(jsonPath("$.averageByCategory.OVERALL").value(8.0))
                .andExpect(jsonPath("$.averageByCategory.SPACE").value(8.0));
    }

    @Test
    @DisplayName("K3: srednja ocena se vidi i u listi mesta")
    void locationListShowsAverageRating() throws Exception {
        leaveReview(userToken, pastRegularEventId, 9);

        mockMvc.perform(get("/api/locations").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].averageRating").value(9.0))
                .andExpect(jsonPath("$[0].reviewCount").value(1));
    }

    @Test
    @DisplayName("K5: utisci mesta se vracaju sa autorom i dogadjajem")
    void listsReviewsForLocation() throws Exception {
        leaveReview(userToken, pastRegularEventId, 7);

        mockMvc.perform(get("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].authorName").value("Gost Prvi"))
                .andExpect(jsonPath("$[0].eventName").value("Karaoke vece"));
    }

    // --- pomocne metode ---

    private void leaveReview(String token, long eventId, int overall) throws Exception {
        mockMvc.perform(post("/api/locations/" + locationId + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\": " + eventId + ", \"overall\": " + overall + "}"))
                .andExpect(status().isCreated());
    }

    private void createUser(String email, String firstName, String lastName) {
        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.USER)
                .enabled(true)
                .createdAt(Instant.now())
                .build());
    }

    private long createLocation(String token, String name) throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/locations")
                        .file(new MockMultipartFile("image", "slika.png", "image/png", new byte[]{1, 2, 3}))
                        .param("name", name)
                        .param("address", "Ignjata Pavlasa 3")
                        .param("type", "KONCERTNA_DVORANA")
                        .param("description", "Opis mesta.")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createEvent(String name, LocalDateTime date, boolean regular) throws Exception {
        return createEvent(name, date, regular, locationId);
    }

    private long createEvent(String name, LocalDateTime date, boolean regular, long onLocationId) throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/api/locations/" + onLocationId + "/events")
                        .file(new MockMultipartFile("image", "poster.png", "image/png", new byte[]{9, 8, 7}))
                        .param("name", name)
                        .param("address", "Ignjata Pavlasa 3")
                        .param("type", "KARAOKE")
                        .param("date", date.format(ISO))
                        .param("regular", String.valueOf(regular))
                        .param("freeEntry", "true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
