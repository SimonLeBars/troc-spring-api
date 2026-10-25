# Session 1 — TP bloc 2 : l'inversion de contrôle avec Spring

## Objectif

Au bloc 1, chaque classe fabriquait elle-même ses dépendances avec `new`, ce qui a provoqué les problèmes relevés au checkpoint #1. Dans ce bloc, on corrige ce code en trois temps :

1. **À la main** : les classes *reçoivent* leurs dépendances au lieu de les fabriquer (injection par constructeur).
2. **Avec Spring** : on laisse le conteneur Spring créer les objets et les brancher entre eux.
3. **Sur le web** : on expose les utilisateurs via une API REST, `GET /users` et `POST /users`.

**Objectif obligatoire de fin de séance** : le code refactoré, l'évolution 2 refaite (en comptant les fichiers modifiés), et `GET` / `POST /users` qui répondent.

## Étape 1 — Injecter à la main

### 1.1 — Un contrat : l'interface `UserRepository`

Dans `fr.cours.troc.user`, créez une interface qui décrit **ce que** fait un stockage d'utilisateurs, sans dire **comment** :

```java
public interface UserRepository {

    User save(User user);

    List<User> findAll();

    User findById(Long id);
}
```

Faites-la implémenter par vos deux stockages :

```java
public class InMemoryUserRepository implements UserRepository { ... }

public class LoggingUserRepository implements UserRepository { ... }
```

Ajoutez `@Override` au-dessus de chaque méthode implémentée : le compilateur vérifiera qu'elle correspond bien à une méthode de l'interface.

Faites de même pour les objets : une interface `ItemRepository` (`save`, `findAll`) dans `fr.cours.troc.item`, implémentée par `InMemoryItemRepository`.

### 1.2 — Les services reçoivent leurs dépendances

`UserService` ne fabrique plus son repository : il le **reçoit** dans son constructeur, et ne connaît plus que l'interface.

```java
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    // findAll() et create() ne changent pas
}
```

Faites de même pour `ItemService`, qui reçoit ses deux dépendances :

```java
public ItemService(ItemRepository items, UserRepository users) {
    this.items = items;
    this.users = users;
}
```

### 1.3 — Quelqu'un doit faire les `new` : `ManualDemo`

Le code ne compile plus dans `ManualDemo` : `new UserService()` n'existe plus. C'est maintenant `ManualDemo` qui crée les objets et les branche entre eux. Remplacez le début de `main` :

```java
UserRepository users = new InMemoryUserRepository();
UserService userService = new UserService(users);
ItemService itemService = new ItemService(new InMemoryItemRepository(), users);
```

Remarquez que `userService` et `itemService` reçoivent **le même** objet `users`.

Décommentez la ligne `itemService.create(tomatoes);` mise de côté au bloc 1, puis relancez `ManualDemo`. Le message `Utilisateur introuvable` a disparu, et la console affiche `1 objet(s) déclaré(s)` : les deux services partagent maintenant le même stockage. **Problème 1 (données incohérentes) résolu.**

### 1.4 — Refaire l'évolution 2

Repassez au stockage avec traces. Combien de fichiers devez-vous modifier ?

<details>
<summary>Solution</summary>

Un seul : la première ligne de `ManualDemo`.

```java
UserRepository users = new LoggingUserRepository("[USERS]");
```

`UserService` et `ItemService` n'ont pas bougé : ils ne connaissent que l'interface. C'est le principe d'inversion des dépendances (le **D** de SOLID) : le service dépend d'un contrat, pas d'une implémentation. Même chose pour l'évolution 3 : un changement de constructeur ne touche que l'endroit qui fait le `new`.

</details>

Notez ce chiffre à côté de ceux du bloc 1.

## Étape 2 — Spring prend le relais

`ManualDemo` fonctionne, mais sur un vrai projet avec 50 classes, quelqu'un doit écrire tous ces `new` dans le bon ordre. On va déléguer ce travail au **conteneur Spring** (l'`ApplicationContext`).

### 2.1 — Déclarer les beans

Un **bean** est simplement un objet dont Spring s'occupe : il le crée, et le passe aux classes qui en ont besoin. On indique à Spring quelles classes sont des beans avec des annotations :

| Classe | Annotation | Rôle |
|---|---|---|
| `InMemoryUserRepository` | `@Repository` | bean d'accès aux données |
| `InMemoryItemRepository` | `@Repository` | bean d'accès aux données |
| `UserService` | `@Service` | bean de la couche métier |
| `ItemService` | `@Service` | bean de la couche métier |

Ajoutez ces annotations (import `org.springframework.stereotype.Repository` et `org.springframework.stereotype.Service`). Ne touchez pas à `LoggingUserRepository` pour l'instant : pas d'annotation dessus.

```java
@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) { // injecté par Spring
        this.users = users;
    }
    // ...
}
```

Aucune annotation à ajouter sur le constructeur : quand une classe n'a **qu'un seul** constructeur, Spring l'utilise automatiquement et lui passe les beans correspondants. Vous croiserez souvent `@Autowired` dans les tutoriels : il n'est pas nécessaire ici.

### 2.2 — Lancer l'application Spring

Supprimez `ManualDemo` : c'est Spring qui fait maintenant son travail.

Lancez `TrocApplication` (flèche verte à côté de `main`, ou `mvnw.cmd spring-boot:run` sous Windows, `./mvnw spring-boot:run` sous Linux/macOS). Au démarrage, Spring :

1. **scanne** le package de `TrocApplication` et ses sous-packages, à la recherche des classes annotées ;
2. **crée** une instance de chaque bean, en commençant par ceux dont les autres ont besoin ;
3. **injecte** chaque bean dans les constructeurs qui le demandent.

Dans les logs, vous devez voir `Tomcat started on port 8080`, puis `Started TrocApplication`. Rien n'est encore visible depuis un navigateur : aucune route HTTP n'existe. C'est l'étape suivante.

### 2.3 — Provoquer une erreur (pour comprendre)

Retirez temporairement `@Repository` de `InMemoryUserRepository` et relancez. L'application refuse de démarrer :

```
Parameter 1 of constructor in fr.cours.troc.item.ItemService required a bean of type
'fr.cours.troc.user.UserRepository' that could not be found.
```

Spring vous dit précisément ce qui manque : `ItemService` demande un `UserRepository` (son deuxième paramètre, numéroté 1), et aucun bean ne correspond. Selon l'ordre dans lequel Spring crée les beans, le message peut aussi désigner `UserService` : c'est le même problème. Remettez l'annotation.

## Étape 3 — Exposer l'API : `UserController`

Dans `fr.cours.troc.user`, créez le contrôleur, troisième couche de l'architecture :

```java
@RestController // bean de la couche web, répond en JSON
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users") // GET /users
    public List<User> findAll() {
        return userService.findAll();
    }

    @PostMapping("/users") // POST /users + corps JSON
    public User create(@RequestBody User user) {
        return userService.create(user);
    }
}
```

- `@RestController` est lui aussi un bean (comme `@Service` et `@Repository`) : Spring lui injecte `UserService`.
- `@GetMapping` et `@PostMapping` associent une méthode Java à un verbe HTTP et un chemin.
- Spring convertit automatiquement l'objet Java renvoyé en JSON. Dans l'autre sens, `@RequestBody` convertit le JSON reçu en objet `User`, en appelant les setters correspondant aux clés (`"name"` → `setName`). Cette conversion est faite par la bibliothèque Jackson, incluse avec Spring Web.

### Tester avec Bruno

Relancez l'application, puis testez avec [Bruno](https://www.usebruno.com/), un client HTTP gratuit et open-source dont les collections sont de simples fichiers texte qu'on peut versionner dans le dépôt :

1. Ouvrez Bruno > **Open Collection** > sélectionnez le dossier `bruno/` du projet.
2. Choisissez l'environnement **Local** (en haut à droite) : il définit `baseUrl = http://localhost:8080`.
3. Exécutez **Créer utilisateur** : `POST /users`, avec l'en-tête `Content-Type: application/json` et le corps `{"name": "Alice"}`. La réponse est `{"id": 1, "name": "Alice"}` : l'`id` a été attribué par le repository.
4. Exécutez **Lister utilisateurs** : `GET /users` renvoie `[{"id": 1, "name": "Alice"}]`.

Sans Bruno, l'équivalent en ligne de commande :

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice"}'

curl http://localhost:8080/users
```

Pour un `GET`, un navigateur suffit aussi : ouvrez <http://localhost:8080/users>.

Remarquez que le `POST` répond `200 OK`. Pour une création, le code attendu est `201 Created` : les codes de statut HTTP seront traités en séance 2.

Les données sont en mémoire : elles disparaissent à chaque redémarrage de l'application. La base de données arrive en séance 3.

## Étape 4 — Refaire l'évolution 2 avec Spring

Le client veut de nouveau voir les traces des opérations sur les utilisateurs. Avec Spring, plus de `ManualDemo` : pour changer d'implémentation, il suffit de **déplacer `@Repository`** d'`InMemoryUserRepository` vers `LoggingUserRepository`.

Faites-le et relancez. L'application refuse de démarrer :

```
Parameter 0 of constructor in fr.cours.troc.user.LoggingUserRepository required a bean of type
'java.lang.String' that could not be found.
```

Depuis l'évolution 3, le seul constructeur de `LoggingUserRepository` attend une `String` (le préfixe). Spring essaie de l'injecter comme les autres dépendances, mais aucun bean de type `String` n'existe. Ajoutez un constructeur sans paramètre qui fournit une valeur par défaut :

```java
public LoggingUserRepository() {
    this("[USERS]");
}
```

Quand une classe a plusieurs constructeurs, dont un sans paramètre, Spring utilise celui-là. Relancez, exécutez **Créer utilisateur** dans Bruno : la ligne `[USERS] save 1 Alice` apparaît dans les logs. Les services et le contrôleur n'ont pas été modifiés.

Comptez les fichiers modifiés et comparez avec le tableau du bloc 1.

Remettez ensuite `@Repository` sur `InMemoryUserRepository` (et retirez-le de `LoggingUserRepository`) : c'est le point de départ de la séance 2, où le préfixe viendra d'un fichier de configuration.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `required a bean of type ... that could not be found` | Annotation oubliée sur la classe, ou classe placée **en dehors** de `fr.cours.troc` : le scan part du package de `TrocApplication` et ne voit pas les autres. |
| `required a single bean, but 2 were found` | Deux classes annotées implémentent la même interface (par exemple `@Repository` sur `InMemoryUserRepository` **et** `LoggingUserRepository`) : Spring ne sait pas laquelle injecter. |
| `415 Unsupported Media Type` sur le `POST` | En-tête `Content-Type: application/json` manquant. |
| `Port 8080 was already in use` | Une instance précédente de l'application tourne encore : arrêtez-la (bouton stop rouge dans IntelliJ). |
| `UnsupportedClassVersionError` au lancement | Le projet est lancé avec un JDK trop ancien : dans IntelliJ, **File > Project Structure > Project > SDK** doit être un JDK 25. |
| Le `POST` renvoie un utilisateur avec `"name": null` | Clé JSON mal orthographiée (`"Name"`, `"nom"`…) : elle doit correspondre exactement au setter (`setName` → `"name"`). |

## En avance ? Features bonus

### Bonus 1 — Choisir le stockage selon l'environnement (profils Spring)

Au lieu de déplacer `@Repository` à la main, Spring peut choisir l'implémentation au démarrage, selon un **profil** actif.

1. Annotez **les deux** repositories avec `@Repository`, puis ajoutez `@Profile("dev")` sur `LoggingUserRepository` et `@Profile("!dev")` sur `InMemoryUserRepository` (`!dev` signifie « tout profil sauf `dev` »).
2. Lancez normalement : pas de traces.
3. Activez le profil `dev`, au choix :
   - dans `src/main/resources/application.properties`, ajoutez `spring.profiles.active=dev` ;
   - ou dans IntelliJ : configuration de lancement de `TrocApplication` > **Active profiles** : `dev`.
4. Relancez : les traces `[USERS]` apparaissent, sans avoir modifié une seule classe.

Documentation : [Spring Boot — Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html).

### Bonus 2 — Consulter un utilisateur : `GET /users/{id}`

1. Ajoutez à `UserService` une méthode `User findById(Long id)`.
2. Ajoutez au contrôleur :

   ```java
   @GetMapping("/users/{id}")
   public User findById(@PathVariable Long id) {
       return userService.findById(id);
   }
   ```

   `{id}` dans le chemin est une variable : `@PathVariable` la convertit en `Long` et la passe à la méthode.
3. Dans Bruno, dupliquez **Lister utilisateurs** (clic droit > **Clone**), nommez-la **Consulter utilisateur** et changez l'URL en `{{baseUrl}}/users/1`.

Que renvoie `GET /users/42` pour un utilisateur qui n'existe pas ? Le bon comportement (`404 Not Found`) sera vu en séance 2.

## Pour aller plus loin

- [Spring — Guide « Building a RESTful Web Service »](https://spring.io/guides/gs/rest-service)
- [Spring Framework — The IoC Container](https://docs.spring.io/spring-framework/reference/core/beans.html) (référence complète, à parcourir)
- [Spring Boot — Structuring Your Code](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html) (pourquoi la classe principale doit être dans le package racine)
- [Wikipedia — Inversion de contrôle](https://fr.wikipedia.org/wiki/Inversion_de_contr%C3%B4le)
