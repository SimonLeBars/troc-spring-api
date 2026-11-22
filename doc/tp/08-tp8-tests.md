# TP 8 — Les tests automatisés

> Rythme indicatif : séance 4. Point de départ : la mise en place du TP 6 terminée (le TP 7 n'est pas nécessaire). Si `accept` ne fonctionne pas chez vous, commencez par les tests de `UserService` et le test d'intégration : ils n'en dépendent pas.

## Le concept en bref

Un **test automatisé**, c'est du code qui vérifie du code. On l'écrit une fois, on le relance en quelques secondes, après chaque modification, aussi souvent qu'on veut. Quand le projet grossit et que plusieurs personnes le modifient, c'est le filet de sécurité qui permet de changer une partie sans casser celle d'un autre.

On écrit deux sortes de tests :

| | Test **unitaire** | Test d'**intégration** |
|---|---|---|
| Ce qu'il teste | une classe seule (`TradeService`) | toute la chaîne : HTTP, JSON, contrôleur, service, SQL |
| Ses dépendances | des **faux** (mocks), programmés par le test | les vraies, avec une base H2 en mémoire |
| Spring, la base | non | oui |
| Durée | quelques millisecondes | quelques secondes |
| Quand il échoue | il désigne une méthode, une règle | la cause peut être n'importe où dans la chaîne |
| Combien | beaucoup | quelques-uns |

---

## Partie 1 — Expérimentation : trouver une régression (facultative)

**Vous pouvez passer à la partie 2 si vous savez répondre à ces questions :**

- Combien de temps faut-il pour vérifier à la main, avec Bruno, que les règles d'acceptation d'un troc fonctionnent toujours ?
- Qu'est-ce qu'un *mock*, et pourquoi l'injection par constructeur permet-elle d'en utiliser un ?
- Pourquoi faut-il voir un test échouer au moins une fois ?

Cette partie modifie **temporairement** `TradeService` et `UserService` : remettez-les dans leur état d'origine à la fin.

### Étape 1 — Le bug caché

Une personne du groupe introduit, **sans dire où**, un bug dans `TradeService.accept` : par exemple, met en commentaire une des vérifications de stock, ou inverse destinataire et proposeur dans un transfert. Les autres doivent le trouver **avec Bruno uniquement**, en chronométrant.

Notez le temps, et tout ce qu'il a fallu faire (remettre la base à zéro, recréer les utilisateurs, les objets, proposer, réduire un stock, accepter, relire la base…). Et maintenant, imaginez refaire tout ça après **chaque** modification du projet.

Remettez `TradeService` dans son état d'origine.

### Étape 2 — Un test tout fait

Un test unitaire de `UserService`. Créez `src/test/java/fr/cours/troc/user/UserServiceTest.java` (même package que la classe testée, mais sous `src/test/java`) :

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
        // given : un faux repository, qui renvoie ce qu'on lui donne à enregistrer
        UserRepository users = mock(UserRepository.class);
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        UserService service = new UserService(users, mock(ItemRepository.class)); // adaptez à votre constructeur

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

Lancez-le avec la flèche verte dans la marge : il passe, en quelques millisecondes, sans démarrer Spring ni la base.

Introduisez maintenant un bug dans `UserService.create` : supprimez la ligne qui efface l'`id` envoyé par le client (TP 2). Relancez le test. Que dit le message ? Combien de temps a-t-il fallu ? Remettez la ligne.

### Étape 3 — Et avec le couplage fort ?

Au TP 1, la partie expérimentation annonçait un cinquième problème du couplage fort, gardé pour ce TP. Imaginez que `UserService` fasse encore `private final UserRepository users = new InMemoryUserRepository();`. Le test de l'étape 2 pourrait-il lui passer un faux repository ?

<details>
<summary>Réponse</summary>

Non : le service fabrique lui-même son repository, le test n'a aucun moyen de lui en glisser un autre. Il teste forcément avec le vrai stockage, et, une fois la base branchée, il faudrait une base pour tester la moindre règle. L'injection par constructeur rend la classe **testable** : le test fait lui-même l'injection, en passant ce qu'il veut.

</details>

### Ce qu'il faut retenir

- Vérifier à la main est lent, fastidieux, et on ne le fait donc pas après chaque modification. Un test automatisé se relance en une seconde.
- Un test unitaire bien nommé ne dit pas seulement « quelque chose ne va pas » : il dit **quoi**.
- Un test qu'on n'a jamais vu échouer ne prouve rien.
- Un **mock** est un faux objet, programmé par le test. On peut le passer à une classe parce qu'elle reçoit ses dépendances par son constructeur.

Vous pouvez garder `UserServiceTest` : il fait partie de la mise en place.

---

## Partie 2 — Mise en place : protéger les règles par des tests (obligatoire)

### Objectifs

1. Les tests d'intégration utilisent une **base de test séparée** (H2 en mémoire), jamais votre base `data/troc`.
2. `UserServiceTest` : au moins un test unitaire de `UserService` (celui de l'expérimentation convient).
3. `TradeServiceTest` : trois tests **unitaires** des règles d'acceptation, tous au vert :

   | Règle | Code attendu | Nom du test |
   |---|---|---|
   | Le stock a baissé depuis la proposition | `409` | `acceptRefusedWhenStockTooLow` |
   | Celui qui accepte n'est pas le destinataire | `403` | `acceptRefusedWhenNotRecipient` |
   | Le troc a déjà reçu une réponse | `409` | `acceptRefusedWhenAlreadyExecuted` |

   Chacun vérifie aussi l'exigence commune : quand la règle refuse, **rien n'est écrit** (aucun transfert, aucun `save`).
4. `UserApiTest` : deux tests d'**intégration** de l'API : un utilisateur créé par `POST /users` apparaît dans `GET /users` ; `GET /users/999` répond `404`.
5. `./mvnw test` : tous les tests au vert.

Chaque test porte le **nom de la règle qu'il protège** : quand il passera au rouge dans six mois, son nom doit suffire à dire ce qui est cassé. `test1` ne dit rien.

> Ordre de priorité si le temps manque : la base de test, puis les tests unitaires, puis le test d'intégration.

### Ce qui est déjà en place

Ouvrez `pom.xml` : start.spring.io a ajouté dès le départ la dépendance `spring-boot-starter-webmvc-test` (`<scope>test</scope>` : elle ne sert qu'aux tests, et ne fait pas partie de l'application livrée). Elle apporte :

- [**JUnit**](https://junit.org/junit5/docs/current/user-guide/) : découvre et lance les tests, et vérifie les résultats ;
- [**Mockito**](https://site.mockito.org/) : fabrique des faux objets ;
- les outils de test de Spring, dont **MockMvc**.

Si elle est absente, ajoutez-la et rechargez Maven.

Un test existe aussi : `TrocApplicationTests`, qui vérifie seulement que l'application **démarre** (`contextLoads`). Il démarre l'application avec sa configuration normale, donc sur **votre base `data/troc`** : un test qui écrirait des données les mélangerait aux vôtres.

### Syntaxes utiles

#### Une base pour les tests : un profil

Un fichier `src/test/resources/application-test.properties` est un **fichier de profil** (TP 3) : quand le profil `test` est actif, ses clés s'ajoutent à celles d'`application.properties`, et remplacent celles qui ont le même nom. Il suffit d'y changer l'URL de la base :

```properties
spring.datasource.url=jdbc:h2:mem:troc-test;CASE_INSENSITIVE_IDENTIFIERS=TRUE
```

`jdbc:h2:mem:` : la base vit en mémoire, et `schema.sql` y crée les tables, vides, à chaque lancement. On active le profil sur une classe de test :

```java
@SpringBootTest
@ActiveProfiles("test") // org.springframework.test.context.ActiveProfiles
class UneClasseDeTest {
```

> **Piège** : n'appelez pas ce fichier `application.properties`. Un fichier de ce nom dans `src/test/resources` **remplace entièrement** celui de `src/main/resources` pendant les tests : vous perdriez toute votre configuration.

#### JUnit

| Syntaxe | Rôle |
|---|---|
| `@Test` (`org.junit.jupiter.api`) | la méthode est un test ; ni la classe ni la méthode n'ont besoin d'être `public` |
| `assertEquals(attendu, obtenu)` | échoue si les deux diffèrent ; l'attendu **d'abord**, sinon le message sera trompeur |
| `assertNull(x)`, `assertTrue(condition)` | |
| `var ex = assertThrows(UneException.class, () -> code());` | échoue si le code ne lève pas d'exception de ce type ; sinon, la renvoie pour qu'on l'examine |

Les assertions sont des méthodes statiques de `org.junit.jupiter.api.Assertions`. Un test suit trois temps, **given / when / then** : la situation de départ, l'action testée, le résultat attendu.

#### Mockito

| Syntaxe | Rôle |
|---|---|
| `UnType faux = mock(UnType.class);` | un faux : chaque méthode ne fait rien et renvoie une valeur vide (`null`, `0`, `Optional.empty()`, liste vide) |
| `when(faux.methode(42L)).thenReturn(valeur);` | programme une réponse pour cet argument |
| `when(faux.save(any())).thenAnswer(call -> call.getArgument(0));` | programme une réponse calculée : ici, renvoyer l'argument reçu |
| `any()`, `anyInt()` | « quel que soit l'argument » ; `anyInt()` pour un paramètre `int` (`any()` renverrait `null`) |
| `verify(faux, never()).methode(any());` | vérifie que la méthode n'a **jamais** été appelée |
| `verify(faux).methode(a, b);` | vérifie qu'elle a été appelée une fois, avec ces arguments |

Les fonctions viennent de `org.mockito.Mockito` et `org.mockito.ArgumentMatchers` (imports statiques).

Pour une classe qui a plusieurs dépendances, des annotations évitent d'écrire chaque `mock(…)` :

```java
@ExtendWith(MockitoExtension.class) // active les annotations de Mockito dans ce test
class UnServiceTest {

    @Mock
    UneDependance dependance;   // un faux, recréé avant chaque test
    @InjectMocks
    UnService service;          // construit en lui passant les @Mock déclarés
}
```

`@InjectMocks` appelle le constructeur du service avec les mocks déclarés ; les dépendances **non déclarées** reçoivent `null`. Déclarez donc en `@Mock` tout ce que la méthode testée utilise.

Mockito est **strict** : un `when(…)` qui n'est jamais utilisé par le test fait échouer celui-ci (`UnnecessaryStubbingException`). C'est voulu : cela signale un test qui ne passe pas par où il croit. Ne programmez que ce que la méthode utilise **avant** de lever son exception.

#### Un test d'intégration avec MockMvc

```java
@SpringBootTest         // démarre tout le contexte Spring, comme TrocApplication
@AutoConfigureMockMvc   // org.springframework.boot.webmvc.test.autoconfigure : fournit un faux client HTTP
@ActiveProfiles("test")
@Transactional          // chaque test est annulé (ROLLBACK) à la fin
class UneApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void unScenario() throws Exception {
        mvc.perform(post("/chemin").contentType(MediaType.APPLICATION_JSON).content("{\"cle\": \"valeur\"}"))
                .andExpect(status().isCreated())            // isOk(), isNotFound(), isConflict()…
                .andExpect(jsonPath("$.id").isNumber());
    }
}
```

- MockMvc envoie de vraies requêtes au contrôleur, sans lancer de serveur. `get(…)`, `post(…)` viennent de `MockMvcRequestBuilders`, `status()` et `jsonPath(…)` de `MockMvcResultMatchers` (imports statiques).
- `jsonPath("$.id")` lit une valeur dans le JSON de la réponse : `$` est la racine, `$[*].name` « le champ `name` de chaque élément du tableau ». Avec `hasItem("Alice")` (`org.hamcrest.Matchers`), on vérifie qu'une liste contient une valeur. Syntaxe : [JsonPath](https://github.com/json-path/JsonPath).
- `@Autowired` : jusqu'ici, l'injection par constructeur suffisait. Mais une classe de test est créée par **JUnit**, pas par Spring : il faut marquer le champ que Spring doit remplir. C'est l'usage courant dans les tests, et le seul endroit du cours où on l'utilise.
- `@Transactional` sur une classe de test : Spring fait un **`ROLLBACK`** à la fin de chaque test au lieu d'un `COMMIT`. Les tests ne se polluent pas entre eux, et leur ordre n'a pas d'importance. Ne supposez pas pour autant qu'un utilisateur créé aura l'id `1` : les compteurs de la base, eux, ne reviennent pas en arrière.

### Indices

- Pour `TradeServiceTest`, de petites méthodes utilitaires privées (`proposedTrade(…)`, `item(…)`) évitent de répéter dix lignes de `set…` dans chaque test.
- Relisez `accept` et ses vérifications : à quel moment le mauvais destinataire est-il détecté ? Les objets ont-ils déjà été lus ? Ne programmez que ce qui est utilisé.
- Vérifier qu'aucune écriture n'a eu lieu, c'est vérifier un **comportement** : la vérification est bien faite **avant** toute écriture.
- Le test unitaire de `accept` ne voit pas la base : la contrainte `CHECK` n'existe pas ici. Il vérifie que **le service** fait son travail.

### Vérifier

#### La base de test

Lancez `TrocApplicationTests` après lui avoir ajouté `@ActiveProfiles("test")`. Dans la console, cherchez :

```
The following 1 profile is active: "test"
… url=jdbc:h2:mem:troc-test …
```

#### Casser une règle

Dans `TradeService.accept`, mettez **temporairement** en commentaire la vérification du stock demandé. Relancez `TradeServiceTest`, et répondez par écrit :

1. Quel test passe au rouge ? Que dit son message ?
2. Les deux autres tests sont-ils toujours verts ? Qu'est-ce que ça vous apprend sur l'endroit où chercher le problème ?

Remettez la vérification : tout est vert.

#### Tout lancer

Dans IntelliJ, clic droit sur `src/test/java` > **Run 'All Tests'**, ou dans le terminal :

```
./mvnw test
```

(sous Windows, dans PowerShell : `.\mvnw test`)

```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

> **La règle pour la suite du projet** : tous les tests au vert avant chaque commit.

- [ ] `application-test.properties` existe dans `src/test/resources`, et les tests d'intégration ne touchent pas `data/troc`.
- [ ] Les trois tests de `TradeServiceTest` sont verts, et vérifient aussi qu'aucune écriture n'a eu lieu.
- [ ] Le test cassé volontairement a été vu au rouge, puis remis au vert.
- [ ] `UserApiTest` contient les deux tests d'intégration demandés, verts.
- [ ] `./mvnw test` : `BUILD SUCCESS`.

#### Que tester, et où ?

| Ce qu'on veut protéger | Exemples | Type de test |
|---|---|---|
| Les règles métier | stock insuffisant, mauvais destinataire, troc déjà exécuté | unitaire |
| Le contrat HTTP | URL, JSON, codes de statut `201`, `404`, `409` | intégration |
| Les contraintes SQL | FK, `CHECK`, `UNIQUE` réellement appliquées | intégration |
| La transaction | échec au milieu, stocks intacts | intégration (bonus 3) |

Savoir **pourquoi** tel test est unitaire et tel autre d'intégration fait partie de ce qu'on attend dans le rapport du projet.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `UnnecessaryStubbingException` | Un `when(…)` programmé mais jamais utilisé par le test. |
| `NullPointerException` dans le service pendant un test unitaire | Une dépendance non déclarée en `@Mock` reçoit `null`, et la méthode testée s'en sert. |
| `NullPointerException` sur `service` dans le test | `@ExtendWith(MockitoExtension.class)` oublié. |
| `NullPointerException` sur `mvc` | `@Autowired` oublié sur le champ `MockMvc`. |
| `No qualifying bean of type 'MockMvc'` | `@AutoConfigureMockMvc` oublié. |
| `Cannot resolve symbol 'AutoConfigureMockMvc'` | Dépendance `spring-boot-starter-webmvc-test` absente, ou import d'un ancien tutoriel : en Spring Boot 4, le package est `org.springframework.boot.webmvc.test.autoconfigure`. |
| Le test d'intégration écrit dans `data/troc` | `@ActiveProfiles("test")` oublié, ou fichier `application-test.properties` mal nommé ou mal placé. |
| `500` « bad SQL grammar » dans les tests d'intégration | L'URL de test a perdu `;CASE_INSENSITIVE_IDENTIFIERS=TRUE`. |
| Toute la configuration semble perdue pendant les tests | Le fichier de `src/test/resources` s'appelle `application.properties`. |
| Un test d'intégration passe seul, échoue avec les autres | `@Transactional` oublié sur la classe, ou un id supposé (`/users/1`). |
| `expected: <201 CREATED> but was: <200 OK>` | Votre `POST` répond `200` : corrigez le contrôleur (TP 2). |
| Avertissement `A Java agent has been loaded dynamically` | Mockito s'attache à la JVM pour fabriquer ses faux : sans conséquence. |

## Bonus

### Bonus 1 — Le cas qui marche

Écrivez `acceptTransfersBothItemsAndExecutesTrade` : un troc valide ; vérifiez que le statut devient `EXECUTED`, et que le transfert a été appelé **deux fois, avec les bons arguments**. Puis inversez proposeur et destinataire dans un transfert de `accept` : quel test le détecte ?

### Bonus 2 — Un troc de bout en bout par l'API

Écrivez `TradeApiTest`, un test d'intégration qui enchaîne par MockMvc tout le scénario : créer Alice et Bob, leurs objets, proposer un troc, l'accepter (`200`, `EXECUTED`), puis vérifier par `GET /items?ownerId=…` que les stocks ont été échangés. Puis un second test : accepter deux fois répond `409`.

Regroupez les requêtes répétées dans des méthodes utilitaires (`createUser(String name)`, qui renvoie l'id lu dans la réponse). Pour lire une valeur dans une réponse :

```java
String json = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Alice\"}"))
        .andReturn().getResponse().getContentAsString();
Number aliceId = JsonPath.read(json, "$.id"); // com.jayway.jsonpath.JsonPath
```

### Bonus 3 — Tester le rollback

Transformez la preuve du TP 6 en test : si le second transfert échoue, la base doit être **exactement** dans l'état d'avant l'appel.

Pour provoquer une panne au milieu, remplacez le bean `ItemService` par un **espion** (*spy*) : un vrai `ItemService`, dont on peut reprogrammer certains appels.

```java
@MockitoSpyBean // org.springframework.test.context.bean.override.mockito.MockitoSpyBean
ItemService itemService;

// le premier transfert s'exécute pour de vrai, le second tombe en panne
doCallRealMethod().doThrow(new IllegalStateException("panne simulée"))
        .when(itemService).transfer(any(), anyInt(), any());
```

**Le piège** : ne mettez **pas** `@Transactional` sur cette classe de test. Sinon, `accept` s'exécute **dans** la transaction du test : son `ROLLBACK` n'a pas encore eu lieu quand le test vérifie la base. Sans `@Transactional`, videz vous-même les tables après chaque test (`@AfterEach`, en supprimant les trocs, puis les objets, puis les utilisateurs, à cause des clés étrangères).

Une fois le test vert, retirez `@Transactional` de `accept` : le test doit passer au rouge. Remettez-le.

### Et ensuite

Le TP 9 ajoute Spring Security : il fera passer vos tests d'intégration au rouge (`401`), puis vous les adapterez. Un projet couvert par des tests est un projet où l'on voit tout de suite ce qu'un changement casse.

## Pour aller plus loin

- [JUnit 5 — Guide de l'utilisateur](https://junit.org/junit5/docs/current/user-guide/), en particulier [les assertions](https://junit.org/junit5/docs/current/user-guide/#writing-tests-assertions)
- [Mockito — Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html) (la Javadoc de la classe `Mockito` est un vrai tutoriel)
- [Spring Boot — Testing](https://docs.spring.io/spring-boot/reference/testing/index.html) et [tester une application Spring Boot](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Spring Framework — MockMvc](https://docs.spring.io/spring-framework/reference/testing/mockmvc.html)
- [Spring Guide — Testing the Web Layer](https://spring.io/guides/gs/testing-web/)
- [Martin Fowler — The Practical Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html) (en anglais)
