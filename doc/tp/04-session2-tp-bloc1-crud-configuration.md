# Session 2 — TP bloc 1 : CRUD users et items, règles dans la configuration

## Objectif

À la fin de la séance 1, l'API savait faire deux choses : `GET /users` et `POST /users`. Dans ce bloc, on la complète en trois temps :

1. **Les utilisateurs, de bout en bout** : consulter, modifier, supprimer, avec le bon code de statut.
2. **Les objets** : la même chose sur `/items`, en autonomie, avec un filtre par propriétaire.
3. **La configuration** : la règle métier « un utilisateur ne peut pas déclarer plus de N objets » sort du code Java et part dans `application.properties`.

Vous avez déjà écrit des API REST : ce support ne réexplique pas ce qu'est un CRUD, un `404` ou un paramètre de requête. Il montre **comment on l'écrit en Spring**, et il est volontairement moins guidé que ceux de la séance 1 (les solutions sont repliées, à ouvrir en cas de blocage). La vraie nouveauté du bloc est l'étape 6, la configuration.

**Objectif obligatoire de fin de bloc** : tous les endpoints du tableau ci-dessous répondent, avec le bon code, et `max-per-user` est lu depuis la configuration.

| Verbe | Chemin | Rôle | Code si tout va bien | Codes d'erreur |
|---|---|---|---|---|
| `GET` | `/users` | lister les utilisateurs | `200` | |
| `GET` | `/users/{id}` | consulter un utilisateur | `200` | `404` inconnu |
| `POST` | `/users` | créer un utilisateur | `201` | |
| `PUT` | `/users/{id}` | modifier un utilisateur | `200` | `404` inconnu |
| `DELETE` | `/users/{id}` | supprimer un utilisateur | `204` | `404` inconnu |
| `GET` | `/items` (option `?ownerId=…`) | lister les objets, tous ou ceux d'un utilisateur | `200` | |
| `GET` | `/items/{id}` | consulter un objet | `200` | `404` inconnu |
| `POST` | `/items` | déclarer un objet | `201` | `400` quantité ≤ 0, `404` propriétaire inconnu, `409` trop d'objets |
| `PUT` | `/items/{id}` | modifier un objet | `200` | `400`, `404` |
| `DELETE` | `/items/{id}` | supprimer un objet | `204` | `404` inconnu |

Les données restent **en mémoire** pendant toute la séance : la base de données arrive en séance 3.

**Point de départ** : le projet de fin de séance 1 (`TP bloc 2 : l'inversion de contrôle avec Spring`), avec `GET` et `POST /users` qui répondent. Si ce n'est pas le cas, terminez d'abord l'étape 3 de ce support : c'est la base commune.

> Organisation conseillée pour un groupe de 4 : deux personnes sur `user/` (étapes 1 à 4), deux sur `item/` (étape 5, en commençant par le repository), puis tout le monde sur l'étape 6. Le découpage du projet par fonctionnalité s'y prête bien.

## Rappel express : vos réflexes REST, en Spring

| Vous connaissez… | En Spring |
|---|---|
| Associer un verbe et un chemin à un traitement | `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` sur une méthode |
| Un préfixe commun à un groupe de routes | `@RequestMapping("/users")` sur la classe du contrôleur |
| Une variable de chemin (`/items/7`) : désigne **une** ressource | paramètre de méthode annoté `@PathVariable Long id` |
| Un paramètre de requête (`/items?ownerId=42`) : filtre une liste | `@RequestParam(required = false) Long ownerId` |
| Le corps JSON d'un `POST` / `PUT` | `@RequestBody Item item` (converti par Jackson, via les setters) |
| Renvoyer du JSON | renvoyer l'objet Java : Spring le convertit tout seul |
| Choisir le code et les en-têtes de la réponse (`201` + `Location`) | renvoyer une `ResponseEntity<T>` |
| Un code fixe pour une route (`204` sur un `DELETE`) | `@ResponseStatus(HttpStatus.NO_CONTENT)` sur la méthode |
| Interrompre le traitement avec une erreur (`400`, `404`, `409`) | `throw new ResponseStatusException(HttpStatus.NOT_FOUND, "message")` |

Deux règles du projet, à garder en tête pour tout le cours :

- **`400` ou `409` ?** `400` si la requête est invalide en elle-même (quantité négative), `409` si elle est valide mais que **l'état actuel des données** s'y oppose (l'utilisateur a déjà trop d'objets). Ce choix fait partie des choix techniques à justifier à l'évaluation.
- **Un `500` n'est jamais un choix** : c'est une exception non prévue, donc un bug à corriger.

Besoin de vous rafraîchir la mémoire ? [MDN — Codes de statut HTTP](https://developer.mozilla.org/fr/docs/Web/HTTP/Status), [MDN — Méthodes HTTP](https://developer.mozilla.org/fr/docs/Web/HTTP/Methods), [Microsoft — Conception d'API web RESTful](https://learn.microsoft.com/fr-fr/azure/architecture/best-practices/api-design).

## Étape 1 — Un préfixe commun : `@RequestMapping`

Déclarez `@RequestMapping("/users")` sur `UserController`, et retirez `"/users"` de `@GetMapping` et `@PostMapping`. Le chemin d'une méthode est la concaténation des deux : `@RequestMapping("/users")` + `@GetMapping("/{id}")` donne `/users/{id}`.

Relancez et vérifiez dans Bruno que **Utilisateurs > Créer utilisateur** et **Lister utilisateurs** fonctionnent toujours.

> La collection Bruno du projet est rangée en dossiers (**Utilisateurs**, **Objets**, **Trocs**) et contient une requête pour chaque endpoint de cette séance. Si vous avez votre propre collection, ajoutez-y une requête à chaque nouvel endpoint : elle vous servira encore en séance 3 et pour la démonstration finale.

## Étape 2 — Consulter un utilisateur : `GET /users/{id}`

> Si vous avez fait le bonus 2 de la séance 1, vous avez déjà la route : il reste à traiter le cas « utilisateur inconnu ».

### 2.1 — Le repository renvoie un `Optional`

Aujourd'hui, `findById` renvoie `null` quand l'utilisateur n'existe pas, et rien dans la signature `User findById(Long id)` ne prévient l'appelant. Java fournit un type fait pour ça : [`Optional<T>`](https://dev.java/learn/api/streams/optionals/), une « boîte » qui contient soit une valeur, soit rien. Changez la signature dans l'interface `UserRepository` :

```java
Optional<User> findById(Long id); // import java.util.Optional
```

Puis dans les **deux** implémentations (`InMemoryUserRepository` et `LoggingUserRepository`) :

```java
@Override
public Optional<User> findById(Long id) {
    return Optional.ofNullable(users.get(id)); // vide si la map ne contient pas l'id
}
```

Le projet ne compile plus : `ItemService` comparait le résultat à `null`. Remplacez le test par `users.findById(item.getOwnerId()).isEmpty()`. Le compilateur a trouvé tous les endroits qui devaient traiter le cas « absent » : c'est exactement ce qu'on voulait.

### 2.2 — Le service lève le `404`, le contrôleur expose la route

```java
// UserService
public User findById(Long id) {
    return users.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur " + id + " introuvable"));
}
```

```java
// UserController
@GetMapping("/{id}") // GET /users/42
public User findById(@PathVariable Long id) {
    return userService.findById(id);
}
```

- `orElseThrow` renvoie l'utilisateur s'il est dans la boîte, sinon lève l'exception fournie.
- `ResponseStatusException` (package `org.springframework.web.server`) est traduite par Spring en réponse HTTP avec le code indiqué.
- Le lien entre `{id}` et le paramètre Java se fait par le nom : `{id}` ↔ `Long id`. La conversion en `Long` est automatique : `GET /users/abc` répond `400` sans une ligne de code.

### 2.3 — Afficher le message d'erreur

Testez **Consulter utilisateur inconnu** (`GET /users/42`) dans Bruno. Le code est bien `404`, mais le message « Utilisateur 42 introuvable » n'apparaît pas dans la réponse : par défaut, Spring Boot le masque. Ajoutez dans `src/main/resources/application.properties` :

```properties
spring.web.error.include-message=always
```

Relancez : le corps contient maintenant `"message": "Utilisateur 42 introuvable"`. C'est votre premier réglage dans ce fichier : on a changé un comportement de Spring Boot sans toucher au code. On y revient à l'étape 6.

> En production, on évite parfois d'afficher les messages d'erreur, qui peuvent révéler des détails internes. Ici, nos messages sont écrits par nous et ne révèlent rien : on les garde.

## Étape 3 — Un `POST` qui répond `201 Created`

Le `POST /users` de la séance 1 répond `200 OK`. Pour renvoyer `201` avec l'en-tête `Location`, la méthode renvoie une `ResponseEntity` (package `org.springframework.http`) :

```java
@PostMapping
public ResponseEntity<User> create(@RequestBody User user) {
    User saved = userService.create(user);
    return ResponseEntity.created(URI.create("/users/" + saved.getId())) // 201 + Location
                         .body(saved);
}
```

Vérifiez avec **Créer utilisateur** : `201`, et `Location: /users/1` dans l'onglet **Headers** de la réponse.

**Et si le client envoie un `id` ?** Créez deux utilisateurs, puis exécutez **Créer utilisateur** avec le corps `{"id": 1, "name": "Pirate"}` : Alice est écrasée, parce que le repository a vu un `id` déjà rempli. C'est le serveur qui attribue les identifiants : ajoutez `user.setId(null);` au début de `UserService.create`.

## Étape 4 — Modifier et supprimer

À vous, avec le tableau du rappel :

1. **`PUT /users/{id}`** : `404` si l'utilisateur n'existe pas (réutilisez `findById`), sinon l'`id` vient du chemin, les données du corps. `200` par défaut : rien à préciser.
2. **`DELETE /users/{id}`** : ajoutez `void deleteById(Long id)` à l'interface `UserRepository` et à ses **deux** implémentations (dans `LoggingUserRepository`, n'oubliez pas la trace), une méthode `delete` dans le service (`404` si inconnu), puis la route, qui répond toujours `204` quand tout va bien.

Testez avec **Modifier utilisateur**, puis **Créer un 3e utilisateur** et **Supprimer utilisateur** (`DELETE /users/3`) → `204`. Une deuxième suppression → `404`.

<details>
<summary>Solution : PUT et DELETE</summary>

```java
// UserService
public User update(Long id, User user) {
    findById(id); // 404 si l'utilisateur n'existe pas
    user.setId(id);
    return users.save(user);
}

public void delete(Long id) {
    findById(id);
    users.deleteById(id);
}
```

```java
// UserController
@PutMapping("/{id}")
public User update(@PathVariable Long id, @RequestBody User user) {
    return userService.update(id, user);
}

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT) // 204 : supprimé, rien à renvoyer
public void delete(@PathVariable Long id) {
    userService.delete(id);
}
```

Ici le code de succès ne dépend jamais du cas : `@ResponseStatus` suffit, pas besoin de `ResponseEntity`.

</details>

> **Question** : on supprime un utilisateur qui possède des objets. Que deviennent ses objets ? Ici, rien : ils restent, avec un `ownerId` qui ne désigne plus personne. En séance 3, la base de données refusera cette suppression grâce à une **clé étrangère**.

## Étape 5 — Les objets : `/items`, à vous

Appliquez la même démarche aux objets, dans le package `fr.cours.troc.item`. Un objet (`Item`) appartient à un utilisateur (`ownerId`) et a une quantité (par exemple : 10 tomates, 2 vélos, 30 livres).

### 5.1 — Le repository

Complétez `ItemRepository` et `InMemoryItemRepository` :

| Méthode | Rôle |
|---|---|
| `Optional<Item> findById(Long id)` | un objet, ou vide |
| `List<Item> findByOwnerId(Long ownerId)` | les objets d'un utilisateur |
| `long countByOwnerId(Long ownerId)` | le nombre d'objets d'un utilisateur (servira à l'étape 6) |
| `void deleteById(Long id)` | supprime un objet |

Pour filtrer les valeurs d'une map, utilisez un [stream](https://dev.java/learn/api/streams/) :

```java
return items.values().stream()
        .filter(item -> Objects.equals(item.getOwnerId(), ownerId))
        .toList();
```

`Objects.equals` compare deux `Long` sans risque de `NullPointerException`. Attention : comparer deux `Long` avec `==` compare les **objets**, pas les valeurs, et donne des résultats faux au-delà de 127.

Les noms de méthodes (`findByOwnerId`, `countByOwnerId`) ne sont pas choisis au hasard : en séance 3, Spring Data saura écrire ces requêtes tout seul, à partir de leur nom.

### 5.2 — Le service

Dans `ItemService`, ajoutez `findById`, `update` et `delete`, sur le modèle de `UserService`, et faites évoluer les méthodes existantes :

- `findAll(Long ownerId)` : tous les objets si `ownerId` est `null`, sinon ceux de cet utilisateur.
- `create` et `update` vérifient l'objet reçu : quantité ≤ 0 → `400` ; propriétaire inconnu → `404` (remplace l'`IllegalArgumentException` de la séance 1). Ces vérifications communes peuvent aller dans une méthode `private void checkItem(Item item)`.
- `create` ignore l'`id` envoyé par le client, comme à l'étape 3.

### 5.3 — Le contrôleur

Créez `ItemController`, avec `@RequestMapping("/items")`. La seule nouveauté est le filtre `GET /items?ownerId=1`, avec `@RequestParam(required = false) Long ownerId` : `ownerId` vaut `null` si le paramètre est absent. Sans `required = false`, le paramètre est **obligatoire** et `GET /items` répond `400`.

Testez avec le dossier **Objets** de Bruno, dans l'ordre (créez d'abord les utilisateurs 1 et 2 dans le dossier **Utilisateurs**). Vérifiez aussi les cas d'erreur, en modifiant le corps de **Créer objet (Alice)** : `"quantity": -1` → `400`, `"ownerId": 99` → `404`.

<details>
<summary>Solution : ItemService</summary>

```java
@Service
public class ItemService {

    private final ItemRepository items;
    private final UserRepository users;

    public ItemService(ItemRepository items, UserRepository users) {
        this.items = items;
        this.users = users;
    }

    public List<Item> findAll(Long ownerId) {
        if (ownerId == null) {
            return items.findAll();
        }
        return items.findByOwnerId(ownerId);
    }

    public Item findById(Long id) {
        return items.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Objet " + id + " introuvable"));
    }

    public Item create(Item item) {
        checkItem(item);
        item.setId(null);
        return items.save(item);
    }

    public Item update(Long id, Item item) {
        findById(id);
        checkItem(item);
        item.setId(id);
        return items.save(item);
    }

    public void delete(Long id) {
        findById(id);
        items.deleteById(id);
    }

    private void checkItem(Item item) {
        if (item.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité doit être positive");
        }
        if (users.findById(item.getOwnerId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Utilisateur " + item.getOwnerId() + " introuvable");
        }
    }
}
```

</details>

<details>
<summary>Solution : ItemController</summary>

```java
@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping // GET /items ou GET /items?ownerId=1
    public List<Item> findAll(@RequestParam(required = false) Long ownerId) {
        return itemService.findAll(ownerId);
    }

    @GetMapping("/{id}")
    public Item findById(@PathVariable Long id) {
        return itemService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Item> create(@RequestBody Item item) {
        Item saved = itemService.create(item);
        return ResponseEntity.created(URI.create("/items/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public Item update(@PathVariable Long id, @RequestBody Item item) {
        return itemService.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        itemService.delete(id);
    }
}
```

</details>

## Checkpoint intermédiaire — Vérifier le code, pas seulement le JSON

Avant de passer à la configuration, rejouez toutes les requêtes des dossiers **Utilisateurs** et **Objets** et vérifiez le **code** de chaque réponse avec le tableau de l'objectif. En ligne de commande, pensez à `curl -i` (sans `-i`, ni le code ni les en-têtes ne s'affichent ; sous Windows PowerShell, tapez `curl.exe`).

## Étape 6 — Sortir la règle métier dans la configuration

Le client fixe une règle : **un utilisateur ne peut pas déclarer plus de 20 objets**. La première idée est d'écrire `20` directement dans `ItemService.create`. Mais dans six mois, personne ne saura d'où vient ce 20 ; et si le client veut 50 en production et 3 pour une démonstration, il faudra modifier le code, recompiler et redéployer.

Une valeur qui peut changer sans que le code change n'a rien à faire dans le code : elle va dans `application.properties`.

### 6.1 — Déclarer la règle

Ajoutez dans `src/main/resources/application.properties` :

```properties
# Règles métier du troc
troc.item.max-per-user=20
```

- Les clés `spring.*` et `server.*` sont lues par Spring Boot ; les clés `troc.*` sont **les nôtres**. Le préfixe `troc.` évite toute collision avec une clé de Spring.
- Les mots sont séparés par des tirets (`max-per-user`) : c'est la convention recommandée.
- Le format `application.yml` existe aussi et fait la même chose avec une syntaxe indentée. Un seul des deux suffit : on garde `.properties`.

### 6.2 — Lire la valeur : `@Value`

La valeur est une dépendance d'`ItemService`, comme le repository : elle arrive par le constructeur.

```java
@Service
public class ItemService {

    private final ItemRepository items;
    private final UserRepository users;
    private final int maxPerUser;

    public ItemService(ItemRepository items, UserRepository users,
                       @Value("${troc.item.max-per-user:20}") int maxPerUser) {
        this.items = items;
        this.users = users;
        this.maxPerUser = maxPerUser;
    }
    // ...
}
```

`@Value` vient du package `org.springframework.beans.factory.annotation`. La syntaxe `${clé:défaut}` signifie : la valeur de la clé dans la configuration, ou `20` si la clé est absente. Spring convertit lui-même le texte `"20"` en `int`.

Dans `create`, avant d'enregistrer, refusez l'objet si l'utilisateur en a déjà `maxPerUser` :

```java
if (items.countByOwnerId(item.getOwnerId()) >= maxPerUser) {
    throw new ResponseStatusException(HttpStatus.CONFLICT,
            "L'utilisateur " + item.getOwnerId() + " a déjà " + maxPerUser + " objets");
}
```

`409` et pas `400` : la requête est valide (déclarée par un utilisateur qui n'a encore rien, elle serait acceptée), c'est **l'état actuel des données** qui l'empêche.

**Testez** : passez `troc.item.max-per-user` à `2`, relancez, et exécutez trois fois **Créer objet (Alice)**. La troisième répond `409`. Aucune ligne de Java n'a changé entre `20` et `2`.

Faites ensuite deux expériences :

1. Supprimez la ligne du fichier et relancez : la valeur par défaut `20` s'applique.
2. Remettez la ligne, avec une faute de frappe dans le code Java (`${troc.item.max-per-usr:20}`) : l'application démarre sans erreur… et utilise `20`, quelle que soit la valeur du fichier. Le compilateur ne vérifie pas les clés : c'est la limite de `@Value`, et la raison de l'étape 6.4.

Remettez la clé correcte, et `20` dans le fichier.

### 6.3 — Retour sur l'évolution 3 : le préfixe des logs

En séance 1, `LoggingUserRepository` a reçu un constructeur sans paramètre qui fixait le préfixe `[USERS]` en dur, parce que Spring ne savait pas quelle `String` injecter. Avec `@Value`, on peut maintenant le lui dire.

Remplacez les **deux** constructeurs par un seul :

```java
public LoggingUserRepository(@Value("${troc.user.log-prefix:[USERS]}") String prefix) {
    this.prefix = prefix;
}
```

Pour vérifier : déplacez `@Repository` d'`InMemoryUserRepository` vers `LoggingUserRepository`, ajoutez `troc.user.log-prefix=[UTILISATEURS]` dans `application.properties`, relancez et créez un utilisateur. La trace `[UTILISATEURS] save 1 Alice` apparaît dans la console. Remettez ensuite `@Repository` sur `InMemoryUserRepository` (et retirez la clé du fichier si vous le souhaitez : la valeur par défaut reprendra le relais).

### 6.4 — Regrouper les réglages : `@ConfigurationProperties`

D'autres réglages `troc.item.*` viendront avec le projet. Plutôt que de multiplier les `@Value` et leurs chaînes de caractères, on décrit **tous** les réglages d'une fonctionnalité dans une classe dédiée.

Dans `fr.cours.troc.item`, créez :

```java
@ConfigurationProperties("troc.item") // préfixe des clés à lire
public record ItemProperties(@DefaultValue("20") int maxPerUser) { // max-per-user → maxPerUser
}
```

- Un **record** (Java 16+) est une classe immuable déclarée en une ligne : Java génère le constructeur et un accesseur par composant, ici `maxPerUser()`. Voir [dev.java — Records](https://dev.java/learn/records/).
- Spring lit toutes les clés commençant par `troc.item.` et remplit le record. La conversion `max-per-user` → `maxPerUser` est automatique.
- `@DefaultValue("20")` joue le rôle du `:20` de `@Value`.

Spring doit être prévenu qu'il doit chercher ces classes. Ajoutez une annotation sur `TrocApplication` :

```java
@SpringBootApplication
@ConfigurationPropertiesScan // détecte les classes @ConfigurationProperties
public class TrocApplication {
```

`ItemProperties` est maintenant un bean comme un autre : injectez-le dans `ItemService` à la place de l'`int`, et remplacez `maxPerUser` par `props.maxPerUser()`.

```java
public ItemService(ItemRepository items, UserRepository users, ItemProperties props) {
```

Relancez et vérifiez que la limite fonctionne toujours. Avantages : une seule classe à lire pour connaître tous les réglages des objets, un nom de méthode vérifié par le compilateur, et l'autocomplétion de l'IDE.

> **`@Value` ou `@ConfigurationProperties` ?** `@Value` pour une valeur isolée (le préfixe des logs) ; `@ConfigurationProperties` dès qu'un groupe de réglages apparaît pour une fonctionnalité.

### 6.5 — D'où vient la valeur ? La plus haute gagne

Le fichier `application.properties` n'est qu'une des sources de configuration. Spring Boot en consulte plusieurs, et la plus prioritaire l'emporte :

| Priorité | Source | Exemple |
|---|---|---|
| 1 (la plus forte) | argument de ligne de commande | `--troc.item.max-per-user=50` |
| 2 | variable d'environnement | `TROC_ITEM_MAXPERUSER=50` |
| 3 | fichier de profil | `application-dev.properties` |
| 4 | fichier principal | `application.properties` |
| 5 (la plus faible) | valeur par défaut dans le code | `@DefaultValue("20")` |

Pour le nom de la variable d'environnement : tout en majuscules, points remplacés par `_`, tirets supprimés.

**Testez** : dans IntelliJ, ouvrez la configuration de lancement de `TrocApplication` (**Run > Edit Configurations…**), et dans **Environment variables**, saisissez `TROC_ITEM_MAXPERUSER=1`. Si le champ n'apparaît pas, cliquez sur **Modify options > Environment variables**. Relancez : le deuxième objet d'Alice est refusé, alors que le fichier dit toujours `20`.

En ligne de commande, l'équivalent avec un argument :

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--troc.item.max-per-user=1
```

C'est ce mécanisme qui permet à **la même application** de tourner en développement, en test et en production : seule la configuration change. C'est aussi pour ça que les secrets (mot de passe de la base de données…) n'ont rien à faire dans le code, ni dans git : ils sont fournis par l'environnement.

Retirez la variable d'environnement avant de continuer.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `415 Unsupported Media Type` | En-tête `Content-Type: application/json` oublié. |
| `400 Bad Request` | JSON mal formé (virgule en trop, guillemets manquants), ou `/items/abc` au lieu d'un nombre. La console de l'application indique la raison précise. |
| `405 Method Not Allowed` | Bon chemin, mauvais verbe : par exemple un `PUT` alors que la méthode `@PutMapping` n'existe pas encore. |
| `404` sur une route qui devrait exister | Chemin mal recopié, ou `/users` présent à la fois dans `@RequestMapping` et dans `@GetMapping("/users")` (ce qui donne `/users/users`). |
| `400` sur `GET /items` sans paramètre | `required = false` oublié sur le `@RequestParam`. |
| Un champ arrive à `null` (ou `0`) | Nom de la clé JSON différent du nom Java (`"owner"` au lieu de `"ownerId"`), ou setter manquant. |
| Un champ manque dans la réponse | Getter manquant : Jackson ne convertit que ce qui est exposé par un getter. |
| Le message d'erreur n'apparaît pas dans la réponse | `spring.web.error.include-message=always` absent d'`application.properties`. |
| `does not override abstract method deleteById` | Une méthode a été ajoutée à l'interface, mais pas dans **toutes** ses implémentations (pensez à `LoggingUserRepository`). |
| La limite `max-per-user` est ignorée | Faute de frappe dans la clé (fichier ou `@Value`), ou `@ConfigurationPropertiesScan` absent de `TrocApplication`. |

## En avance ? Features bonus

Le CRUD étant un terrain connu, la plupart des groupes devraient atteindre au moins le bonus 1 pendant la séance.

### Bonus 1 — Centraliser la gestion des erreurs : `@RestControllerAdvice`

`ResponseStatusException` dans un service mélange deux couches : le service, qui porte le métier, connaît maintenant les codes HTTP. Plus propre : le service lève une exception **métier**, et un composant de la couche web la traduit en réponse HTTP.

1. Créez une exception métier, par exemple dans un package `fr.cours.troc.common` :

   ```java
   public class NotFoundException extends RuntimeException {
       public NotFoundException(String message) {
           super(message);
       }
   }
   ```

2. Dans les services, remplacez les `ResponseStatusException(HttpStatus.NOT_FOUND, ...)` par `new NotFoundException(...)`.
3. Créez un gestionnaire global :

   ```java
   @RestControllerAdvice // variante de @ControllerAdvice pour les API REST : s'applique à tous les contrôleurs
   public class ApiExceptionHandler {

       @ExceptionHandler(NotFoundException.class)
       public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException e) {
           ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
           return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
       }
   }
   ```

   `ProblemDetail` produit un corps d'erreur au format standard [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457).
4. Faites de même pour `400` et `409` (`InvalidRequestException`, `ConflictException`, ou les noms de votre choix).

Les services ne dépendent plus de rien de « web » : ils pourraient être réutilisés derrière une autre interface qu'une API REST.

Documentation : [Spring Framework — Exceptions](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html).

### Bonus 2 — Valider les entrées : `@Valid`

Plutôt que de tester chaque champ à la main dans le service, on peut déclarer les contraintes sur la classe elle-même.

1. Ajoutez la dépendance dans `pom.xml`, puis rechargez Maven (icône **Load Maven Changes** dans IntelliJ) :

   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-validation</artifactId>
   </dependency>
   ```

2. Annotez les champs d'`Item` (package `jakarta.validation.constraints`) :

   ```java
   @NotBlank
   private String name;

   @Positive
   private int quantity;

   @NotNull
   private Long ownerId;
   ```

3. Demandez la validation dans le contrôleur : `create(@Valid @RequestBody Item item)`.
4. Envoyez un objet sans nom ou avec une quantité négative : `400`, avant même d'entrer dans le service. Faites de même pour `User` (`@NotBlank` sur `name`).

Documentation : [Spring Guide — Validating Form Input](https://spring.io/guides/gs/validating-form-input) et [Jakarta Bean Validation — contraintes disponibles](https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/constraints/package-summary).

## Pour aller plus loin

- [Spring Boot — Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) (toutes les sources de configuration et leur ordre de priorité)
- [Spring Framework — Annotated Controllers](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html) (`@PathVariable`, `@RequestParam`, `@RequestBody`, `ResponseEntity`)
