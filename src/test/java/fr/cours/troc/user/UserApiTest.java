package fr.cours.troc.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Séance 4 : test d'intégration, de la requête HTTP jusqu'à la base H2 en mémoire.
@SpringBootTest         // démarre tout le contexte Spring
@AutoConfigureMockMvc   // fournit un faux client HTTP
@ActiveProfiles("test") // application-test.properties : base H2 en mémoire, clé JWT de test
@Transactional          // chaque test est annulé (ROLLBACK) à la fin
class UserApiTest {

    @Autowired
    MockMvc mvc; // JUnit crée cette classe, pas Spring : on demande l'injection

    @Test
    void registeredUserIsListed() throws Exception {
        // l'inscription est ouverte : pas de jeton
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Alice\", \"username\": \"alice\", \"password\": \"tomate42\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // jwt() : un utilisateur connecté, simulé, sans jeton à fabriquer
        mvc.perform(get("/users").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItem("alice")))
                .andExpect(jsonPath("$[*].passwordHash").isEmpty()); // aucun haché dans la liste
    }

    @Test
    void unknownUserIsNotFound() throws Exception {
        mvc.perform(get("/users/999").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }
}
