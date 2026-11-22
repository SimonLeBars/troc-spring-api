# 🧪 Fiche 7 — Tests

> `TP N` = notion vue à partir du TP N · ✅ à faire · ❌ à éviter · ⚠️ piège · 💡 astuce

## ⚖️ Unitaire ou intégration ? · `TP 8`

| | 🔬 Unitaire | 🔗 Intégration |
|---|---|---|
| Teste | une classe seule | toute la chaîne : HTTP → JSON → contrôleur → service → SQL |
| Dépendances | **mocks** (faux, programmés par le test) | les vraies, base H2 en mémoire |
| Spring, la base | ❌ | ✅ |
| Durée | millisecondes | secondes |
| En échec, désigne | une méthode, une règle | quelque part dans la chaîne |
| troc-api | `UserServiceTest`, `TradeServiceTest` | `UserApiTest` |

| Ce qu'on protège | Type |
|---|---|
| règles métier (stock, destinataire, statut) | 🔬 unitaire |
| contrat HTTP (URL, JSON, `201`, `404`, `409`) | 🔗 intégration |
| contraintes SQL, transaction | 🔗 intégration |

## 📁 Où ranger · `TP 8`

```
src/test/java/fr/cours/troc/user/UserServiceTest.java      ← même package que la classe testée
src/test/resources/application-test.properties             ← ⚠️ pas application.properties
```

```properties
spring.datasource.url=jdbc:h2:mem:troc-test;CASE_INSENSITIVE_IDENTIFIERS=TRUE
```

Dépendance `spring-boot-starter-webmvc-test` (`<scope>test</scope>`, déjà là) : JUnit + Mockito + MockMvc.

## ✅ JUnit · `TP 8`

```java
class UserServiceTest {

    @Test                                           // org.junit.jupiter.api.Test
    void createIgnoresIdSentByClient() {            // ✅ nom = la règle protégée · ❌ test1
        // given  : la situation de départ
        // when   : l'action testée
        // then   : le résultat attendu
    }
}
```

| Assertion (`org.junit.jupiter.api.Assertions`, import statique) | Échoue si |
|---|---|
| `assertEquals(attendu, obtenu)` | différents · ⚠️ l'**attendu d'abord** |
| `assertNull(x)` · `assertNotNull(x)` | |
| `assertTrue(condition)` · `assertFalse(…)` | |
| `var ex = assertThrows(UneException.class, () -> code());` | pas d'exception de ce type ; sinon la renvoie |

```java
var ex = assertThrows(ResponseStatusException.class, () -> service.accept(7L, 2L));
assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
```

💡 un test qu'on n'a **jamais vu échouer** ne prouve rien : casser le code, voir rouge, réparer.

## 🎭 Mockito · `TP 8`

```java
@ExtendWith(MockitoExtension.class)     // active @Mock / @InjectMocks
class TradeServiceTest {

    @Mock TradeRepository trades;       // faux, recréé avant chaque test
    @Mock ItemService itemService;
    @InjectMocks TradeService service;  // constructeur appelé avec les @Mock · non déclarés → null
}
```

| Syntaxe | Rôle |
|---|---|
| `mock(UnType.class)` | un faux : ne fait rien, renvoie `null`, `0`, `Optional.empty()`, liste vide |
| `when(faux.findById(7L)).thenReturn(Optional.of(trade))` | programmer une réponse |
| `when(faux.save(any())).thenAnswer(call -> call.getArgument(0))` | renvoyer l'argument reçu |
| `any()` · `anyInt()` · `anyLong()` | quel que soit l'argument (⚠️ `anyInt()` pour un `int`) |
| `verify(faux).methode(a, b)` | appelée une fois, avec ces arguments |
| `verify(faux, times(2)).methode(any())` | appelée deux fois |
| `verify(faux, never()).save(any())` | **jamais** appelée → « rien n'a été écrit » |

- 💡 mock possible **grâce à l'injection par constructeur** (TP 1) : le test fait lui-même l'injection
- ⚠️ Mockito est **strict** : un `when` inutilisé → `UnnecessaryStubbingException`. Ne programmer que ce qui sert **avant** l'exception.
- 💡 méthodes utilitaires privées (`proposedTrade(…)`, `item(…)`) plutôt que dix `set…` par test

## 🌐 Intégration : MockMvc · `TP 8`

```java
@SpringBootTest           // démarre tout le contexte
@AutoConfigureMockMvc     // org.springframework.boot.webmvc.test.autoconfigure
@ActiveProfiles("test")   // application-test.properties → base en mémoire
@Transactional            // ROLLBACK après chaque test
class UserApiTest {

    @Autowired MockMvc mvc;   // JUnit crée la classe, pas Spring : seul usage de @Autowired du cours

    @Test
    void unknownUserIsNotFound() throws Exception {
        mvc.perform(get("/users/999"))
                .andExpect(status().isNotFound());
    }
}
```

| Requête (`MockMvcRequestBuilders`) | |
|---|---|
| `get("/users")` · `post(…)` · `put(…)` · `delete(…)` | verbe + chemin |
| `.contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Alice\"}")` | corps JSON |
| `.param("ownerId", "1")` | `?ownerId=1` |

| Vérification (`MockMvcResultMatchers`) | |
|---|---|
| `status().isOk()` · `isCreated()` · `isNoContent()` · `isBadRequest()` · `isNotFound()` · `isConflict()` | code |
| `status().isUnauthorized()` · `isForbidden()` | `401` · `403` (TP 9) |
| `jsonPath("$.id").isNumber()` | champ de l'objet |
| `jsonPath("$.status").value("EXECUTED")` | valeur exacte |
| `jsonPath("$[*].name", hasItem("Alice"))` | un élément de la liste (`org.hamcrest.Matchers`) |
| `header().exists("Location")` | en-tête |

- ⚠️ ne pas supposer l'id `1` : les compteurs ne reviennent pas en arrière après un `ROLLBACK`
- 💡 lire une valeur : `JsonPath.read(json, "$.id")` (`com.jayway.jsonpath`, bonus)

## 🔒 Avec Spring Security · `TP 9`

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

mvc.perform(get("/users").with(jwt()))                                      // connecté
mvc.perform(post("/trades/1/accept").with(jwt().jwt(j -> j.subject("3"))))  // connecté en tant que 3
```

❌ `@WithMockUser` : pas de `Jwt` → `@AuthenticationPrincipal Jwt` reçoit `null`

## ▶️ Lancer · `TP 8`

| Où | Comment |
|---|---|
| IntelliJ, un test | ▶ vert dans la marge |
| IntelliJ, tout | clic droit sur `src/test/java` > **Run 'All Tests'** |
| IntelliJ, sous le curseur | `Ctrl+Shift+F10` (`⌃⇧R`) : la méthode ou la classe de test |
| IntelliJ, relancer | `Ctrl+F5` (`⌘R`) · bouton **Rerun Failed Tests** : seulement les rouges |
| IntelliJ, couverture | ▶ > **Run with Coverage** : lignes testées en vert, oubliées en rouge |
| Terminal | `./mvnw test` (PowerShell : `.\mvnw test`) · `./mvnw verify` avant chaque commit |

```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

✅ **tous les tests au vert avant chaque commit** (la CI le vérifie sur chaque PR)

## ⌨️ IntelliJ · `TP 8`

⭐ = aide Spring, IntelliJ **Ultimate** (licence étudiante gratuite) · raccourcis Windows / Linux ; macOS et liste générale : [fiche 10](10-raccourcis-intellij.md)

| Raccourci | Effet |
|---|---|
| `Ctrl+Shift+T` dans une classe | aller à son test · le créer (JUnit 5, méthodes cochées, bon package) |
| `Alt+Inser` dans une classe de test | **Test Method** · **SetUp Method** (`@BeforeEach`) |
| `Alt+Entrée` sur `assertEquals` rouge | **Import static method** (`org.junit.jupiter.api.Assertions`) |
| test rouge > **Click to see difference** | attendu / obtenu côte à côte |

## 🚨 Erreurs fréquentes

| Symptôme | 🔎 Cause | TP |
|---|---|:-:|
| `UnnecessaryStubbingException` | `when(…)` jamais utilisé | 8 |
| `NullPointerException` dans le service | dépendance utilisée mais pas déclarée en `@Mock` | 8 |
| `NullPointerException` sur `service` | `@ExtendWith(MockitoExtension.class)` oublié | 8 |
| `NullPointerException` sur `mvc` | `@Autowired` oublié | 8 |
| `No qualifying bean of type 'MockMvc'` | `@AutoConfigureMockMvc` oublié | 8 |
| `Cannot resolve symbol 'AutoConfigureMockMvc'` | import d'un tutoriel Boot 3 : package `org.springframework.boot.webmvc.test.autoconfigure` | 8 |
| le test écrit dans `data/troc` | `@ActiveProfiles("test")` oublié · fichier mal nommé | 8 |
| `500` « bad SQL grammar » | URL de test sans `CASE_INSENSITIVE_IDENTIFIERS=TRUE` | 8 |
| passe seul, échoue avec les autres | `@Transactional` oublié · id supposé (`/users/1`) | 8 |
| `expected: <201 CREATED> but was: <200 OK>` | le contrôleur ne renvoie pas `201` (fiche 2) | 8 |
| `A Java agent has been loaded dynamically` | Mockito : sans conséquence | 8 |
| tout en `401` après Spring Security | requêtes sans `.with(jwt())` | 9 |

## 🔗 Pour aller plus loin

[JUnit 5](https://junit.org/junit5/docs/current/user-guide/) · [Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html) · [Spring Boot — Testing](https://docs.spring.io/spring-boot/reference/testing/index.html) · [MockMvc](https://docs.spring.io/spring-framework/reference/testing/mockmvc.html) · [Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)
