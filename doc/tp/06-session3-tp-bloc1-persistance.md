# Session 3 — TP bloc 1 : tout en base

## Objectif

Jusqu'ici, les utilisateurs, les objets et les trocs vivent dans des `Map` Java : on redémarre l'application, tout disparaît. Dans ce bloc, on les range dans une **vraie base de données**, [H2](https://h2database.com/), avec [Spring Data JDBC](https://docs.spring.io/spring-data/relational/reference/jdbc.html).

C'est le moment où l'injection par constructeur de la séance 1 paie : les services dépendent d'une **interface** de repository, pas d'une classe. On va changer ce qui se cache derrière ces interfaces, sans réécrire les services.

| | Avant | Après |
|---|---|---|
| Les données | dans une `Map` Java, perdues à l'arrêt | dans un fichier `data/troc.mv.db`, conservées |
| Les identifiants | un compteur `AtomicLong` | attribués par la base à l'insertion |
| Les repositories | une interface + une classe `InMemory…` écrite à la main | une interface seule : Spring écrit l'implémentation |
| Les services, les contrôleurs | | inchangés (ou presque) |

**Objectif obligatoire de fin de bloc** :

1. Les trois tables `users`, `items`, `trades` existent, avec leurs clés primaires et étrangères.
2. Les classes `InMemory…Repository` ont disparu du projet.
3. Toutes les requêtes Bruno de la séance 2 répondent comme avant.
4. On redémarre l'application : les données sont toujours là, et on les voit dans la console H2 avec un `SELECT`.

**Point de départ** : le projet de fin de séance 2, avec `POST /trades` qui répond. Si votre `POST /trades` ne fonctionne pas encore, ce n'est pas bloquant pour les étapes 1 à 4 : les utilisateurs et les objets suivent exactement le même schéma.

On avance **une table à la fois** : `users` d'abord (la plus simple), puis `items` (la première clé étrangère), puis `trades`. À chaque table, l'application doit redémarrer et répondre avant de passer à la suivante.

> Organisation conseillée pour un groupe de 4 : tout le monde suit les étapes 1 à 3 ensemble (configuration et première table : c'est là que se trouvent la plupart des pièges). Ensuite, une personne par table pour les étapes 4 et 5, la dernière prépare les vérifications de l'étape 6.

## Étape 1 — Les dépendances

Ajoutez dans `pom.xml`, sous `spring-boot-starter-webmvc` :

```xml
<!-- Spring Data JDBC : les repositories générés et l'accès à la base -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jdbc</artifactId>
</dependency>
<!-- H2 : le moteur de base de données -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- la console web de H2, pour regarder dans la base -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-h2console</artifactId>
</dependency>
```

Puis rechargez Maven (icône **Load Maven Changes** dans IntelliJ, ou **Maven > Reload Project**).

- **H2** est une base de données écrite en Java : elle tourne **dans** votre application, il n'y a rien à installer. Le même code fonctionnerait avec PostgreSQL ou MySQL en changeant cette dépendance et l'URL de connexion.
- `<scope>runtime</scope>` : votre code n'utilise jamais de classe de H2 directement, il passe par Spring. H2 n'est nécessaire qu'à l'exécution.
- `spring-boot-h2console` : depuis Spring Boot 4, la console web de H2 est un module séparé. Sans lui, `/h2-console` répond `404`.

> **Pourquoi Spring Data JDBC et pas JPA ?** JPA (avec Hibernate) est très répandu en entreprise. Il génère le schéma et le SQL, suit les objets modifiés et les enregistre « tout seul ». Puissant, mais beaucoup de choses se passent sans qu'on les voie. Avec Spring Data JDBC, on écrit le schéma à la main, et un objet n'est enregistré que si on appelle `save()`. Les clés, les contraintes et les requêtes restent visibles : c'est ce qu'on veut apprendre ici.

## Étape 2 — Configurer la connexion

### 2.1 — `application.properties`

Ajoutez à la fin de `src/main/resources/application.properties` :

```properties
# Une base H2 dans un fichier du projet (dossier data/)
spring.datasource.url=jdbc:h2:file:./data/troc;CASE_INSENSITIVE_IDENTIFIERS=TRUE
spring.datasource.username=sa
spring.datasource.password=
# Exécuter schema.sql à chaque démarrage
spring.sql.init.mode=always
# Interface web d'administration : http://localhost:8080/h2-console
spring.h2.console.enabled=true
# Afficher dans la console chaque requête SQL envoyée à la base
logging.level.org.springframework.jdbc.core.JdbcTemplate=DEBUG
```

Ligne par ligne :

- `jdbc:h2:file:./data/troc` : la base est stockée dans le fichier `data/troc.mv.db`, à la racine du projet. Les données survivent au redémarrage. Avec `jdbc:h2:mem:troc`, la base vivrait en mémoire et disparaîtrait à l'arrêt, comme nos `Map`.
- `CASE_INSENSITIVE_IDENTIFIERS=TRUE` : une option propre à H2, expliquée à l'étape 3.5. Ne l'oubliez pas : sans elle, rien ne marche.
- `sa` et un mot de passe vide : les identifiants par défaut de H2. Une vraie base aurait un mot de passe, fourni par une variable d'environnement et jamais écrit dans git (séance 2, bloc 1, étape 6.5).
- `spring.sql.init.mode=always` : Spring Boot sait exécuter un script `schema.sql` au démarrage, mais par défaut seulement pour une base **en mémoire**. Pour une base fichier, il faut le demander.
- La dernière ligne active les traces de `JdbcTemplate`, la classe de Spring qui envoie le SQL. On verra passer chaque requête : c'est la meilleure façon de comprendre ce que fait Spring Data JDBC.

> En séance 1, `LoggingUserRepository` affichait chaque opération du repository. Cette ligne de configuration fait mieux, pour tous les repositories, et sans une ligne de Java.

### 2.2 — Ne pas commiter la base

La base est un fichier de données, propre à chaque poste : elle n'a rien à faire dans git. Ajoutez à la fin de `.gitignore` :

```
### H2 (base fichier locale) ###
data/
```

## Étape 3 — Première table : `users`

### 3.1 — Le schéma

Créez le fichier `src/main/resources/schema.sql` :

```sql
-- "users" et non "user" : USER est un mot réservé en SQL.
CREATE TABLE IF NOT EXISTS users (
    id        BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name      VARCHAR(100) NOT NULL
);
```

- `PRIMARY KEY` : la **clé primaire**, ce qui identifie une ligne de façon unique. Deux utilisateurs ne peuvent pas avoir le même `id`.
- `GENERATED BY DEFAULT AS IDENTITY` : c'est la base qui attribue l'identifiant à l'insertion (1, 2, 3…). Le compteur `AtomicLong` de nos repositories en mémoire n'a plus de raison d'être.
- `NOT NULL` : un utilisateur sans nom est refusé par la base.
- `IF NOT EXISTS` : avec `spring.sql.init.mode=always`, le script est rejoué **à chaque démarrage**. Sans cette précaution, le deuxième démarrage échouerait sur « table already exists ».

> **Corollaire important** : comme la table existe déjà, modifier ensuite le `CREATE TABLE` (ajouter une colonne, une contrainte…) **n'a aucun effet** sur la base existante. Pendant le cours, la solution est simple : arrêtez l'application, **supprimez le dossier `data/`**, relancez. La base est recréée à partir du script (vide). En entreprise, on utilise un outil de migration de schéma comme [Flyway](https://documentation.red-gate.com/flyway) ou [Liquibase](https://www.liquibase.com/), qui applique les modifications une par une sans perdre les données.

### 3.2 — L'entité : une ligne de la table

Annotez la classe `User` :

```java
@Table("users")         // la table qui stocke les User
public class User {

    @Id                 // la clé primaire
    private Long id;    // null → la base attribuera l'id à l'insertion
    private String name;

    // getters et setters inchangés
}
```

`@Table` vient de `org.springframework.data.relational.core.mapping`, `@Id` de `org.springframework.data.annotation` (attention, IntelliJ propose aussi un `@Id` de `jakarta.persistence` : c'est celui de JPA, pas le bon).

Sans `@Table`, Spring chercherait une table nommée d'après la classe : `user`, le mot réservé.

### 3.3 — Le repository : l'interface suffit

Remplacez tout le contenu de `UserRepository` par :

```java
public interface UserRepository extends ListCrudRepository<User, Long> {
}
```

(`org.springframework.data.repository.ListCrudRepository`)

C'est tout. `ListCrudRepository<User, Long>` (« des `User`, dont l'identifiant est un `Long` ») déclare déjà `save`, `findAll`, `findById`, `deleteById`, `count`, `existsById`… avec exactement les signatures qu'on avait écrites. Au démarrage, Spring Data JDBC **génère une classe** qui implémente cette interface en SQL, et l'enregistre comme bean. `UserService`, qui reçoit un `UserRepository` par son constructeur, reçoit cette classe générée sans le savoir.

`save()` choisit seul entre les deux requêtes :

| L'`id` de l'objet est… | `save()` exécute | Puis |
|---|---|---|
| `null` | `INSERT` | l'`id` attribué par la base est recopié dans l'objet retourné |
| renseigné | `UPDATE … WHERE id = ?` | |

C'est pour ça que `UserService.create` remet l'`id` à `null` (séance 2) : il garantit un `INSERT`.

### 3.4 — Supprimer les implémentations en mémoire

Supprimez `InMemoryUserRepository` **et** `LoggingUserRepository`.

- `InMemoryUserRepository` : si elle restait, Spring trouverait deux beans `UserRepository` (elle et la classe générée) et refuserait de démarrer.
- `LoggingUserRepository` : elle implémente `UserRepository`, qui hérite maintenant d'une douzaine de méthodes. Elle ne compile plus. Les traces SQL de l'étape 2.1 la remplacent avantageusement.

Si votre `application.properties` contient encore une clé `troc.user.log-prefix`, vous pouvez la retirer : plus personne ne la lit.

### 3.5 — Lancer, tester, redémarrer

Lancez l'application. Dans la console, cherchez :

```
Finished Spring Data repository scanning in 87 ms. Found 1 JDBC repository interface.
H2 console available at '/h2-console'. Database available at 'jdbc:h2:file:./data/troc;CASE_INSENSITIVE_IDENTIFIERS=TRUE'
```

Dans Bruno, exécutez **Utilisateurs > Créer utilisateur**, puis **Créer un 2e utilisateur**. La console affiche le SQL envoyé :

```
Executing prepared SQL statement [INSERT INTO "users" ("NAME") VALUES (?)]
```

Puis **le test qui compte** : arrêtez l'application, relancez-la, exécutez **Lister utilisateurs**. Alice et Bob sont toujours là.

> **Pourquoi `CASE_INSENSITIVE_IDENTIFIERS` ?** Regardez la requête : Spring Data JDBC entoure les noms de guillemets (`"users"`, `"NAME"`). En SQL, un nom entre guillemets est **sensible à la casse**, alors qu'un nom sans guillemets ne l'est pas : H2 range le `CREATE TABLE users` du script sous le nom `USERS`, en majuscules. Pour H2, `"users"` et `USERS` seraient donc deux tables différentes, et l'application répondrait `500` avec « Table "users" not found ». L'option demande à H2 d'ignorer la casse des noms. Les noms déduits par Spring (`"NAME"`) sont déjà en majuscules pour H2 ; seul le nom écrit à la main dans `@Table("users")` pose problème.

### 3.6 — Regarder dans la base : la console H2

Ouvrez [http://localhost:8080/h2-console](http://localhost:8080/h2-console). Dans le formulaire de connexion :

| Champ | Valeur |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/troc;CASE_INSENSITIVE_IDENTIFIERS=TRUE`, **exactement** la valeur de `spring.datasource.url` |
| User Name | `sa` |
| Password | (vide) |

Le champ propose par défaut `jdbc:h2:~/test` ou `jdbc:h2:mem:testdb` : ce sont d'autres bases, vides. Recopiez l'URL du fichier de configuration.

Une fois connecté, tapez dans la zone de texte et cliquez sur **Run** :

```sql
SELECT * FROM users;
```

Vous voyez les lignes créées par Bruno. La console sert aussi à vérifier ce que fait votre code, sans passer par l'API : gardez-la ouverte dans un onglet, elle servira beaucoup au bloc 2.

> La console accède directement à la base : un `DELETE` ou un `UPDATE` tapé ici s'applique pour de vrai. C'est un outil de développement, qu'on n'active jamais en production.

## Étape 4 — Deuxième table : `items`, et la première clé étrangère

### 4.1 — Le schéma

Ajoutez à `schema.sql`, **après** `users` (le script est exécuté dans l'ordre, et `items` fait référence à `users`) :

```sql
CREATE TABLE IF NOT EXISTS items (
    id        BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    owner_id  BIGINT NOT NULL REFERENCES users(id),
    name      VARCHAR(100) NOT NULL,
    quantity  INT NOT NULL CHECK (quantity >= 0),
    UNIQUE (owner_id, name)
);
```

Trois nouveautés, qui sont autant de **garanties** apportées par la base :

| Contrainte | Ce que la base refuse | Même si… |
|---|---|---|
| `REFERENCES users(id)` : une **clé étrangère** | un objet dont `owner_id` ne désigne aucun utilisateur ; la suppression d'un utilisateur qui possède encore des objets | …le code Java oublie de vérifier |
| `CHECK (quantity >= 0)` | un stock négatif | …un calcul se trompe |
| `UNIQUE (owner_id, name)` | deux lignes « tomates » pour le même utilisateur | …deux requêtes arrivent en même temps |

> Rappel : le modèle des objets est celui de la séance 1, simple et pas idéal. Le support le garde tel quel ; si votre groupe l'a amélioré, adaptez le schéma en conséquence.

`ItemService` vérifie déjà que le propriétaire existe (`404`). La clé étrangère ne fait pas double emploi : le service vérifie pour **répondre proprement** au client, la base garantit que **rien d'incohérent ne sera jamais enregistré**, quel que soit le chemin emprunté (un bug, une autre application, une commande tapée dans la console…).

Le schéma a changé : la table `users` existe déjà, mais pas `items`. Ici, `IF NOT EXISTS` suffit (le script crée `items` et ignore `users`). Gardez le réflexe pour la suite : schéma modifié → supprimer `data/` en cas de doute.

### 4.2 — L'entité et le nommage des colonnes

Annotez `Item` avec `@Table("items")` et `@Id`, comme `User`.

Le champ `ownerId` est enregistré dans la colonne `owner_id` sans rien préciser : Spring Data JDBC traduit automatiquement le `camelCase` Java en `snake_case` SQL. Le nom de la colonne doit donc **suivre** le nom du champ : une colonne nommée `owner` ou `ownerid` ne serait pas trouvée.

On garde `Long ownerId` (et pas un objet `User owner`) : Spring Data JDBC relie les entités par leurs **identifiants**, exactement comme la colonne `owner_id`. C'est voulu : un objet et son propriétaire sont deux choses qu'on enregistre séparément.

### 4.3 — Le repository : des requêtes déduites du nom

```java
public interface ItemRepository extends ListCrudRepository<Item, Long> {

    List<Item> findByOwnerId(Long ownerId);

    long countByOwnerId(Long ownerId);
}
```

En séance 2, on avait choisi ces noms de méthodes avec soin. Ce n'était pas qu'une convention : Spring Data **lit le nom** et en déduit la requête.

| Méthode | Requête générée |
|---|---|
| `findByOwnerId(Long ownerId)` | `SELECT … FROM items WHERE owner_id = ?` |
| `countByOwnerId(Long ownerId)` | `SELECT COUNT(*) FROM items WHERE owner_id = ?` |

Le nom se lit comme une phrase : `find` (chercher) / `count` (compter), `By`, puis la condition sur les champs de l'entité. Une faute dans un nom de champ (`findByOwner`) est détectée **au démarrage** : l'application refuse de démarrer, avec un message qui cite la méthode fautive. Voir la [liste des mots-clés reconnus](https://docs.spring.io/spring-data/relational/reference/jdbc/query-methods.html).

Supprimez `InMemoryItemRepository`, relancez, et testez le dossier **Objets** de Bruno. Vérifiez dans la console H2 :

```sql
SELECT * FROM items;
```

### 4.4 — La clé étrangère en action

Dans la console H2, essayez d'insérer un objet pour un utilisateur qui n'existe pas :

```sql
INSERT INTO items (owner_id, name, quantity) VALUES (99, 'radis', 5);
```

La base refuse : « Referential integrity constraint violation ». Essayez aussi `quantity` à `-1` : « Check constraint violation ». Aucune ligne de Java n'a été exécutée : c'est la base qui protège les données.

## Étape 5 — Troisième table : `trades`, à vous

### 5.1 — Le schéma

Écrivez la table `trades` dans `schema.sql`, à partir de l'entité `Trade` de la séance 2 :

- une colonne par champ, en `snake_case` (`proposerId` → `proposer_id`) ;
- `status` en `VARCHAR(20) NOT NULL` : Spring Data JDBC enregistre une `enum` sous forme de texte (`'PROPOSED'`), sans configuration ;
- **quatre clés étrangères** : deux vers `users`, deux vers `items`. Un troc a deux participants et deux objets, il est normal de référencer deux fois la même table ;
- toutes les colonnes `NOT NULL` ; les quantités, strictement positives.

<details>
<summary>Solution : la table trades</summary>

```sql
CREATE TABLE IF NOT EXISTS trades (
    id                  BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    status              VARCHAR(20) NOT NULL,
    proposer_id         BIGINT NOT NULL REFERENCES users(id),
    offered_item_id     BIGINT NOT NULL REFERENCES items(id),
    offered_quantity    INT NOT NULL CHECK (offered_quantity > 0),
    recipient_id        BIGINT NOT NULL REFERENCES users(id),
    requested_item_id   BIGINT NOT NULL REFERENCES items(id),
    requested_quantity  INT NOT NULL CHECK (requested_quantity > 0)
);
```

</details>

### 5.2 — L'entité et le repository

Annotez `Trade` (`@Table("trades")`, `@Id`), faites étendre `ListCrudRepository<Trade, Long>` à `TradeRepository` en gardant ses deux méthodes de recherche, et supprimez `InMemoryTradeRepository`.

Les deux méthodes de la séance 2 se traduisent telles quelles :

| Méthode | Requête générée |
|---|---|
| `findByProposerIdOrRecipientId(Long proposerId, Long recipientId)` | `… WHERE proposer_id = ? OR recipient_id = ?` |
| `countByProposerIdAndStatus(Long proposerId, TradeStatus status)` | `SELECT COUNT(*) … WHERE proposer_id = ? AND status = ?` |

<details>
<summary>Solution : TradeRepository</summary>

```java
public interface TradeRepository extends ListCrudRepository<Trade, Long> {

    // trocs où l'utilisateur est proposeur OU destinataire
    List<Trade> findByProposerIdOrRecipientId(Long proposerId, Long recipientId);

    long countByProposerIdAndStatus(Long proposerId, TradeStatus status);
}
```

</details>

### 5.3 — Vérifier

Il ne reste plus aucune classe `InMemory…` dans le projet, et pas une ligne de `UserService`, `ItemService` ou `TradeService` n'a dû changer. Si vous avez dû modifier beaucoup de choses dans un service, c'est un bon sujet de discussion pour le checkpoint : il dépendait sans doute d'une classe concrète plutôt que de l'interface.

Relancez, exécutez **Trocs > Proposer troc**, puis dans la console H2 :

```sql
SELECT * FROM trades;
```

La colonne `STATUS` contient le texte `PROPOSED`.

## Étape 6 — Ce que la base refuse désormais

Rejouez **toutes** les requêtes Bruno de la séance 2, dans l'ordre. Presque tout répond comme avant… sauf deux cas, qui répondent maintenant `500 Internal Server Error` :

1. **Créer objet (Alice)** exécutée deux fois (Alice déclare deux fois « tomates ») ;
2. `DELETE /users/1` alors qu'Alice possède des objets (modifiez l'URL de **Supprimer utilisateur**).

Regardez la console de l'application. La pile d'exception est longue : descendez jusqu'à la **dernière** ligne `Caused by`, c'est presque toujours elle qui dit la vérité :

```
Caused by: org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException:
  Unique index or primary key violation: "PUBLIC.CONSTRAINT_INDEX_5 ON PUBLIC.ITEMS(OWNER_ID, NAME …)"
```

C'est la base qui a refusé l'écriture, grâce aux contraintes de l'étape 4. Les données sont protégées. Mais un `500` n'est jamais une réponse volontaire : le client ne sait pas ce qu'il a mal fait. C'est au **service** de détecter le cas **avant** d'écrire, et de répondre proprement.

> **La règle à retenir** : le code vérifie (pour répondre un code clair au client), la base garantit (pour que rien d'incohérent ne soit enregistré, même si le code se trompe).

### 6.1 — Un nom d'objet unique par utilisateur

Ajoutez à `ItemRepository` :

```java
// WHERE owner_id = ? AND name = ? : au plus une ligne, grâce à UNIQUE (owner_id, name)
Optional<Item> findByOwnerIdAndName(Long ownerId, String name);
```

Dans `ItemService.create`, avant d'enregistrer : si cet utilisateur a déjà un objet de ce nom, répondez `409 Conflict` (la requête est valide, c'est l'état des données qui s'y oppose).

Attention à `update` : renommer les « tomates » d'Alice en « carottes » doit être refusé si elle a déjà des carottes, mais **pas** si on modifie seulement la quantité des tomates (la ligne trouvée est alors l'objet lui-même).

<details>
<summary>Solution : vérifier le nom dans create et update</summary>

```java
public Item create(Item item) {
    checkItem(item);
    if (items.countByOwnerId(item.getOwnerId()) >= props.maxPerUser()) {
        // ... inchangé
    }
    checkNameAvailable(item, null);
    item.setId(null);
    return items.save(item);
}

public Item update(Long id, Item item) {
    findById(id);
    checkItem(item);
    checkNameAvailable(item, id);
    item.setId(id);
    return items.save(item);
}

// id : l'objet en cours de modification (null pour une création), qui a le droit de porter ce nom
private void checkNameAvailable(Item item, Long id) {
    items.findByOwnerIdAndName(item.getOwnerId(), item.getName())
            .filter(existing -> !Objects.equals(existing.getId(), id))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "L'utilisateur " + item.getOwnerId() + " a déjà un objet « " + item.getName() + " »");
            });
}
```

`filter` garde le contenu de l'`Optional` seulement s'il respecte la condition : ici, si l'objet trouvé n'est pas celui qu'on modifie. `ifPresent` n'exécute son code que si l'`Optional` n'est pas vide.

</details>

> En séance 2, l'expérience sur `max-per-user` consistait à exécuter trois fois **Créer objet (Alice)**. Elle répond maintenant `409` dès la deuxième fois, pour une autre raison : pour la refaire, changez le nom de l'objet entre deux exécutions.

### 6.2 — Supprimer un utilisateur qui possède des objets : à vous de choisir

Deux réponses se défendent :

| Choix | Ce que fait `DELETE /users/1` | Pour | Contre |
|---|---|---|---|
| Refuser | `409 Conflict`, « l'utilisateur possède encore des objets » | aucune donnée perdue par erreur | le client doit d'abord supprimer les objets un par un |
| Supprimer en cascade | supprime d'abord ses objets, puis l'utilisateur | une seule requête | on peut perdre beaucoup de données d'un coup |

Choisissez, implémentez, et notez **pourquoi** dans un commentaire. Ce genre de choix justifié fait partie de l'évaluation du projet.

Indice pour le refus : `UserService` peut recevoir l'`ItemRepository` par son constructeur et utiliser `countByOwnerId`. Pas de cycle ici : `ItemService` dépend de `UserRepository`, pas de `UserService`.

<details>
<summary>Solution : le refus en 409</summary>

```java
public UserService(UserRepository users, ItemRepository items) {
    this.users = users;
    this.items = items;
}

public void delete(Long id) {
    findById(id);
    // La clé étrangère items.owner_id refuserait la suppression (500).
    // Choix : le signaler proprement par un 409 plutôt que supprimer ses objets en cascade.
    if (items.countByOwnerId(id) > 0) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "L'utilisateur " + id + " possède encore des objets");
    }
    users.deleteById(id);
}
```

</details>

> **Question pour le checkpoint** : la table `trades` a, elle aussi, des clés étrangères vers `users` et `items`. Que se passe-t-il si on supprime un objet qui apparaît dans un troc ? Essayez avec **Supprimer objet** (après avoir modifié son URL). Que faudrait-il répondre ?

## Avant le checkpoint : la liste de vérification

- [ ] L'application démarre sans erreur, et la console indique `Found 3 JDBC repository interfaces`.
- [ ] Plus aucune classe `InMemory…Repository` (ni `LoggingUserRepository`) dans le projet.
- [ ] `data/` est dans `.gitignore`.
- [ ] Les requêtes Bruno de la séance 2 répondent avec les mêmes codes qu'avant.
- [ ] Après un redémarrage, `GET /users`, `GET /items` et `GET /trades` renvoient toujours les données.
- [ ] Dans la console H2, `SELECT * FROM trades` montre le troc proposé.
- [ ] Un deuxième objet « tomates » pour Alice répond `409`, pas `500`.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `500` avec « Table "users" not found (candidates are: "USERS") » | `;CASE_INSENSITIVE_IDENTIFIERS=TRUE` absent de `spring.datasource.url` (étape 3.5). |
| `Syntax error in SQL statement "CREATE TABLE IF NOT EXISTS USER…"` | La table s'appelle `user`, un mot réservé : nommez-la `users`, et `@Table("users")`. |
| `Table "USERS" already exists` au deuxième démarrage | `IF NOT EXISTS` oublié dans `schema.sql`. |
| Une colonne ajoutée dans `schema.sql` est introuvable (`Column "…" not found`) | La table existait déjà : le `CREATE TABLE IF NOT EXISTS` a été ignoré. Arrêtez l'application, supprimez `data/`, relancez. |
| `Column "OWNERID" not found`, ou un champ toujours `null` à la lecture | La colonne ne suit pas le nom du champ : `ownerId` doit correspondre à `owner_id`. |
| `Referential integrity constraint violation` au démarrage | Dans `schema.sql`, une table est créée avant celle qu'elle référence : `users`, puis `items`, puis `trades`. |
| `No property 'owner' found for type 'Item'` au démarrage | Le nom d'une méthode du repository cite un champ qui n'existe pas (`findByOwner` au lieu de `findByOwnerId`). |
| `required a single bean, but 2 were found` | Une classe `InMemory…Repository` est restée, à côté de la classe générée par Spring. |
| `Failed to instantiate [fr.cours.troc.item.Item]` | Vous avez ajouté un constructeur avec paramètres : Spring ne sait plus créer l'objet à partir d'une ligne. Ajoutez un constructeur sans paramètre. |
| `@Id` sans effet, `id` toujours `null` dans la réponse | Mauvais import : `jakarta.persistence.Id` (JPA) au lieu de `org.springframework.data.annotation.Id`. |
| Console H2 : la table n'existe pas, ou la base est vide | L'URL de connexion ne correspond pas exactement à `spring.datasource.url` : vous êtes connecté à une autre base. |
| `404` sur `/h2-console` | Dépendance `spring-boot-h2console` absente, ou `spring.h2.console.enabled=true` oublié. |
| Console H2 : « Database may be already in use » | Une deuxième instance de l'application tourne déjà (vérifiez l'onglet **Run** d'IntelliJ) : un fichier H2 ne peut être ouvert que par une application à la fois. |

## En avance ? Features bonus

Commencez par la question du checkpoint (étape 6.2) : que répondre quand on supprime un objet qui apparaît dans un troc ?

### Bonus 5 — Une base pour chaque usage : le profil `demo`

Pour une démonstration, on veut repartir d'une base vide à chaque lancement, sans toucher au fichier de la base de développement.

1. Créez `src/main/resources/application-demo.properties` contenant uniquement :

   ```properties
   spring.datasource.url=jdbc:h2:mem:troc;CASE_INSENSITIVE_IDENTIFIERS=TRUE
   ```

2. Lancez l'application avec le profil `demo` : dans **Run > Edit Configurations…**, champ **Active profiles** : `demo` (ou la variable d'environnement `SPRING_PROFILES_ACTIVE=demo`).
3. Vérifiez : les données créées disparaissent à l'arrêt, et votre base `data/` n'a pas bougé.

Un fichier de profil ne contient que ce qui **change** : toutes les autres clés viennent toujours d'`application.properties` (séance 2, bloc 1, étape 6.5).

### Bonus 6 — Des données de démonstration : `data.sql`

Spring Boot exécute aussi, après `schema.sql`, un fichier `src/main/resources/data.sql` s'il existe. Écrivez-y les `INSERT` d'Alice, Bob et de leurs objets, pour ne plus avoir à les recréer avec Bruno.

Problème : avec une base fichier, ce script est rejoué à chaque démarrage. Que se passe-t-il au deuxième lancement ? Trouvez une solution (indice : combinez-le avec le bonus 5, ou cherchez la syntaxe [`MERGE INTO`](https://h2database.com/html/commands.html#merge_into) de H2).

### Bonus 7 — Dater les trocs

Ajoutez une colonne `proposed_at TIMESTAMP NOT NULL` à la table `trades`, et un champ `Instant proposedAt` à `Trade`, renseigné par `TradeService.propose` avec `Instant.now()`. N'oubliez pas : schéma modifié, donc `data/` à supprimer.

Le champ apparaît dans le JSON sous la forme `"2026-10-27T09:41:12.345Z"`. Au bloc 2, il servira à dater aussi la réponse du destinataire.

## Pour aller plus loin

- [Spring Data JDBC — documentation de référence](https://docs.spring.io/spring-data/relational/reference/jdbc.html), en particulier [le mapping des entités](https://docs.spring.io/spring-data/relational/reference/jdbc/mapping.html) et [les méthodes de requête](https://docs.spring.io/spring-data/relational/reference/jdbc/query-methods.html)
- [Spring Boot — Initialiser une base avec `schema.sql` et `data.sql`](https://docs.spring.io/spring-boot/how-to/data-initialization.html)
- [Spring Boot — La console H2](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.h2-web-console)
- [H2 — Fonctionnalités et options de l'URL de connexion](https://h2database.com/html/features.html)
- [Spring Guide — Accessing Relational Data using JDBC with Spring](https://spring.io/guides/gs/relational-data-access/) (le niveau en dessous de Spring Data : `JdbcTemplate`, qu'on voit passer dans les traces)
