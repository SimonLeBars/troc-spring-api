# Session 4 — TP bloc 2 : protéger les règles par des tests

## Objectif

En séance 3, pour vérifier le rôle de la transaction, vous avez remis la base à zéro, recréé Alice, Bob, leurs objets, proposé un troc, réduit un stock, accepté, puis regardé la base. Trois fois. Et à chaque modification du code, il faudrait tout recommencer pour être sûr de n'avoir rien cassé.

Un **test automatisé**, c'est du code qui vérifie du code. On l'écrit une fois, on le relance en quelques secondes, après chaque modification, aussi souvent qu'on veut. En séance 5, quand vous modifierez le projet à quatre en autonomie, ce sera votre filet de sécurité.

On écrit deux sortes de tests :

| | Test **unitaire** | Test d'**intégration** |
|---|---|---|
| Ce qu'il teste | une classe seule (`TradeService`) | toute la chaîne : HTTP, JSON, contrôleur, service, SQL |
| Ses dépendances | des **faux** (mocks), programmés par le test | les vraies, avec une base H2 en mémoire |
| Spring, la base | non | oui |
| Durée | quelques millisecondes | quelques secondes |
| Quand il échoue | il désigne une méthode, une règle | la cause peut être n'importe où dans la chaîne |
| Combien | beaucoup | quelques-uns |

**Objectif obligatoire de fin de bloc** :

1. `TradeServiceTest` : trois tests unitaires des règles d'acceptation (stock insuffisant, mauvais destinataire, troc déjà exécuté), tous au vert.
2. Un test d'intégration sur l'API (`UserApiTest`), au vert, sur une base de test séparée.
3. L'**expérience** de l'étape 5 réalisée : un test passe au rouge quand on casse une règle.
4. `./mvnw test` : tous les tests au vert.

**Point de départ** : le bloc 1 terminé. Si `accept` ne fonctionne pas chez vous, faites les étapes 1, 2, 3 et 6 : elles ne dépendent que de `UserService`.

> Ordre de priorité si le temps manque : les tests unitaires (étapes 1 à 5) d'abord, le test d'intégration (étape 6) ensuite. Ce support reste disponible pour la séance 5.

## Étape 1 — Ce qui est déjà en place

### 1.1 — Les dépendances

Ouvrez `pom.xml` : start.spring.io a déjà ajouté, dès la séance 1, une dépendance de test.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

Elle apporte tout ce qu'il faut :

- [**JUnit**](https://junit.org/junit5/docs/current/user-guide/) : la bibliothèque qui découvre et lance les tests, et vérifie les résultats (`assertEquals`…) ;
- [**Mockito**](https://site.mockito.org/) : la bibliothèque qui fabrique des faux objets ;
- les outils de test de Spring, dont **MockMvc** (étape 6).

`<scope>test</scope>` : ces bibliothèques ne servent qu'aux tests. Elles ne font pas partie de l'application livrée.

Si cette dépendance est absente de votre `pom.xml`, ajoutez-la, et rechargez Maven.

### 1.2 — Où ranger les tests

Les tests vivent dans `src/test/java`, **dans le même package** que la classe testée : `TradeServiceTest` dans `src/test/java/fr/cours/troc/trade/`. Avantages : le test voit les méthodes sans modificateur (*package-private*) de la classe, et IntelliJ range les tests à côté de ce qu'ils testent.

Un test y est déjà : `TrocApplicationTests`, créé par start.spring.io. Ouvrez-le : un test vide, qui vérifie seulement que l'application **démarre** (`contextLoads`). Lancez-le avec la flèche verte dans la marge : il passe.

Mais il a un défaut : il démarre l'application avec sa configuration normale, donc sur **votre base `data/troc`**. Un test qui écrirait des données les mélangerait à vos données de démonstration. Il faut une base à part.

## Étape 2 — Une base rien que pour les tests

### 2.1 — Le profil `test`

Créez le dossier `src/test/resources`, et dedans `application-test.properties` :

```properties
# Profil "test" : une base H2 en mémoire, vide à chaque lancement des tests.
# Seule l'URL change : le reste de src/main/resources/application.properties s'applique toujours.
spring.datasource.url=jdbc:h2:mem:troc-test;CASE_INSENSITIVE_IDENTIFIERS=TRUE
```

C'est un **fichier de profil** (séance 2, étape 6.5) : quand le profil `test` est actif, ses valeurs s'ajoutent à celles d'`application.properties`, et remplacent celles qui ont le même nom. Ici, seule l'URL change : `jdbc:h2:mem:` crée la base en mémoire, elle disparaît à l'arrêt. `schema.sql` est toujours exécuté au démarrage : les tables sont créées, vides.

> **Piège** : n'appelez pas ce fichier `application.properties`. Un fichier de ce nom dans `src/test/resources` **remplace entièrement** celui de `src/main/resources` pendant les tests (même nom, même emplacement pour Java) : vous perdriez toute votre configuration, `troc.trade.max-pending` compris.

### 2.2 — Activer le profil

Ajoutez `@ActiveProfiles("test")` sur `TrocApplicationTests` :

```java
@SpringBootTest
@ActiveProfiles("test") // base en mémoire, pas data/troc
class TrocApplicationTests {
```

Relancez-le. Dans la console, cherchez :

```
The following 1 profile is active: "test"
… Added connection conn0: url=jdbc:h2:mem:troc-test user=SA
```

## Étape 3 — Premier test unitaire : `UserService`, sans Spring

### 3.1 — Le retour de la séance 1

En séance 1, le couplage fort avait un quatrième problème, gardé pour aujourd'hui : **impossible de tester**. Avec `new InMemoryUserRepository()` écrit dans le service, un test n'a aucun moyen de lui glisser autre chose : il teste forcément avec le vrai stockage.

Depuis, `UserService` reçoit ses repositories **par son constructeur**. Un test peut donc lui passer ce qu'il veut, en particulier un **faux** repository, entièrement sous son contrôle.

Écrire ce faux à la main serait long : `UserRepository` étend `ListCrudRepository`, une dizaine de méthodes à implémenter pour n'en utiliser qu'une. **Mockito** l'écrit pour nous :

```java
UserRepository users = mock(UserRepository.class);           // un faux : toutes les méthodes ne font rien
when(users.save(any())).thenAnswer(call -> call.getArgument(0)); // sauf save, qui renvoie ce qu'on lui donne
```

- `mock(…)` fabrique un objet qui implémente l'interface. Par défaut, chaque méthode ne fait rien et renvoie une valeur vide (`null`, `0`, `false`, `Optional.empty()`, une liste vide).
- `when(…).thenReturn(…)` ou `.thenAnswer(…)` **programme** une réponse. `any()` : « quel que soit l'argument ».

### 3.2 — Le test

Créez `src/test/java/fr/cours/troc/user/UserServiceTest.java` :

```java
package fr.cours.troc.user;

import fr.cours.troc.item.ItemRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void createIgnoresIdSentByClient() {
        // given : un faux repository qui renvoie ce qu'on lui donne à enregistrer
        UserRepository users = mock(UserRepository.class);
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        UserService service = new UserService(users, mock(ItemRepository.class));

        User user = new User();
        user.setId(42L);
        user.setName("Alice");

        // when
        User created = service.create(user);

        // then : le nom est conservé, l'id du client est effacé (c'est la base qui l'attribue)
        assertEquals("Alice", created.getName());
        assertNull(created.getId());
    }
}
```

Adaptez si besoin à votre code : les paramètres du constructeur de `UserService`, et la règle sur l'id (séance 2, « Et si le client envoie un `id` ? »).

- `@Test` (de `org.junit.jupiter.api`) : JUnit exécute cette méthode comme un test. La classe et la méthode n'ont pas besoin d'être `public`.
- `assertEquals(attendu, obtenu)` : si les deux diffèrent, le test **échoue**. L'ordre compte : l'attendu d'abord, sinon le message d'erreur sera trompeur.
- Le test suit trois temps, **given / when / then** : la situation de départ, l'action testée, le résultat attendu.
- **Aucune trace de Spring** : pas d'annotation de Spring, pas de base. C'est le test qui appelle `new UserService(…)` : l'injection par constructeur, faite à la main, comme en séance 1.

Lancez-le (flèche verte dans la marge de la classe) : il passe, en quelques millisecondes.

### 3.3 — Le voir échouer

Un test qu'on n'a jamais vu échouer ne prouve rien. Remplacez `"Alice"` par `"Bob"` dans le `assertEquals`, relancez :

```
org.opentest4j.AssertionFailedError: expected: <Bob> but was: <Alice>
```

Le message dit ce qui était attendu, ce qui a été obtenu, et IntelliJ indique la ligne. Remettez `"Alice"`.

## Étape 4 — Les règles d'acceptation de `TradeService`

### 4.1 — Ce qu'on veut protéger

`accept` est la méthode la plus délicate du projet. Trois règles, chacune avec son code d'erreur, et une exigence commune : **rien n'est écrit** quand la règle refuse.

| Règle | Code attendu | Nom du test |
|---|---|---|
| Le stock a baissé depuis la proposition | `409` | `acceptRefusedWhenStockTooLow` |
| Celui qui accepte n'est pas le destinataire | `403` | `acceptRefusedWhenNotRecipient` |
| Le troc a déjà reçu une réponse | `409` | `acceptRefusedWhenAlreadyExecuted` |

Chaque test porte le **nom de la règle qu'il protège**. Quand il passera au rouge dans six mois, son nom doit suffire à dire ce qui est cassé : `test1` ne dit rien.

### 4.2 — Les mocks déclarés par annotations

`TradeService` a quatre dépendances. Plutôt que d'écrire `mock(…)` pour chacune, Mockito propose des annotations :

```java
package fr.cours.troc.trade;

// imports : laissez IntelliJ les ajouter (Alt+Entrée), en choisissant
// org.junit.jupiter… et org.mockito… quand il propose plusieurs classes du même nom.

@ExtendWith(MockitoExtension.class) // active les annotations de Mockito dans ce test
class TradeServiceTest {

    @Mock
    TradeRepository trades;
    @Mock
    ItemService itemService;
    @InjectMocks
    TradeService service; // construit avec les mocks ci-dessus

    // Un troc PROPOSED d'Alice (1) vers Bob (2) : offeredQuantity de l'objet offeredItemId
    // contre requestedQuantity de l'objet requestedItemId.
    private static Trade proposedTrade(Long offeredItemId, int offeredQuantity,
                                       Long requestedItemId, int requestedQuantity) {
        Trade trade = new Trade();
        trade.setId(7L);
        trade.setStatus(TradeStatus.PROPOSED);
        trade.setProposerId(1L);
        trade.setOfferedItemId(offeredItemId);
        trade.setOfferedQuantity(offeredQuantity);
        trade.setRecipientId(2L);
        trade.setRequestedItemId(requestedItemId);
        trade.setRequestedQuantity(requestedQuantity);
        return trade;
    }

    private static Item item(Long id, Long ownerId, String name, int quantity) {
        Item item = new Item();
        item.setId(id);
        item.setOwnerId(ownerId);
        item.setName(name);
        item.setQuantity(quantity);
        return item;
    }
}
```

- `@Mock` : un faux, créé avant chaque test.
- `@InjectMocks` : Mockito appelle le constructeur de `TradeService` en lui passant les mocks déclarés. C'est l'injection par constructeur, faite par Mockito au lieu de Spring. Les dépendances **non déclarées** (`UserService`, `TradeProperties`) reçoivent `null` : sans importance ici, `accept` ne s'en sert pas. Pour tester `propose`, il faudrait les déclarer aussi.
- `proposedTrade` et `item` sont de petites méthodes utilitaires : elles évitent de répéter dix lignes de `set…` dans chaque test. Adaptez-les à vos entités si vous leur avez donné des constructeurs.

### 4.3 — Premier test : stock insuffisant

C'est le scénario de l'expérience de la séance 3 : Bob a donné ses carottes ailleurs entre la proposition et l'acceptation.

```java
@Test
void acceptRefusedWhenStockTooLow() {
    // given : Bob n'a plus qu'1 carotte, le troc en demande 4
    when(trades.findById(7L)).thenReturn(Optional.of(proposedTrade(10L, 3, 20L, 4)));
    when(itemService.findById(10L)).thenReturn(item(10L, 1L, "tomates", 10));
    when(itemService.findById(20L)).thenReturn(item(20L, 2L, "carottes", 1));

    // when / then
    var ex = assertThrows(ResponseStatusException.class, () -> service.accept(7L, 2L));
    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    verify(itemService, never()).transfer(any(), anyInt(), any()); // aucun stock touché
    verify(trades, never()).save(any());                          // statut inchangé
}
```

- `assertThrows(TypeException.class, () -> …)` : exécute le code, et **échoue** s'il ne lève pas d'exception de ce type. Si l'exception est bien levée, il la renvoie : on peut vérifier son code.
- `verify(mock, never()).méthode(…)` : vérifie que la méthode n'a **jamais** été appelée. On ne vérifie plus seulement un résultat, mais un **comportement** : aucune écriture n'a eu lieu, donc `checkStock` est bien appelé **avant** toute écriture.
- `anyInt()` plutôt que `any()` pour un paramètre de type `int` : `any()` renverrait `null`, impossible à convertir en `int`.

Lancez-le : vert.

### 4.4 — À vous : les deux autres règles

Écrivez `acceptRefusedWhenNotRecipient` et `acceptRefusedWhenAlreadyExecuted`, sur le même modèle.

Attention : Mockito est **strict**. Un `when(…)` qui n'est jamais utilisé par le test fait échouer celui-ci (`UnnecessaryStubbingException`). C'est voulu : cela signale un test qui ne passe pas par où il croit. Ne programmez donc que ce que `accept` utilise **avant** de lever son exception. Relisez `accept` et `checkAcceptable` : à quel moment le mauvais destinataire est-il détecté ? `itemService.findById` a-t-il déjà été appelé ?

<details>
<summary>Solution : les deux autres règles</summary>

```java
@Test
void acceptRefusedWhenNotRecipient() {
    when(trades.findById(7L)).thenReturn(Optional.of(proposedTrade(10L, 3, 20L, 4)));

    // Alice (1) tente d'accepter sa propre proposition
    var ex = assertThrows(ResponseStatusException.class, () -> service.accept(7L, 1L));
    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    verify(trades, never()).save(any());
}

@Test
void acceptRefusedWhenAlreadyExecuted() {
    Trade trade = proposedTrade(10L, 3, 20L, 4);
    trade.setStatus(TradeStatus.EXECUTED);
    when(trades.findById(7L)).thenReturn(Optional.of(trade));

    var ex = assertThrows(ResponseStatusException.class, () -> service.accept(7L, 2L));
    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    verify(trades, never()).save(any());
}
```

`checkAcceptable` est appelé juste après `findById(tradeId)` : les objets ne sont pas encore lus. Programmer `itemService.findById` provoquerait une `UnnecessaryStubbingException`.

</details>

Lancez toute la classe (flèche verte à côté de `class TradeServiceTest`) : trois tests verts.

## Étape 5 — L'expérience : casser une règle

Dans `TradeService.accept`, mettez **temporairement** en commentaire la seconde vérification du stock, comme en séance 3 :

```java
checkStock(offered, trade.getOfferedQuantity());
// checkStock(requested, trade.getRequestedQuantity());   ← EXPÉRIENCE, à remettre
```

Relancez `TradeServiceTest`, et répondez par écrit :

1. Quel test passe au rouge ? Que dit son message ?
2. Combien de temps a-t-il fallu pour détecter la régression ? Comparez avec la séance 3 (base à remettre à zéro, données à recréer dans Bruno, requêtes SQL à relire).
3. Les deux autres tests sont-ils toujours verts ? Qu'est-ce que ça vous apprend sur l'endroit où chercher le problème ?

<details>
<summary>Ce que vous devriez observer</summary>

```
[ERROR] TradeServiceTest.acceptRefusedWhenStockTooLow Expected org.springframework.web.server.ResponseStatusException to be thrown, but nothing was thrown.
```

Sans la vérification, `accept` appelle `transfer` et `save` sur les mocks, qui ne font rien : aucune exception, `assertThrows` échoue. Le test qui échoue désigne **la règle** cassée par son nom, en une seconde, sans base ni Bruno. Les deux autres règles, toujours vertes, sont hors de cause.

C'est l'intérêt d'un test unitaire : il ne dit pas seulement « quelque chose ne va pas », il dit **quoi**.

</details>

**Remettez la vérification**, relancez : tout est vert.

> Seul `ItemService` est un mock dans ce test : la vraie contrainte `CHECK (quantity >= 0)` de la base n'existe pas ici. Le test unitaire vérifie que **le service** fait son travail, pas la base. C'est le rôle du test d'intégration de vérifier que tout fonctionne ensemble.

## Étape 6 — Un test d'intégration : l'API de bout en bout

### 6.1 — Le test

Cette fois, on démarre toute l'application, avec la base de test, et on lui envoie de **vraies requêtes HTTP**, sans lancer de serveur : MockMvc les fait passer directement au contrôleur.

Créez `src/test/java/fr/cours/troc/user/UserApiTest.java` :

```java
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
}
```

Adaptez `isCreated()` (`201`) si votre `POST /users` répond `200` (`isOk()`).

### 6.2 — Ligne par ligne

- `@SpringBootTest` : démarre l'application complète, comme `TrocApplication`, avec tous ses beans.
- `@AutoConfigureMockMvc` : met à disposition un bean `MockMvc`, le faux client HTTP. Avec Spring Boot 4, il est obligatoire et vient de `spring-boot-starter-webmvc-test`.
- `@Autowired` : jusqu'ici, on n'en a jamais eu besoin, l'injection par constructeur suffisait. Mais une classe de test est créée par **JUnit**, pas par Spring : il faut marquer explicitement le champ que Spring doit remplir. C'est l'usage courant dans les tests, et le seul endroit du cours où on l'utilise.
- `@Transactional` sur une classe de test : Spring ouvre une transaction avant chaque test, et fait un **`ROLLBACK`** à la fin au lieu d'un `COMMIT`. Alice n'existe que le temps du test : les tests ne se polluent pas entre eux, et l'ordre dans lequel ils s'exécutent n'a pas d'importance.
- `mvc.perform(post("/users")…)` : envoie la requête, exactement comme Bruno.
- `.andExpect(status().isCreated())` : vérifie le code de statut.
- `jsonPath("$.id")` : lit une valeur dans le JSON de la réponse. `$` est la racine du document, `$[*].name` « le champ `name` de chaque élément du tableau ». La syntaxe est celle de [JsonPath](https://github.com/json-path/JsonPath).

Lancez-le. C'est plus long que les tests unitaires (Spring démarre), mais toute la chaîne est vérifiée : JSON, contrôleur, service, repository, SQL. Dans la console, vous voyez passer l'`INSERT` et le `SELECT`.

### 6.3 — À vous

Ajoutez dans `UserApiTest` un test `unknownUserIsNotFound` : `GET /users/999` doit répondre `404` (`status().isNotFound()`).

Puis, si le temps le permet, un test sur `/items` : créer un utilisateur, lui créer un objet, vérifier le `201`. Indice : l'id de l'utilisateur créé n'est pas forcément `1` (d'autres tests ont pu en créer avant, même annulés). Récupérez-le dans la réponse :

```java
String json = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Alice\"}"))
        .andReturn().getResponse().getContentAsString();
Number aliceId = JsonPath.read(json, "$.id"); // com.jayway.jsonpath.JsonPath
```

## Étape 7 — Tout lancer, avant chaque commit

Dans IntelliJ, clic droit sur `src/test/java` > **Run 'All Tests'**. Ou, dans le terminal :

```
./mvnw test
```

(sous Windows, dans PowerShell : `.\mvnw test`)

```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

> **La règle de la séance 5** : tous les tests au vert avant chaque commit. À quatre sur le même code, c'est ce qui permet de modifier une partie du projet sans avoir peur de casser celle d'un autre.

### Que tester, et où ?

| Ce qu'on veut protéger | Exemples | Type de test |
|---|---|---|
| Les règles métier | stock insuffisant, mauvais destinataire, troc déjà exécuté | unitaire |
| Le contrat HTTP | URL, JSON, codes de statut `201`, `404`, `409` | intégration |
| Les contraintes SQL | FK, `CHECK`, `UNIQUE` réellement appliquées | intégration |
| La transaction | échec au milieu, stocks intacts | intégration (bonus 17) |

Savoir **pourquoi** tel test est unitaire et tel autre d'intégration fait partie de ce qu'on attend dans le rapport du projet (« détail des choix techniques »).

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `UnnecessaryStubbingException` | Un `when(…)` programmé mais jamais utilisé par le test. Retirez-le, ou vérifiez que le test passe bien par où vous croyez (étape 4.4). |
| `NullPointerException` dans `TradeService` pendant un test unitaire | Une dépendance non déclarée en `@Mock` reçoit `null` (`userService`, `props`), et la méthode testée s'en sert. Déclarez-la en `@Mock`. |
| `NullPointerException` sur `service` dans le test | `@ExtendWith(MockitoExtension.class)` oublié : les annotations `@Mock` et `@InjectMocks` ne sont pas traitées. |
| `NullPointerException` sur `mvc` | `@Autowired` oublié sur le champ `MockMvc`. |
| `No qualifying bean of type 'MockMvc'` | `@AutoConfigureMockMvc` oublié. |
| `Cannot resolve symbol 'AutoConfigureMockMvc'` | Dépendance `spring-boot-starter-webmvc-test` absente, ou import d'un ancien tutoriel : en Spring Boot 4, le package est `org.springframework.boot.webmvc.test.autoconfigure`. |
| Le test d'intégration écrit dans `data/troc` | `@ActiveProfiles("test")` oublié, ou fichier `application-test.properties` mal nommé, ou placé dans `src/main/resources`. |
| `500` « bad SQL grammar » sur toutes les requêtes des tests d'intégration | L'URL de test a perdu `;CASE_INSENSITIVE_IDENTIFIERS=TRUE`. |
| Toute la configuration semble perdue pendant les tests | Le fichier de `src/test/resources` s'appelle `application.properties` : il remplace celui de `main` (étape 2.1). |
| Un test d'intégration passe seul, échoue avec les autres | Les tests se polluent : `@Transactional` oublié sur la classe, ou un id supposé (`/users/1`) alors que la base contient déjà des données d'un autre test. |
| `expected: <201 CREATED> but was: <200 OK>` | Votre `POST` répond `200` : adaptez le test, ou le contrôleur (séance 2, étape 3). |
| Avertissement `A Java agent has been loaded dynamically` | Mockito s'attache à la JVM pour fabriquer ses faux : sans conséquence pour le cours. |

## En avance ? Features bonus

### Bonus 15 — Le cas qui marche

Les trois tests de l'étape 4 vérifient des refus. Écrivez `acceptTransfersBothItemsAndExecutesTrade` : un troc valide, et vérifiez que le statut devient `EXECUTED`, et que `transfer` a été appelé **deux fois, avec les bons arguments**.

Indices : `trades.save` doit renvoyer le troc qu'on lui passe (comme `users.save` à l'étape 3) ; `verify(itemService).transfer(tomatoes, 3, 2L)` vérifie un appel précis.

<details>
<summary>Solution</summary>

```java
@Test
void acceptTransfersBothItemsAndExecutesTrade() {
    Item tomatoes = item(10L, 1L, "tomates", 10);
    Item carrots = item(20L, 2L, "carottes", 5);
    when(trades.findById(7L)).thenReturn(Optional.of(proposedTrade(10L, 3, 20L, 4)));
    when(itemService.findById(10L)).thenReturn(tomatoes);
    when(itemService.findById(20L)).thenReturn(carrots);
    when(trades.save(any())).thenAnswer(call -> call.getArgument(0));

    Trade result = service.accept(7L, 2L);

    assertEquals(TradeStatus.EXECUTED, result.getStatus());
    verify(itemService).transfer(tomatoes, 3, 2L); // 3 tomates d'Alice vers Bob
    verify(itemService).transfer(carrots, 4, 1L);  // 4 carottes de Bob vers Alice
}
```

</details>

Puis faites l'essai : inversez `trade.getRecipientId()` et `trade.getProposerId()` dans un `transfer` de `accept`. Quel test le détecte ?

### Bonus 16 — Un troc de bout en bout par l'API

Écrivez `TradeApiTest`, un test d'intégration qui enchaîne, par MockMvc, tout le scénario de la séance 3 : créer Alice et Bob, leurs objets, proposer un troc, l'accepter (`200`, `"status": "EXECUTED"`), puis vérifier par `GET /items?ownerId=…` que les stocks ont été échangés. Puis un second test : accepter deux fois répond `409`.

C'est long à écrire : regroupez les requêtes répétées dans des méthodes utilitaires (`createUser(String name)`, qui renvoie l'id lu dans la réponse…).

### Bonus 17 — Tester le rollback

Transformez l'expérience de la séance 3 en test : si le second transfert échoue, la base doit être **exactement** dans l'état d'avant l'appel.

Pour provoquer une panne au milieu, on remplace le bean `ItemService` par un **espion** (*spy*) : un vrai `ItemService`, dont on peut reprogrammer certains appels.

```java
@MockitoSpyBean
ItemService itemService; // org.springframework.test.context.bean.override.mockito.MockitoSpyBean

// le premier transfert s'exécute pour de vrai, le second tombe en panne
doCallRealMethod().doThrow(new IllegalStateException("panne simulée"))
        .when(itemService).transfer(any(), anyInt(), any());
```

**Le piège** : ne mettez **pas** `@Transactional` sur cette classe de test. Sinon, `accept` s'exécute **dans** la transaction du test : son `ROLLBACK` n'a pas encore eu lieu quand le test vérifie la base, et il voit les écritures à moitié faites. Sans `@Transactional`, il faut vider les tables soi-même après chaque test.

Une fois le test vert, retirez `@Transactional` de `accept` : le test doit passer au rouge. Remettez-le.

<details>
<summary>Solution : TradeRollbackTest</summary>

```java
@SpringBootTest
@ActiveProfiles("test")
class TradeRollbackTest {

    @Autowired
    UserService userService;
    @Autowired
    TradeService tradeService;
    @MockitoSpyBean
    ItemService itemService;
    @Autowired
    UserRepository users;
    @Autowired
    ItemRepository items;
    @Autowired
    TradeRepository trades;

    @AfterEach
    void cleanUp() { // pas de rollback automatique : on vide les tables (enfants d'abord, à cause des FK)
        trades.deleteAll();
        items.deleteAll();
        users.deleteAll();
    }

    @Test
    void acceptRollsBackWhenSecondTransferFails() {
        // given : Alice (10 tomates) propose 3 tomates contre 4 des 5 carottes de Bob
        User alice = user("Alice");
        User bob = user("Bob");
        Item tomatoes = item(alice, "tomates", 10);
        Item carrots = item(bob, "carottes", 5);
        Trade trade = tradeService.propose(new ProposeTradeRequest(
                alice.getId(), tomatoes.getId(), 3, bob.getId(), carrots.getId(), 4));

        doCallRealMethod().doThrow(new IllegalStateException("panne simulée"))
                .when(itemService).transfer(any(), anyInt(), any());

        // when
        assertThrows(IllegalStateException.class, () -> tradeService.accept(trade.getId(), bob.getId()));

        // then : la base est exactement dans l'état d'avant l'appel
        assertEquals(10, items.findById(tomatoes.getId()).orElseThrow().getQuantity());
        assertTrue(items.findByOwnerIdAndName(bob.getId(), "tomates").isEmpty());
        assertEquals(TradeStatus.PROPOSED, trades.findById(trade.getId()).orElseThrow().getStatus());
    }

    private User user(String name) {
        User user = new User();
        user.setName(name);
        return userService.create(user);
    }

    private Item item(User owner, String name, int quantity) {
        Item item = new Item();
        item.setOwnerId(owner.getId());
        item.setName(name);
        item.setQuantity(quantity);
        return itemService.create(item);
    }
}
```

Sans `@Transactional` sur `accept` : `expected: <10> but was: <7>`, les 3 tomates d'Alice sont parties pour rien. C'est l'essai 5.2 de la séance 3, devenu reproductible en une commande.

</details>

### Bonus 18 — Premiers pas avec Spring Security

Ajoutez la dépendance `spring-boot-starter-security`, relancez, et appelez `GET /users` dans Bruno. Que se passe-t-il ? Relancez `./mvnw test` : que deviennent les tests d'intégration ?

Ajouter la dépendance suffit à **tout verrouiller** : chaque route répond `401 Unauthorized`, et un mot de passe généré s'affiche dans la console au démarrage. C'est une sécurité « fermée par défaut ». Pour aller plus loin (configurer les routes ouvertes, déclarer des utilisateurs, remplacer le `userId` du corps par l'utilisateur connecté), suivez l'annexe du cours sur Spring Security, puis le guide [Securing a Web Application](https://spring.io/guides/gs/securing-web/).

Retirez la dépendance avant de commiter si vous n'allez pas au bout : un projet où toutes les routes répondent `401` n'est plus testable dans Bruno.

## Pour aller plus loin

- [JUnit 5 — Guide de l'utilisateur](https://junit.org/junit5/docs/current/user-guide/), en particulier [les assertions](https://junit.org/junit5/docs/current/user-guide/#writing-tests-assertions)
- [Mockito — Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html) (la Javadoc de la classe `Mockito` est un vrai tutoriel)
- [Spring Boot — Testing](https://docs.spring.io/spring-boot/reference/testing/index.html) et [tester une application Spring Boot](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Spring Framework — MockMvc](https://docs.spring.io/spring-framework/reference/testing/mockmvc.html)
- [Spring Guide — Testing the Web Layer](https://spring.io/guides/gs/testing-web/)
- [Martin Fowler — The Practical Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html) (en anglais : pourquoi beaucoup de tests unitaires et peu de tests d'intégration)
