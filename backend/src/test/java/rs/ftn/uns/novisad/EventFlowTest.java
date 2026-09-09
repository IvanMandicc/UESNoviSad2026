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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.ftn.uns.novisad.model.Role;
import rs.ftn.uns.novisad.model.User;
import rs.ftn.uns.novisad.repository.EventRepository;
import rs.ftn.uns.novisad.repository.LocationRepository;
import rs.ftn.uns.novisad.repository.ManagesRepository;
import rs.ftn.uns.novisad.repository.UserRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pokriva [K4] / [M1] rukovanje dogadjajima.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventFlowTest {

    private static final String ADMIN_EMAIL = "admin@test.rs";
    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String MANAGER_EMAIL = "menadzer@example.com";
    private static final String OTHER_EMAIL = "drugi@example.com";
    private static final String PASSWORD = "Lozinka123";

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ManagesRepository managesRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private long locationId;
    private Long managerId;

    @BeforeEach
    void setUp() throws Exception {
        eventRepository.deleteAll();
        managesRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.findByEmailIgnoreCase(MANAGER_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase(OTHER_EMAIL).ifPresent(userRepository::delete);

        managerId = createUser(MANAGER_EMAIL, "Menadzer", "Mesta");
        createUser(OTHER_EMAIL, "Drugi", "Korisnik");

        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        locationId = createLocation(adminToken);

        mockMvc.perform(post("/api/admin/locations/" + locationId + "/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + managerId + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("K4: menadzer mesta dodaje dogadjaj sa slikom")
    void managerCreatesEvent() throws Exception {
        String token = loginAndGetToken(MANAGER_EMAIL, PASSWORD);

        mockMvc.perform(eventMultipart(locationId, "Koncert Bajage", LocalDateTime.now().plusDays(7), false, "800.00")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Koncert Bajage"))
                .andExpect(jsonPath("$.locationId").value(locationId))
                .andExpect(jsonPath("$.freeEntry").value(false))
                .andExpect(jsonPath("$.price").value(800.00))
                .andExpect(jsonPath("$.imageUrl").exists());
    }

    @Test
    @DisplayName("K4: besplatan dogadjaj nema cenu")
    void freeEventHasNoPrice() throws Exception {
        String token = loginAndGetToken(MANAGER_EMAIL, PASSWORD);

        mockMvc.perform(eventMultipart(locationId, "Besplatna zurka", LocalDateTime.now().plusDays(3), true, null)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.freeEntry").value(true))
                .andExpect(jsonPath("$.price").doesNotExist());
    }

    @Test
    @DisplayName("K4: dogadjaj koji nije besplatan mora imati cenu")
    void paidEventRequiresPrice() throws Exception {
        String token = loginAndGetToken(MANAGER_EMAIL, PASSWORD);

        mockMvc.perform(eventMultipart(locationId, "Bez cene", LocalDateTime.now().plusDays(3), false, null)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("K4: dogadjaj bez slike se odbija")
    void rejectsEventWithoutImage() throws Exception {
        String token = loginAndGetToken(MANAGER_EMAIL, PASSWORD);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/locations/" + locationId + "/events")
                        .param("name", "Bez slike")
                        .param("address", "Neka adresa 1")
                        .param("type", "KONCERT")
                        .param("date", LocalDateTime.now().plusDays(2).format(ISO))
                        .param("regular", "false")
                        .param("freeEntry", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("K4: korisnik koji nije menadzer mesta ne moze da doda dogadjaj")
    void nonManagerCannotCreateEvent() throws Exception {
        String token = loginAndGetToken(OTHER_EMAIL, PASSWORD);

        mockMvc.perform(eventMultipart(locationId, "Nedozvoljeno", LocalDateTime.now().plusDays(2), true, null)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("K4: administrator sme da rukuje dogadjajima (nasledjuje menadzerove funkcionalnosti)")
    void adminCanCreateEvent() throws Exception {
        String token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(eventMultipart(locationId, "Admin dogadjaj", LocalDateTime.now().plusDays(2), true, null)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("K3: na mestu se prikazuju samo predstojeci dogadjaji")
    void locationShowsOnlyUpcomingEvents() throws Exception {
        String token = loginAndGetToken(MANAGER_EMAIL, PASSWORD);

        mockMvc.perform(eventMultipart(locationId, "Prosli koncert", LocalDateTime.now().minusDays(5), true, null)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
        mockMvc.perform(eventMultipart(locationId, "Buduci koncert", LocalDateTime.now().plusDays(5), true, null)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/locations/" + locationId + "/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Buduci koncert"));

        mockMvc.perform(get("/api/locations/" + locationId + "/events?all=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("K5: broji se koliko se puta redovan dogadjaj vec odrzao")
    void countsTimesHeld() throws Exception {
        String token = loginAndGetToken(MANAGER_EMAIL, PASSWORD);

        // Tri pojave istog redovnog dogadjaja: dve u proslosti, jedna u buducnosti.
        mockMvc.perform(eventMultipart(locationId, "Karaoke vece", LocalDateTime.now().minusDays(14), true, null)
                .header("Authorization", "Bearer " + token)).andExpect(status().isCreated());
        mockMvc.perform(eventMultipart(locationId, "Karaoke vece", LocalDateTime.now().minusDays(7), true, null)
                .header("Authorization", "Bearer " + token)).andExpect(status().isCreated());

        String body = mockMvc.perform(eventMultipart(locationId, "Karaoke vece", LocalDateTime.now().plusDays(7), true, null)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long upcomingId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/events/" + upcomingId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timesHeld").value(2))
                .andExpect(jsonPath("$.regular").value(true))
                .andExpect(jsonPath("$.hasTakenPlace").value(false));
    }

    @Test
    @DisplayName("K4: slika dogadjaja je javno dostupna, bez tokena")
    void eventImageIsPublic() throws Exception {
        String token = loginAndGetToken(MANAGER_EMAIL, PASSWORD);
        long eventId = createEvent(token, "Sa slikom", LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/events/" + eventId + "/image"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("K4: uklonjen dogadjaj nestaje sa liste, ali ostaje u bazi")
    void deletedEventIsHiddenButKept() throws Exception {
        String token = loginAndGetToken(MANAGER_EMAIL, PASSWORD);
        long eventId = createEvent(token, "Za brisanje", LocalDateTime.now().plusDays(4));

        mockMvc.perform(delete("/api/events/" + eventId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/locations/" + locationId + "/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));

        assertThat(eventRepository.findById(eventId)).isPresent();
    }

    @Test
    @DisplayName("K4: menadzer ne moze da dira dogadjaj na tudjem mestu")
    void managerCannotTouchForeignEvent() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        long foreignLocation = createLocation(adminToken, "Tudje mesto");
        long foreignEvent = createEvent(adminToken, "Tudji dogadjaj", LocalDateTime.now().plusDays(3), foreignLocation);

        String managerToken = loginAndGetToken(MANAGER_EMAIL, PASSWORD);

        mockMvc.perform(delete("/api/events/" + foreignEvent).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    // --- pomocne metode ---

    private Long createUser(String email, String firstName, String lastName) {
        return userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.USER)
                .enabled(true)
                .createdAt(Instant.now())
                .build()).getId();
    }

    private long createLocation(String adminToken) throws Exception {
        return createLocation(adminToken, "Studio M");
    }

    private long createLocation(String adminToken, String name) throws Exception {
        String body = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/locations")
                        .file(new MockMultipartFile("image", "slika.png", "image/png", new byte[]{1, 2, 3}))
                        .param("name", name)
                        .param("address", "Ignjata Pavlasa 3")
                        .param("type", "KONCERTNA_DVORANA")
                        .param("description", "Opis mesta.")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createEvent(String token, String name, LocalDateTime date) throws Exception {
        return createEvent(token, name, date, locationId);
    }

    private long createEvent(String token, String name, LocalDateTime date, long onLocationId) throws Exception {
        String body = mockMvc.perform(eventMultipart(onLocationId, name, date, true, null)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private static MockMultipartHttpServletRequestBuilder eventMultipart(long onLocationId,
                                                                         String name,
                                                                         LocalDateTime date,
                                                                         boolean free,
                                                                         String price) {
        MockMultipartHttpServletRequestBuilder builder =
                (MockMultipartHttpServletRequestBuilder) MockMvcRequestBuilders
                        .multipart("/api/locations/" + onLocationId + "/events")
                        .file(new MockMultipartFile("image", "poster.png", "image/png", new byte[]{9, 8, 7}))
                        .param("name", name)
                        .param("address", "Ignjata Pavlasa 3")
                        .param("type", "KONCERT")
                        .param("date", date.format(ISO))
                        .param("regular", "true")
                        .param("freeEntry", String.valueOf(free));
        if (price != null) {
            builder.param("price", price);
        }
        return builder;
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
