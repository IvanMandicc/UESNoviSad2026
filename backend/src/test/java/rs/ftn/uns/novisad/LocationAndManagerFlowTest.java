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
import rs.ftn.uns.novisad.repository.LocationRepository;
import rs.ftn.uns.novisad.repository.ManagesRepository;
import rs.ftn.uns.novisad.repository.UserRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pokriva [K3] rukovanje mestima i [A2] upravljanje menadzerima mesta.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LocationAndManagerFlowTest {

    private static final String ADMIN_EMAIL = "admin@test.rs";
    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String USER_EMAIL = "jovan@example.com";
    private static final String USER_PASSWORD = "Lozinka123";

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
    private PasswordEncoder passwordEncoder;

    private Long userId;

    @BeforeEach
    void setUp() {
        managesRepository.deleteAll();
        locationRepository.deleteAll();
        userRepository.findByEmailIgnoreCase(USER_EMAIL).ifPresent(userRepository::delete);

        User user = userRepository.save(User.builder()
                .email(USER_EMAIL)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .firstName("Jovan")
                .lastName("Jovanovic")
                .role(Role.USER)
                .enabled(true)
                .createdAt(Instant.now())
                .build());
        userId = user.getId();
    }

    @Test
    @DisplayName("K3: administrator dodaje mesto sa slikom")
    void adminCreatesLocation() throws Exception {
        String token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(locationMultipart("/api/locations", "SKC Fabrika", image())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("SKC Fabrika"))
                .andExpect(jsonPath("$.type").value("KLUB"))
                .andExpect(jsonPath("$.imageUrl").exists());
    }

    @Test
    @DisplayName("K3: mesto bez slike se odbija")
    void rejectsLocationWithoutImage() throws Exception {
        String token = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/locations")
                        .param("name", "Bez slike")
                        .param("address", "Bulevar oslobodjenja 1")
                        .param("type", "KLUB")
                        .param("description", "Opis")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("K3: obican korisnik ne moze da doda mesto")
    void regularUserCannotCreateLocation() throws Exception {
        String token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(locationMultipart("/api/locations", "Nedozvoljeno", image())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("K3: slika mesta je javno dostupna, bez tokena")
    void locationImageIsPublic() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        long locationId = createLocation(adminToken, "Studio");

        mockMvc.perform(get("/api/locations/" + locationId + "/image"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A2: administrator dodeljuje i uklanja menadzera, uloga prati stanje")
    void assignAndRemoveManager() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        long locationId = createLocation(adminToken, "Kulturni centar");

        assertThat(userRepository.findById(userId).orElseThrow().getRole()).isEqualTo(Role.USER);

        mockMvc.perform(post("/api/admin/locations/" + locationId + "/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + userId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(USER_EMAIL));

        assertThat(userRepository.findById(userId).orElseThrow().getRole()).isEqualTo(Role.MANAGER);

        mockMvc.perform(delete("/api/admin/locations/" + locationId + "/managers/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Bilo mu je jedino mesto, pa se vraca na ulogu obicnog korisnika.
        assertThat(userRepository.findById(userId).orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("K3: menadzer azurira atribute svog mesta")
    void managerUpdatesOwnLocationAttributes() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        long locationId = createLocation(adminToken, "Petrovaradinska tvrdjava");

        mockMvc.perform(post("/api/admin/locations/" + locationId + "/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + userId + "}"))
                .andExpect(status().isCreated());

        String managerToken = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(patch("/api/locations/" + locationId + "/attributes")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"address":"Tvrdjava bb","type":"OTVORENI_PROSTOR","description":"Novi opis"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Tvrdjava bb"))
                .andExpect(jsonPath("$.type").value("OTVORENI_PROSTOR"))
                // Naziv se ovim putem ne menja.
                .andExpect(jsonPath("$.name").value("Petrovaradinska tvrdjava"));
    }

    @Test
    @DisplayName("K3: menadzer ne moze da azurira tudje mesto")
    void managerCannotUpdateForeignLocation() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        long ownLocation = createLocation(adminToken, "Moje mesto");
        long foreignLocation = createLocation(adminToken, "Tudje mesto");

        mockMvc.perform(post("/api/admin/locations/" + ownLocation + "/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + userId + "}"))
                .andExpect(status().isCreated());

        String managerToken = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(patch("/api/locations/" + foreignLocation + "/attributes")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"address":"Neka adresa","type":"KLUB","description":"Opis"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("K3: menadzer ne moze da obrise mesto")
    void managerCannotDeleteLocation() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        long locationId = createLocation(adminToken, "Klub Trema");

        mockMvc.perform(post("/api/admin/locations/" + locationId + "/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + userId + "}"))
                .andExpect(status().isCreated());

        String managerToken = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(delete("/api/locations/" + locationId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("K3: uklonjeno mesto vise nije u listi (logicko brisanje)")
    void deletedLocationDisappearsFromList() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        long locationId = createLocation(adminToken, "Za brisanje");

        mockMvc.perform(delete("/api/locations/" + locationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/locations").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Podatak je i dalje u bazi, samo je neaktivan.
        assertThat(locationRepository.findById(locationId)).isPresent();
    }

    @Test
    @DisplayName("K3: duplikat naziva mesta se odbija")
    void rejectsDuplicateName() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        createLocation(adminToken, "Isti naziv");

        mockMvc.perform(locationMultipart("/api/locations", "Isti naziv", image())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("A2: administrator ne moze biti postavljen za menadzera")
    void adminCannotBeManager() throws Exception {
        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        long locationId = createLocation(adminToken, "Neko mesto");
        Long adminId = userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).orElseThrow().getId();

        mockMvc.perform(post("/api/admin/locations/" + locationId + "/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + adminId + "}"))
                .andExpect(status().isBadRequest());
    }

    // --- pomocne metode ---

    private long createLocation(String token, String name) throws Exception {
        String body = mockMvc.perform(locationMultipart("/api/locations", name, image())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private static org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
    locationMultipart(String url, String name, MockMultipartFile image) {
        return (org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder)
                MockMvcRequestBuilders.multipart(url)
                        .file(image)
                        .param("name", name)
                        .param("address", "Bulevar oslobodjenja 1")
                        .param("type", "KLUB")
                        .param("description", "Opis mesta za testiranje.");
    }

    private static MockMultipartFile image() {
        return new MockMultipartFile("image", "slika.png", "image/png", new byte[]{1, 2, 3, 4});
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
