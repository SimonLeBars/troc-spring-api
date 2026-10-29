# Session 2 — TP bloc 1 : CRUD users et items, règles dans la configuration

## Objectif

À la fin de la séance 1, l'API savait faire deux choses : `GET /users` et `POST /users`. Dans ce bloc, on la complète en trois temps :

1. **Les utilisateurs, de bout en bout** : consulter, modifier, supprimer un utilisateur, avec le bon code de statut HTTP à chaque fois.
2. **Les objets** : la même chose sur `/items`, en autonomie, avec un filtre par propriétaire.
3. **La configuration** : la règle métier « un utilisateur ne peut pas déclarer plus de N objets » sort du code Java et part dans `application.properties`.

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

## Étape 1 — Un préfixe commun : `@RequestMapping`

Toutes les méthodes de `UserController` vont commencer par `/users`. Plutôt que de le répéter sur chaque méthode, déclarez-le une fois sur la classe :

```java
@RestController
@RequestMapping("/users") // préfixe commun à toutes les méthodes de la classe
public class UserController {

    // ... constructeur inchangé

    @GetMapping // GET /users
    public List<User> findAll() {
        return userService.findAll();
    }

    @PostMapping // POST /users
    public User create(@RequestBody User user) {
        return userService.create(user);
    }
}
```

Le chemin d'une méthode est la concaténation du chemin de la classe et de celui de la méthode : `@RequestMapping("/users")` + `@GetMapping("/{id}")` donne `/users/{id}`.

Relancez et vérifiez dans Bruno que **Utilisateurs > Créer utilisateur** et **Lister utilisateurs** fonctionnent toujours.

> La collection Bruno du projet a été rangée en dossiers (**Utilisateurs**, **Objets**, **Trocs**) et contient une requête pour chaque endpoint de cette séance. Si vous avez votre propre collection, ajoutez-y une requête à chaque nouvel endpoint : elle vous servira encore en séance 3 et pour la démonstration finale.

## Étape 2 — Consulter un utilisateur : `GET /users/{id}`

> Si vous avez fait le bonus 2 de la séance 1, vous avez déjà la route : passez directement au 2.2, qui corrige le cas « utilisateur inconnu ».

### 2.1 — Le repository renvoie un `Optional`

Aujourd'hui, `findById` renvoie `null` quand l'utilisateur n'existe pas. C'est un piège : rien dans la signature `User findById(Long id)` ne prévient l'appelant qu'il peut recevoir `null`, et un oubli se termine en `NullPointerException`.

Java fournit un type fait pour ça : [`Optional<T>`](https://dev.java/learn/api/streams/optionals/), une « boîte » qui contient soit une valeur, soit rien. Modifiez l'interface :

```java
public interface UserRepository {

    User save(User user);

    List<User> findAll();

    Optional<User> findById(Long id); // import java.util.Optional
}
```

Puis les deux implémentations (`InMemoryUserRepository` **et** `LoggingUserRepository` : elles doivent toutes les deux respecter le contrat) :

```java
@Override
public Optional<User> findById(Long id) {
    return Optional.ofNullable(users.get(id)); // vide si la map ne contient pas l'id
}
```

Le projet ne compile plus : `ItemService` comparait le résultat à `null`. Remplacez le test par :

```java
if (users.findById(item.getOwnerId()).isEmpty()) {
```

C'est exactement ce qu'on voulait : le compilateur a trouvé tous les endroits qui devaient traiter le cas « absent ».

### 2.2 — Le service lève une erreur `404`

Ajoutez à `UserService` :

```java
public User findById(Long id) {
    return users.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur " + id + " introuvable"));
}
```

- `orElseThrow` renvoie l'utilisateur s'il est dans la boîte, sinon lève l'exception fournie.
- `ResponseStatusException` (package `org.springframework.web.server`) est une exception que Spring sait traduire en réponse HTTP : ici, un `404 Not Found`. Elle interrompt le traitement, comme n'importe quelle exception.

### 2.3 — La route

Dans `UserController` :

```java
@GetMapping("/{id}") // GET /users/42
public User findById(@PathVariable Long id) {
    return userService.findById(id);
}
```

`{id}` est une variable de chemin : `@PathVariable` récupère sa valeur et la convertit en `Long`. Le lien se fait par le nom : `{id}` ↔ `Long id`.

### 2.4 — Afficher le message d'erreur

Testez **Consulter utilisateur inconnu** (`GET /users/42`) dans Bruno. Le code est bien `404`, mais le message « Utilisateur 42 introuvable » n'apparaît nulle part dans la réponse : par défaut, Spring Boot le masque.

Ajoutez dans `src/main/resources/application.properties` :

```properties
spring.web.error.include-message=always
```

Relancez : le corps de la réponse contient maintenant `"message": "Utilisateur 42 introuvable"`. C'est votre premier réglage dans ce fichier : on lui a changé un comportement de Spring Boot sans toucher au code. On y reviendra à l'étape 6.

> En production, on évite parfois d'afficher les messages d'erreur, qui peuvent révéler des détails internes. Ici, nos messages sont écrits par nous et ne révèlent rien : on les garde.

Testez aussi `GET /users/abc` : `400 Bad Request`, sans une ligne de code. Spring n'arrive pas à convertir `abc` en `Long`, et sait que c'est la faute du client.

## Étape 3 — Un `POST` qui répond `201 Created`

En séance 1, le `POST /users` répondait `200 OK`. Le code attendu pour une création est `201 Created`, accompagné d'un en-tête `Location` qui indique où trouver la ressource créée.

Quand on a besoin de choisir le code **et** les en-têtes, la méthode renvoie une `ResponseEntity` (package `org.springframework.http`) : une réponse HTTP complète, construite à la main.

```java
@PostMapping
public ResponseEntity<User> create(@RequestBody User user) {
    User saved = userService.create(user);
    return ResponseEntity.created(URI.create("/users/" + saved.getId())) // 201 + Location
                         .body(saved);
}
```

Relancez, exécutez **Créer utilisateur** : Bruno affiche `201 Created`, et l'onglet **Headers** de la réponse contient `Location: /users/1`.

### Et si le client envoie un `id` ?

Exécutez deux fois **Créer utilisateur**, puis modifiez son corps en `{"id": 1, "name": "Pirate"}` et exécutez-le. Listez les utilisateurs : Alice a disparu, remplacée par Pirate. Le repository a vu un `id` déjà rempli et a écrasé l'utilisateur existant.

C'est le service qui décide des identifiants, pas le client. Corrigez `UserService.create` :

```java
public User create(User user) {
    user.setId(null); // l'id est attribué par le repository, pas par le client
    return users.save(user);
}
```

## Étape 4 — Modifier et supprimer

### 4.1 — `PUT /users/{id}` : remplacer un utilisateur

`PUT` remplace la ressource entière par celle envoyée dans le corps. L'`id` vient du chemin, les données du corps :

```java
// UserService
public User update(Long id, User user) {
    findById(id); // 404 si l'utilisateur n'existe pas
    user.setId(id);
    return users.save(user);
}
```

```java
// UserController
@PutMapping("/{id}")
public User update(@PathVariable Long id, @RequestBody User user) {
    return userService.update(id, user);
}
```

Le code de retour par défaut, `200 OK`, est le bon : rien à préciser. Testez avec **Modifier utilisateur**.

### 4.2 — `DELETE /users/{id}` : supprimer un utilisateur

1. Ajoutez `void deleteById(Long id);` à l'interface `UserRepository`, et implémentez-la dans les deux repositories (`users.remove(id)` ; dans `LoggingUserRepository`, n'oubliez pas la trace).
2. Ajoutez à `UserService` une méthode `delete(Long id)` qui lève le `404` si l'utilisateur n'existe pas (réutilisez `findById`), puis supprime.
3. Ajoutez la route :

```java
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT) // 204 : supprimé, rien à renvoyer
public void delete(@PathVariable Long id) {
    userService.delete(id);
}
```

Ici, le code ne dépend jamais du cas (quand tout va bien, c'est toujours `204`) : `@ResponseStatus` suffit, pas besoin de `ResponseEntity`.

Testez : **Créer un 3e utilisateur**, puis **Supprimer utilisateur** (`DELETE /users/3`) → `204`. Exécutez une deuxième fois la suppression → `404`.

> **Question** : on supprime un utilisateur qui possède des objets. Que deviennent ses objets ? Ici, rien : ils restent, avec un `ownerId` qui ne désigne plus personne. En séance 3, la base de données refusera cette suppression grâce à une **clé étrangère**.

### Récapitulatif : choisir son outil

| Besoin | Outil |
|---|---|
| Le code ou les en-têtes dépendent du cas (`201` + `Location`) | `ResponseEntity` |
| Le code est toujours le même quand tout va bien (`204`) | `@ResponseStatus` sur la méthode |
| Interrompre le traitement avec une erreur (`404`, `400`, `409`) | `throw new ResponseStatusException(...)` |

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

`Objects.equals` compare deux `Long` sans risque de `NullPointerException` si l'un des deux est `null`. Attention : comparer deux `Long` avec `==` compare les **objets**, pas les valeurs, et donne des résultats faux au-delà de 127.

Les noms de méthodes (`findByOwnerId`, `countByOwnerId`) ne sont pas choisis au hasard : en séance 3, Spring Data saura écrire ces requêtes tout seul, à partir de leur nom.

### 5.2 — Le service

Dans `ItemService`, ajoutez `findById`, `update` et `delete`, sur le modèle de `UserService`, et faites évoluer les méthodes existantes :

- `findAll(Long ownerId)` : tous les objets si `ownerId` est `null`, sinon ceux de cet utilisateur.
- `create` et `update` vérifient l'objet reçu :
  - quantité ≤ 0 → `400 Bad Request` (la requête est invalide en elle-même) ;
  - propriétaire inconnu → `404 Not Found` (remplace l'`IllegalArgumentException` de la séance 1).
- `create` ignore l'`id` envoyé par le client, comme à l'étape 3.

Les vérifications communes à `create` et `update` peuvent aller dans une méthode `private void checkItem(Item item)`.

### 5.3 — Le contrôleur

Créez `ItemController`, avec `@RequestMapping("/items")`. La nouveauté est le filtre de `GET /items?ownerId=1` : un **paramètre de requête**, récupéré avec `@RequestParam`.

```java
@GetMapping // GET /items ou GET /items?ownerId=1
public List<Item> findAll(@RequestParam(required = false) Long ownerId) {
    return itemService.findAll(ownerId); // ownerId vaut null si le paramètre est absent
}
```

Par défaut, un `@RequestParam` est **obligatoire** : sans `required = false`, `GET /items` (sans paramètre) renverrait `400`.

Pour choisir entre chemin, paramètre et corps :

| L'information… | Elle va dans… | Exemple |
|---|---|---|
| désigne **une** ressource précise | le chemin, `@PathVariable` | `GET /items/7` |
| filtre ou affine une liste | un paramètre, `@RequestParam` | `GET /items?ownerId=42` |
| contient les données à enregistrer | le corps, `@RequestBody` | `POST /items` + JSON |

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

`ItemController` est construit exactement comme `UserController` : remplacez `User` par `Item` et `/users` par `/items`, et ajoutez le `@RequestParam` sur `findAll`.

</details>

## Checkpoint intermédiaire — Vérifier le code, pas seulement le JSON

Avant de passer à la configuration, rejouez toutes les requêtes des dossiers **Utilisateurs** et **Objets** et vérifiez le **code** de chaque réponse avec le tableau de l'objectif. Bruno l'affiche en haut de la réponse.

En ligne de commande, `curl` n'affiche le code que si on ajoute `-i` :

```bash
curl -i -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Bob"}'
```

```
HTTP/1.1 201
Location: /users/2
Content-Type: application/json

{"id":2,"name":"Bob"}
```

Sous Windows PowerShell, `curl` est un alias d'une autre commande : tapez `curl.exe`.

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

Pourquoi `409 Conflict` et pas `400` ? La requête est parfaitement valide : déclarée par un utilisateur qui n'a encore rien, elle serait acceptée. C'est **l'état actuel des données** qui l'empêche. Règle à retenir : `400` si la requête est invalide en elle-même, `409` si elle est valide mais que l'état des données s'y oppose.

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
- [MDN — Codes de statut HTTP](https://developer.mozilla.org/fr/docs/Web/HTTP/Status) (tous les codes, avec exemples)
- [MDN — Méthodes de requête HTTP](https://developer.mozilla.org/fr/docs/Web/HTTP/Methods) (`GET`, `POST`, `PUT`, `DELETE`…)
