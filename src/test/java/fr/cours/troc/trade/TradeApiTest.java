package fr.cours.troc.trade;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Séance 4 : un troc par l'API, avec des utilisateurs connectés simulés (jwt() et leur subject).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TradeApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void acceptByNonRecipientIsForbidden() throws Exception {
        long tradeId = aliceProposesTomatoesForBobsCarrots();

        // Alice, la proposeuse, n'est pas la destinataire
        mvc.perform(post("/trades/" + tradeId + "/accept").with(as(aliceId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptByRecipientExchangesStocks() throws Exception {
        long tradeId = aliceProposesTomatoesForBobsCarrots();

        mvc.perform(post("/trades/" + tradeId + "/accept").with(as(bobId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // Alice : 7 tomates et 4 carottes
        mvc.perform(get("/items").param("ownerId", String.valueOf(aliceId)).with(as(aliceId)))
                .andExpect(jsonPath("$[?(@.name == 'tomates')].quantity").value(7))
                .andExpect(jsonPath("$[?(@.name == 'carottes')].quantity").value(4));

        // accepter deux fois : 409
        mvc.perform(post("/trades/" + tradeId + "/accept").with(as(bobId)))
                .andExpect(status().isConflict());
    }

    @Test
    void proposeWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/trades").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // --- Données : Alice (10 tomates) propose 3 tomates à Bob contre 4 de ses carottes (5) ---

    long aliceId;
    long bobId;

    private long aliceProposesTomatoesForBobsCarrots() throws Exception {
        aliceId = register("alice");
        bobId = register("bob");
        long tomatoes = createItem(aliceId, "tomates", 10);
        long carrots = createItem(bobId, "carottes", 5);

        String json = mvc.perform(post("/trades").with(as(aliceId)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"offeredItemId": %d, "offeredQuantity": 3,
                                 "recipientId": %d, "requestedItemId": %d, "requestedQuantity": 4}
                                """.formatted(tomatoes, bobId, carrots)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(json);
    }

    private long register(String username) throws Exception {
        String json = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "username": "%s", "password": "mot-de-passe"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(json);
    }

    private long createItem(long ownerId, String name, int quantity) throws Exception {
        String json = mvc.perform(post("/items").with(as(ownerId)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerId": %d, "name": "%s", "quantity": %d}
                                """.formatted(ownerId, name, quantity)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(json);
    }

    // Un utilisateur connecté simulé : le subject du jeton est son id, en texte.
    private static RequestPostProcessor as(long userId) {
        return jwt().jwt(j -> j.subject(String.valueOf(userId)));
    }

    private static long idOf(String json) {
        Number id = JsonPath.read(json, "$.id");
        return id.longValue();
    }
}
