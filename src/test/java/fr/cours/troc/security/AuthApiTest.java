package fr.cours.troc.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Séance 4 (bonus du TP 9) : de bout en bout, avec un vrai jeton fabriqué et vérifié par l'application.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void tokenFromLoginOpensProtectedRoutes() throws Exception {
        String token = registerAndLogin();

        mvc.perform(get("/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tamperedTokenIsUnauthorized() throws Exception {
        String token = registerAndLogin();
        // un caractère modifié au milieu du contenu : la signature ne correspond plus
        int middle = token.indexOf('.') + 10;
        char changed = token.charAt(middle) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, middle) + changed + token.substring(middle + 1);

        mvc.perform(get("/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        registerAndLogin();

        mvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"alice\", \"password\": \"pas-le-bon\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndLogin() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Alice\", \"username\": \"alice\", \"password\": \"tomate42\"}"))
                .andExpect(status().isCreated());

        String json = mvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"alice\", \"password\": \"tomate42\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }
}
