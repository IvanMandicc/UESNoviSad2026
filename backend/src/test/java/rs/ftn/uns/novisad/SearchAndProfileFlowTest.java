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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pokriva [K6] pretragu i filtriranje, [K9] promenu lozinke i [K10] profil.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SearchAndProfileFlowTest {

    private static final String ADMIN_EMAIL = "admin@test.rs";
    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String USER_EMAIL = "profil@example.com";
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

    private String adminToken;
    private String userToken;
    private long klubId;
    private long dvoranaId;

    @BeforeEach
    void setUp() throws Exception {
        commentRepository.deleteAll();
        reviewRepository.deleteAll();
        eventRepository.deleteAll();
        managesRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.findByEmailIgnoreCase(USER_EMAIL).ifPresent(userRepository::delete);

        userRepository.save(User.builder()
                .email(USER_EMAIL)
                .password(passwordEncoder.encode(PASSWORD))
                .firstName("Petar")
                .lastName("Petrovic")
                .role(Role.USER)
                .enabled(true)
                .createdAt(Instant.now())
                .build());

        adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        userToken = loginAndGetToken(USER_EMAIL, PASSWORD);

        klubId = createLocation("Klub Trema", "Bulevar oslobodjenja 5", "KLUB");
        dvoranaId = createLocation("Studio M", "Ignjata Pavlasa 3", "KONCERTNA_DVORANA");
    }

    // --- [K6] pretraga mesta ---

    @Test
    @DisplayName("K6: mesta se pretrazuju po nazivu")
    void searchesLocationsByName() throws Exception {
        mockMvc.perform(get("/api/locations?query=trema").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Klub Trema"));
    }

    @Test
    @DisplayName("K6: mesta se pretrazuju po adresi")
    void searchesLocationsByAddress() throws Exception {
        mockMvc.perform(get("/api/locations?query=pavlasa").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Studio M"));
    }

    @Test
    @DisplayName("K6: mesta se filtriraju po tipu mesta")
    void filtersLocationsByType() throws Exception {
        mockMvc.perform(get("/api/locations?type=KLUB").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Klub Trema"));
    }

    @Test
    @DisplayName("K6: bez parametara se vracaju sva mesta")
    void listsAllLocationsWithoutFilters() throws Exception {
        mockMvc.perform(get("/api/locations").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // --- [K6] pretraga dogadjaja ---

    @Test
    @DisplayName("K6: bez parametara se vide danasnji dogadjaji sa svih mesta")
    void showsTodayEventsByDefault() throws Exception {
        createEvent(klubId, "Danasnja zurka", LocalDateTime.now().withHour(20).withMinute(0), "ZURKA", true, null);
        createEvent(dvoranaId, "Sutrasnji koncert", LocalDateTime.now().plusDays(1), "KONCERT", true, null);

        mockMvc.perform(get("/api/events").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Danasnja zurka"));
    }

    @Test
    @DisplayName("K6: dogadjaji se filtriraju po proizvoljnom datumu u proslosti")
    void filtersEventsByPastDate() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(10);
        createEvent(klubId, "Prosli koncert", pastDate.atTime(21, 0), "KONCERT", true, null);

        mockMvc.perform(get("/api/events?date=" + pastDate).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Prosli koncert"));
    }

    @Test
    @DisplayName("K6: dogadjaji se filtriraju po proizvoljnom datumu u buducnosti")
    void filtersEventsByFutureDate() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(20);
        createEvent(klubId, "Buduci festival", futureDate.atTime(18, 0), "FESTIVAL", true, null);

        mockMvc.perform(get("/api/events?date=" + futureDate).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Buduci festival"));
    }

    @Test
    @DisplayName("K6: dogadjaji se filtriraju po tipu i mestu")
    void filtersEventsByTypeAndLocation() throws Exception {
        createEvent(klubId, "Zurka u klubu", LocalDateTime.now().plusDays(2), "ZURKA", true, null);
        createEvent(dvoranaId, "Koncert u dvorani", LocalDateTime.now().plusDays(2), "KONCERT", true, null);

        mockMvc.perform(get("/api/events?allDates=true&type=ZURKA").header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Zurka u klubu"));

        mockMvc.perform(get("/api/events?allDates=true&locationId=" + dvoranaId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Koncert u dvorani"));
    }

    @Test
    @DisplayName("K6: dogadjaji se filtriraju po ceni i po besplatnom ulazu")
    void filtersEventsByPrice() throws Exception {
        createEvent(klubId, "Jeftin", LocalDateTime.now().plusDays(2), "KONCERT", false, "500.00");
        createEvent(klubId, "Skup", LocalDateTime.now().plusDays(3), "KONCERT", false, "3000.00");
        createEvent(klubId, "Besplatan", LocalDateTime.now().plusDays(4), "KONCERT", true, null);

        mockMvc.perform(get("/api/events?allDates=true&maxPrice=1000")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Jeftin"));

        mockMvc.perform(get("/api/events?allDates=true&minPrice=1000")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Skup"));

        mockMvc.perform(get("/api/events?allDates=true&freeEntry=true")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Besplatan"));
    }

    @Test
    @DisplayName("K6: dogadjaji se pretrazuju po adresi")
    void searchesEventsByAddress() throws Exception {
        createEvent(klubId, "Kod bulevara", LocalDateTime.now().plusDays(2), "KONCERT", true, null);

        mockMvc.perform(get("/api/events?allDates=true&query=bulevar")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Kod bulevara"));
    }

    // --- [K9] promena lozinke ---

    @Test
    @DisplayName("K9: korisnik menja lozinku i prijavljuje se novom")
    void changesPassword() throws Exception {
        mockMvc.perform(post("/api/users/me/password")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"NovaLozinka1","confirmPassword":"NovaLozinka1"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk());

        // Stara lozinka vise ne radi, nova radi.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + USER_EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());

        loginAndGetToken(USER_EMAIL, "NovaLozinka1");
    }

    @Test
    @DisplayName("K9: pogresna trenutna lozinka se odbija")
    void rejectsWrongCurrentPassword() throws Exception {
        mockMvc.perform(post("/api/users/me/password")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"pogresna","newPassword":"NovaLozinka1","confirmPassword":"NovaLozinka1"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("K9: nova lozinka i potvrda moraju da se poklapaju")
    void rejectsMismatchedConfirmation() throws Exception {
        mockMvc.perform(post("/api/users/me/password")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"NovaLozinka1","confirmPassword":"DrugaLozinka1"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("K9: nova lozinka ne sme biti ista kao trenutna")
    void rejectsSamePassword() throws Exception {
        mockMvc.perform(post("/api/users/me/password")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s","confirmPassword":"%s"}
                                """.formatted(PASSWORD, PASSWORD, PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    // --- [K10] profil ---

    @Test
    @DisplayName("K10: korisnik menja dodatne podatke na profilu")
    void updatesProfile() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Petar","lastName":"Petrovic","city":"Novi Sad","phone":"0601112223"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Novi Sad"))
                .andExpect(jsonPath("$.phone").value("0601112223"));
    }

    @Test
    @DisplayName("K10: korisnik postavlja sliku profila, koja je javno dostupna")
    void updatesProfileImage() throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/users/me/image")
                        .file(new MockMultipartFile("image", "ja.png", "image/png", new byte[]{1, 2, 3}))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").exists())
                .andReturn().getResponse().getContentAsString();

        long userId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/users/" + userId + "/image"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("K10: na profilu se vidi spisak utisaka i mesta kojima korisnik upravlja")
    void profileShowsReviewsAndManagedLocations() throws Exception {
        long userId = userRepository.findByEmailIgnoreCase(USER_EMAIL).orElseThrow().getId();

        // Redovan dogadjaj koji se odrzao, pa utisak na njega.
        long eventId = createEvent(klubId, "Karaoke", LocalDateTime.now().minusDays(3), "KARAOKE", true, null);
        mockMvc.perform(post("/api/locations/" + klubId + "/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\": " + eventId + ", \"overall\": 9}"))
                .andExpect(status().isCreated());

        // Korisnik postaje menadzer drugog mesta.
        mockMvc.perform(post("/api/admin/locations/" + dvoranaId + "/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + userId + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.reviews.length()").value(1))
                .andExpect(jsonPath("$.reviews[0].eventName").value("Karaoke"))
                .andExpect(jsonPath("$.managedLocations.length()").value(1))
                .andExpect(jsonPath("$.managedLocations[0].name").value("Studio M"));
    }

    @Test
    @DisplayName("K10: profil nije dostupan bez prijave")
    void profileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("K10: email se ne menja kroz izmenu profila")
    void emailStaysUnchanged() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Novo","lastName":"Ime","city":"Beograd","phone":"061"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_EMAIL));

        assertThat(userRepository.findByEmailIgnoreCase(USER_EMAIL)).isPresent();
    }

    // --- pomocne metode ---

    private long createLocation(String name, String address, String type) throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/locations")
                        .file(new MockMultipartFile("image", "slika.png", "image/png", new byte[]{1, 2, 3}))
                        .param("name", name)
                        .param("address", address)
                        .param("type", type)
                        .param("description", "Opis mesta.")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createEvent(long locationId, String name, LocalDateTime date,
                             String type, boolean free, String price) throws Exception {
        var builder = MockMvcRequestBuilders.multipart("/api/locations/" + locationId + "/events")
                .file(new MockMultipartFile("image", "poster.png", "image/png", new byte[]{9, 8, 7}))
                .param("name", name)
                .param("address", locationId == klubId ? "Bulevar oslobodjenja 5" : "Ignjata Pavlasa 3")
                .param("type", type)
                .param("date", date.format(ISO))
                .param("regular", "true")
                .param("freeEntry", String.valueOf(free));
        if (price != null) {
            builder.param("price", price);
        }

        String body = mockMvc.perform(builder.header("Authorization", "Bearer " + adminToken))
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
