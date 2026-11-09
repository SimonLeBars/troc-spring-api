# TP 5 — La persistance : H2 et Spring Data JDBC

> Rythme indicatif : séance 3. Point de départ : la mise en place du TP 4 terminée (`POST /trades` répond). Si ce n'est pas encore le cas, les utilisateurs et les objets suffisent pour commencer : `trades` vient en dernier.

## Le concept en bref

Jusqu'ici, les données vivent dans des `Map` Java, dans la mémoire de l'application. Une **base de données** les range ailleurs, dans un système fait pour ça, qui apporte :

- la **durabilité** : les données survivent à l'arrêt de l'application ;
- le **partage** : plusieurs instances de l'application voient les mêmes données ;
- des **garanties** : clés primaires, clés étrangères, contraintes, vérifiées par la base quel que soit le code qui écrit.

[Spring Data JDBC](https://docs.spring.io/spring-data/relational/reference/jdbc.html) écrit pour nous les classes d'accès à la base : on déclare une **interface** de repository, Spring génère l'implémentation en SQL. C'est le moment où l'injection de dépendances du TP 1 paie : les services dépendent d'une interface, on change ce qui se cache derrière sans les réécrire.

---

## Partie 1 — Expérimentation : les limites de la mémoire (facultative)

**Vous pouvez passer à la partie 2 si vous savez répondre à ces questions :**

- Qu'apporte une base de données par rapport à une `Map`, en dehors de la survie au redémarrage ?
- Qu'est-ce qu'une clé étrangère, et que refuse-t-elle ?
- Si le code vérifie déjà une règle (« le propriétaire existe »), à quoi sert une contrainte dans la base ?

Cette partie n'ajoute aucun code : elle observe l'application telle qu'elle est.

### Étape 1 — Redémarrer

Créez Alice, Bob et leurs objets dans Bruno. Arrêtez l'application, relancez-la, listez les utilisateurs.

### Étape 2 — Deux instances de l'application

En production, une application tourne souvent sur **plusieurs serveurs** à la fois, pour encaisser la charge ou survivre à une panne. Simulons-le : lancez une deuxième instance sur un autre port (TP 3), dans un terminal, pendant que la première tourne dans IntelliJ :

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Créez Alice sur le port 8080 (Bruno), puis listez les utilisateurs sur le port 8081 (<http://localhost:8081/users>). Qu'en pense un client dont les requêtes sont réparties entre les deux serveurs ? Arrêtez la deuxième instance.

### Étape 3 — Des données incohérentes

Toujours sur une seule instance :

1. Exécutez deux fois **Créer objet (Alice)** : combien de lignes « tomates » pour Alice ? Lors d'un troc, laquelle faudra-t-il débiter ?
2. Exécutez **Supprimer utilisateur** sur Alice (modifiez l'URL : `/users/1`), puis **Lister objets** : à qui appartiennent ses tomates ?
3. Imaginez un bug dans `ItemService` qui oublie de vérifier le propriétaire, ou une quantité calculée à `-3`. Qu'est-ce qui l'empêcherait d'être enregistré ?

### Ce qu'il faut retenir

| Problème de la `Map` | Ce qu'apporte la base |
|---|---|
| Tout disparaît au redémarrage | les données sont écrites sur disque |
| Chaque instance a sa propre `Map` | toutes les instances partagent la même base (c'était déjà le cœur du problème 1 du couplage fort, au TP 1) |
| Seul le code vérifie les règles : un oubli, et la donnée incohérente est enregistrée | des **contraintes** (clé étrangère, `UNIQUE`, `CHECK`) refusent l'écriture, quel que soit le code qui la tente |

> **La règle à retenir** : le code vérifie (pour répondre un code clair au client), la base garantit (pour que rien d'incohérent ne soit enregistré, même si le code se trompe).

---

## Partie 2 — Mise en place : tout en base (obligatoire)

### Objectifs

1. Trois tables `users`, `items`, `trades`, avec leurs clés primaires, leurs clés étrangères et leurs contraintes :
   - `items` : propriétaire existant, quantité jamais négative, **un seul** objet d'un nom donné par propriétaire ;
   - `trades` : deux participants et deux objets existants, quantités strictement positives, toutes les colonnes obligatoires.
2. Les classes `InMemory…Repository` ont disparu du projet : il ne reste que des interfaces.
3. Toutes les requêtes Bruno des TP précédents répondent avec les mêmes codes qu'avant.
4. On redémarre l'application : les données sont toujours là, et on les voit dans la console H2.
5. Les deux cas que la base refuse désormais répondent proprement, pas en `500` :
   - un deuxième objet du même nom pour le même utilisateur (à la création comme à la modification) : `409` ;
   - la suppression d'un utilisateur qui possède des objets : à vous de choisir (voir plus bas).

On avance **une table à la fois** : `users` (la plus simple), puis `items` (la première clé étrangère), puis `trades`. À chaque table, l'application doit redémarrer et répondre avant de passer à la suivante.

> Organisation conseillée pour un groupe de 4 : tout le monde ensemble sur les dépendances, la configuration et la table `users` (c'est là que sont la plupart des pièges). Ensuite, une personne par table pour `items` et `trades`, la dernière prépare les vérifications.

### Les dépendances

Ajoutez dans `pom.xml`, puis rechargez Maven (**Load Maven Changes**) :

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

- **H2** est une base écrite en Java : elle tourne **dans** votre application, rien à installer. Le même code fonctionnerait avec PostgreSQL ou MySQL en changeant cette dépendance et l'URL de connexion.
- `<scope>runtime</scope>` : votre code n'utilise jamais H2 directement, il passe par Spring.
- `spring-boot-h2console` : depuis Spring Boot 4, la console web de H2 est un module séparé.

> **Pourquoi Spring Data JDBC et pas JPA ?** JPA (avec Hibernate) est très répandu en entreprise : il génère le schéma et le SQL, suit les objets modifiés et les enregistre « tout seul ». Puissant, mais beaucoup de choses se passent sans qu'on les voie. Avec Spring Data JDBC, on écrit le schéma à la main, et un objet n'est enregistré que si on appelle `save()`. Les clés, les contraintes et les requêtes restent visibles : c'est ce qu'on veut apprendre ici.

### La configuration

Ajoutez dans `application.properties` :

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

- `jdbc:h2:file:./data/troc` : la base est le fichier `data/troc.mv.db`. Avec `jdbc:h2:mem:troc`, elle vivrait en mémoire et disparaîtrait à l'arrêt.
- `CASE_INSENSITIVE_IDENTIFIERS=TRUE` : indispensable, expliqué plus bas.
- `sa` et un mot de passe vide : les identifiants par défaut de H2. Une vraie base aurait un mot de passe, fourni par une variable d'environnement (TP 3), jamais écrit dans git.
- `spring.sql.init.mode=always` : par défaut, Spring Boot n'exécute `schema.sql` que pour une base en mémoire.
- La dernière ligne affiche le SQL envoyé : la meilleure façon de comprendre ce que fait Spring Data JDBC.

La base est un fichier de données propre à chaque poste : ajoutez `data/` au `.gitignore`.

### Syntaxes utiles

#### Le schéma : `src/main/resources/schema.sql`

La première table, en entier :

```sql
-- "users" et non "user" : USER est un mot réservé en SQL.
CREATE TABLE IF NOT EXISTS users (
    id        BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name      VARCHAR(100) NOT NULL
);
```

Les éléments de syntaxe pour les tables suivantes :

| Syntaxe | Sens |
|---|---|
| `id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY` | **clé primaire**, attribuée par la base à l'insertion (1, 2, 3…) : le compteur `AtomicLong` disparaît |
| `NOT NULL` | colonne obligatoire |
| `owner_id BIGINT NOT NULL REFERENCES users(id)` | **clé étrangère** : la valeur doit exister dans `users.id`, et la ligne de `users` ne peut plus être supprimée tant qu'elle est référencée |
| `quantity INT NOT NULL CHECK (quantity >= 0)` | **contrainte de vérification** : la base refuse toute valeur qui ne la respecte pas |
| `UNIQUE (owner_id, name)` (en fin de table) | **unicité** : deux lignes ne peuvent pas avoir le même couple de valeurs |
| `status VARCHAR(20) NOT NULL` | une `enum` Java est enregistrée sous forme de texte (`'PROPOSED'`) |

- Le script est exécuté **dans l'ordre** : une table doit être créée **après** celles qu'elle référence.
- Une table peut référencer deux fois la même table (un troc a deux participants : deux clés étrangères vers `users`).
- `IF NOT EXISTS` : le script est rejoué à chaque démarrage ; sans cette précaution, le deuxième démarrage échoue.

> **Corollaire important** : comme la table existe déjà, modifier ensuite son `CREATE TABLE` **n'a aucun effet** sur la base existante. Pendant le cours : arrêtez l'application, **supprimez le dossier `data/`**, relancez (la base est recréée, vide). En entreprise, on utilise un outil de migration comme [Flyway](https://documentation.red-gate.com/flyway) ou [Liquibase](https://www.liquibase.com/).

#### L'entité : une ligne de la table

```java
@Table("users")       // org.springframework.data.relational.core.mapping.Table
public class User {

    @Id               // org.springframework.data.annotation.Id
    private Long id;  // null → la base attribuera l'id à l'insertion
    …
}
```

- Attention, IntelliJ propose aussi un `@Id` de `jakarta.persistence` : c'est celui de JPA, pas le bon.
- Un champ `camelCase` correspond à une colonne `snake_case`, sans rien préciser : `ownerId` ↔ `owner_id`. Le nom de la colonne doit donc **suivre** celui du champ.
- Spring Data JDBC relie les entités par leurs **identifiants** (`Long ownerId`), pas par des objets (`User owner`), exactement comme la colonne `owner_id`.

#### Le repository : l'interface suffit

```java
public interface UnRepository extends ListCrudRepository<UneEntite, Long> { // org.springframework.data.repository
    // rien d'autre pour save, findAll, findById, deleteById, count, existsById…
}
```

Au démarrage, Spring Data JDBC **génère une classe** qui implémente l'interface en SQL et l'enregistre comme bean. Les services la reçoivent sans le savoir.

`save()` choisit seul la requête : `INSERT` si l'`id` est `null` (l'`id` attribué par la base est alors recopié dans l'objet renvoyé), `UPDATE … WHERE id = ?` sinon.

#### Des requêtes déduites du nom de la méthode

```java
List<Item> findByOwnerId(Long ownerId);   // SELECT … FROM items WHERE owner_id = ?
long countByOwnerId(Long ownerId);        // SELECT COUNT(*) FROM items WHERE owner_id = ?
```

Le nom se lit comme une phrase : `find` ou `count`, `By`, puis les conditions sur les **champs** de l'entité, reliées par `And` ou `Or`. Une faute dans un nom de champ (`findByOwner`) est détectée **au démarrage**, avec un message qui cite la méthode. Voir la [liste des mots-clés reconnus](https://docs.spring.io/spring-data/relational/reference/jdbc/query-methods.html). Les noms choisis aux TP 2 à 4 fonctionnent tels quels.

### Indices

- Supprimez chaque classe `InMemory…Repository` au moment où son interface étend `ListCrudRepository` : si elle restait, Spring trouverait deux beans du même type.
- Si vous aviez une classe `LoggingUserRepository` (bonus du TP 1), supprimez-la aussi : elle ne compile plus, et les traces SQL la remplacent.
- Pour répondre `409` au lieu de laisser la base refuser en `500`, le service doit vérifier **avant** d'écrire. Une méthode `Optional<Item> findByOwnerIdAndName(Long ownerId, String name)` donne au plus une ligne, grâce à `UNIQUE (owner_id, name)`.
- À la **modification**, attention : renommer les « tomates » d'Alice en « carottes » doit être refusé si elle a déjà des carottes, mais **pas** si on change seulement la quantité des tomates (la ligne trouvée est alors l'objet lui-même). Sur un `Optional`, `filter(…)` garde le contenu s'il respecte une condition, et `ifPresent(…)` n'agit que s'il n'est pas vide.

#### Supprimer un utilisateur qui possède des objets : à vous de choisir

| Choix | Ce que fait `DELETE /users/1` | Pour | Contre |
|---|---|---|---|
| Refuser | `409`, « l'utilisateur possède encore des objets » | aucune donnée perdue par erreur | le client doit d'abord supprimer les objets un par un |
| Supprimer en cascade | supprime d'abord ses objets, puis l'utilisateur | une seule requête | on peut perdre beaucoup de données d'un coup |

Choisissez, implémentez, et notez **pourquoi** dans un commentaire. `UserService` peut recevoir l'`ItemRepository` par son constructeur : pas de cycle, tant que ce sont des repositories et non des services qui se croisent.

### Vérifier

#### Le SQL dans la console

Au démarrage, cherchez `Found 3 JDBC repository interfaces` et `H2 console available at '/h2-console'`. Pendant chaque requête Bruno, le SQL envoyé s'affiche :

```
Executing prepared SQL statement [INSERT INTO "users" ("NAME") VALUES (?)]
```

> **Pourquoi `CASE_INSENSITIVE_IDENTIFIERS` ?** Spring Data JDBC entoure les noms de guillemets (`"users"`). En SQL, un nom entre guillemets est **sensible à la casse** ; un nom sans guillemets ne l'est pas, et H2 range le `CREATE TABLE users` sous le nom `USERS`. Sans l'option, `"users"` et `USERS` seraient deux tables différentes : « Table "users" not found ».

#### La console H2

Ouvrez <http://localhost:8080/h2-console>, avec pour **JDBC URL** **exactement** la valeur de `spring.datasource.url`, **User Name** `sa`, mot de passe vide (les valeurs proposées par défaut désignent d'autres bases, vides). Puis :

```sql
SELECT * FROM users;
SELECT * FROM items;
SELECT * FROM trades;
```

Essayez aussi d'écrire directement des données incohérentes, sans passer par Java :

```sql
INSERT INTO items (owner_id, name, quantity) VALUES (99, 'radis', 5);
```

La base refuse (« Referential integrity constraint violation »). Faites de même avec une quantité à `-1`.

> La console accède directement à la base : un `DELETE` tapé ici s'applique pour de vrai. C'est un outil de développement, qu'on n'active jamais en production.

#### Liste de vérification

- [ ] L'application démarre, et la console indique `Found 3 JDBC repository interfaces`.
- [ ] Plus aucune classe `InMemory…Repository` dans le projet.
- [ ] `data/` est dans `.gitignore`.
- [ ] Les requêtes Bruno des TP 2 à 4 répondent avec les mêmes codes qu'avant.
- [ ] Après un redémarrage, `GET /users`, `GET /items` et `GET /trades` renvoient toujours les données.
- [ ] La console H2 refuse un objet pour un utilisateur inexistant, et une quantité négative.
- [ ] Un deuxième objet « tomates » pour Alice répond `409`, pas `500` ; modifier seulement la quantité des tomates fonctionne toujours.
- [ ] `DELETE /users/1` sur Alice, qui possède des objets, répond selon votre choix (pas `500`), justifié dans un commentaire.
- [ ] Les services n'ont (presque) pas changé : si vous avez dû beaucoup les modifier, c'est un bon sujet pour le checkpoint.

> L'expérience `max-per-user` du TP 3 (trois fois **Créer objet (Alice)**) répond maintenant `409` dès la deuxième fois, pour une autre raison : changez le nom de l'objet entre deux exécutions pour la refaire.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `500` avec « Table "users" not found (candidates are: "USERS") » | `;CASE_INSENSITIVE_IDENTIFIERS=TRUE` absent de `spring.datasource.url`. |
| `Syntax error in SQL statement "CREATE TABLE IF NOT EXISTS USER…"` | La table s'appelle `user`, un mot réservé : nommez-la `users`, et `@Table("users")`. |
| `Table "USERS" already exists` au deuxième démarrage | `IF NOT EXISTS` oublié. |
| Une colonne ajoutée dans `schema.sql` est introuvable | La table existait déjà : arrêtez l'application, supprimez `data/`, relancez. |
| `Column "OWNERID" not found`, ou un champ toujours `null` à la lecture | La colonne ne suit pas le nom du champ : `ownerId` doit correspondre à `owner_id`. |
| `Referential integrity constraint violation` au démarrage | Une table est créée avant celle qu'elle référence. |
| `No property 'owner' found for type 'Item'` au démarrage | Une méthode du repository cite un champ qui n'existe pas. |
| `required a single bean, but 2 were found` | Une classe `InMemory…Repository` est restée. |
| `Failed to instantiate [fr.cours.troc.item.Item]` | Un constructeur avec paramètres a été ajouté : ajoutez aussi un constructeur sans paramètre. |
| `@Id` sans effet, `id` toujours `null` | Mauvais import : `jakarta.persistence.Id` au lieu de `org.springframework.data.annotation.Id`. |
| Console H2 : la table n'existe pas, ou la base est vide | L'URL saisie ne correspond pas exactement à `spring.datasource.url`. |
| `404` sur `/h2-console` | Dépendance `spring-boot-h2console` absente, ou `spring.h2.console.enabled=true` oublié. |
| Console H2 : « Database may be already in use » | Une deuxième instance de l'application tourne : un fichier H2 ne peut être ouvert que par une application à la fois. |

## Bonus

### Bonus 1 — Supprimer un objet engagé dans un troc

La table `trades` a des clés étrangères vers `items`. Que se passe-t-il si on supprime un objet qui apparaît dans un troc ? Essayez avec **Supprimer objet**. Que faudrait-il répondre ? Implémentez-le.

### Bonus 2 — Une base jetable : le profil `demo`

Créez `src/main/resources/application-demo.properties` contenant uniquement `spring.datasource.url=jdbc:h2:mem:troc;CASE_INSENSITIVE_IDENTIFIERS=TRUE`, et lancez avec le profil `demo` (**Active profiles**, ou `SPRING_PROFILES_ACTIVE=demo`). Les données disparaissent à l'arrêt, et votre base `data/` n'a pas bougé.

### Bonus 3 — Des données de démonstration : `data.sql`

Spring Boot exécute aussi, après `schema.sql`, un fichier `src/main/resources/data.sql` s'il existe. Écrivez-y les `INSERT` d'Alice, Bob et de leurs objets. Que se passe-t-il au deuxième lancement avec une base fichier ? Trouvez une solution (indice : le bonus 2, ou la syntaxe [`MERGE INTO`](https://h2database.com/html/commands.html#merge_into) de H2).

### Bonus 4 — Dater les trocs

Ajoutez une colonne `proposed_at TIMESTAMP NOT NULL` et un champ `Instant proposedAt`, renseigné par `propose` avec `Instant.now()`. Schéma modifié : `data/` à supprimer. Le champ apparaît dans le JSON sous la forme `"2026-10-27T09:41:12.345Z"`.

## Pour aller plus loin

- [Spring Data JDBC — documentation de référence](https://docs.spring.io/spring-data/relational/reference/jdbc.html), en particulier [le mapping des entités](https://docs.spring.io/spring-data/relational/reference/jdbc/mapping.html) et [les méthodes de requête](https://docs.spring.io/spring-data/relational/reference/jdbc/query-methods.html)
- [Spring Boot — Initialiser une base avec `schema.sql` et `data.sql`](https://docs.spring.io/spring-boot/how-to/data-initialization.html)
- [Spring Boot — La console H2](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.h2-web-console)
- [H2 — Fonctionnalités et options de l'URL de connexion](https://h2database.com/html/features.html)
- [Spring Guide — Accessing Relational Data using JDBC with Spring](https://spring.io/guides/gs/relational-data-access/) (le niveau en dessous : `JdbcTemplate`, qu'on voit passer dans les traces)
