package rs.ftn.uns.novisad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import rs.ftn.uns.novisad.repository.AccountRequestRepository;
import rs.ftn.uns.novisad.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pokriva tok: [K1] slanje zahteva -> [A1] obrada -> [K2] prijava i odjava.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationAndLoginFlowTest {

    private static final String ADMIN_EMAIL = "admin@test.rs";
    private static final String ADMIN_PASSWORD = "Admin123!";
    private static final String USER_EMAIL = "pera.peric@example.com";
    private static final String USER_PASSWORD = "Lozinka123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRequestRepository accountRequestRepository;

    @BeforeEach
    void cleanUp() {
        accountRequestRepository.deleteAll();
        userRepository.findByEmailIgnoreCase(USER_EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("K1: zahtev za registraciju se prima i ceka obradu")
    void submitsRegistrationRequest() throws Exception {
        mockMvc.perform(post("/api/registration-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("K1: isti email ne moze dva puta da posalje zahtev")
    void rejectsDuplicateRequest() throws Exception {
        mockMvc.perform(post("/api/registration-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/registration-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("K1: neispravan unos vraca 400 sa greskama po poljima")
    void validatesRegistrationInput() throws Exception {
        String invalid = """
                {"email":"nije-email","password":"123","firstName":"","lastName":""}
                """;
        mockMvc.perform(post("/api/registration-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("K2: prijava nije moguca dok administrator ne obradi zahtev")
    void cannotLoginWhileRequestIsPending() throws Exception {
        mockMvc.perform(post("/api/registration-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(USER_EMAIL, USER_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("K1 + A1 + K2: nakon prihvatanja zahteva korisnik moze da se prijavi i odjavi")
    void fullHappyPath() throws Exception {
        String requestBody = mockMvc.perform(post("/api/registration-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long requestId = objectMapper.readTree(requestBody).get("id").asLong();

        String adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/admin/registration-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.role").value("USER"));

        String userToken = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_EMAIL));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A1: obican korisnik ne sme da pristupi listi zahteva")
    void onlyAdminCanListRequests() throws Exception {
        mockMvc.perform(get("/api/admin/registration-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("K2: pogresna lozinka vraca 401")
    void rejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(ADMIN_EMAIL, "pogresna-lozinka")))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }

    private static String registrationJson() {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "firstName": "Pera",
                  "lastName": "Peric",
                  "city": "Novi Sad",
                  "phone": "0601234567"
                }
                """.formatted(USER_EMAIL, USER_PASSWORD);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
