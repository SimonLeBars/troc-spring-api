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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Séance 4 : test d'intégration, de la requête HTTP jusqu'à la base H2 en mémoire.
@SpringBootTest         // démarre tout le contexte Spring
@AutoConfigureMockMvc   // fournit un faux client HTTP
@ActiveProfiles("test") // application-test.properties : base H2 en mémoire
@Transactional          // chaque test est annulé (ROLLBACK) à la fin
class UserApiTest {

    @Autowired
    MockMvc mvc; // JUnit crée cette classe, pas Spring : on demande l'injection

    @Test
    void createdUserIsListed() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Alice")));
    }

    @Test
    void unknownUserIsNotFound() throws Exception {
        mvc.perform(get("/users/999"))
                .andExpect(status().isNotFound());
    }
}
