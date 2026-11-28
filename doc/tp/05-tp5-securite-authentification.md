# TP 5 — Sécuriser l'API : authentification par jeton

> Rythme indicatif : séance 2, à terminer entre les séances si besoin : **la séance 3 part de ce TP terminé**. Point de départ : la mise en place du TP 4 terminée (`POST /trades` répond). Peut commencer dès le TP 2 terminé, en parallèle des TP 3 et 4 : l'inscription et la connexion ne touchent que les utilisateurs ; seule l'étape « proposer un troc » attend le TP 4. Comptez environ 2 h pour la mise en place.
>
> Support complémentaire : l'annexe du cours `doc/cours/05-annexe-spring-security.html`, qui explique le mécanisme en détail, avec des schémas.

## Le concept en bref

Depuis le TP 4, le client **se déclare** lui-même : `"proposerId": 1` pour proposer un troc. Le serveur le croit sur parole. N'importe qui peut donc se faire passer pour n'importe qui.

Sécuriser une API, c'est répondre à deux questions, **toujours dans cet ordre** :

| Question | Nom | Si la réponse est non |
|---|---|---|
| Qui es-tu ? | **authentification** : on le prouve (mot de passe, puis jeton) | `401 Unauthorized` (le nom est trompeur : il signifie « non authentifié ») |
| As-tu le droit ? | **autorisation** : décidée une fois l'identité connue | `403 Forbidden` |

Ce TP traite surtout la première question, et une première réponse à la seconde : chacun ne peut modifier ou supprimer que **son** compte. Au TP 7, la même logique décidera que seul le destinataire d'un troc peut l'accepter : c'est le jeton qui dira qui il est.

Le mécanisme de ce TP est celui de la plupart des API actuelles :

1. Le client envoie son identifiant et son mot de passe **une seule fois**, à `POST /auth/token`.
2. Le serveur vérifie le mot de passe, puis renvoie un **jeton** (JWT) : un petit texte qui dit « c'est l'utilisateur 1, jusqu'à 15 h », **signé** avec une clé que seul le serveur connaît.
3. Le client joint ce jeton à chaque requête suivante, dans l'en-tête `Authorization: Bearer <jeton>`. Le serveur vérifie la signature : si quelqu'un a modifié le jeton, la signature ne correspond plus, et la réponse est `401`.

[Spring Security](https://docs.spring.io/spring-security/reference/) fait tout le travail de vérification. Vous lui fournissez des beans (où trouver les comptes, comment vérifier un mot de passe, quelle clé utiliser, quelles routes protéger), et il les branche au bon endroit : c'est encore l'inversion de contrôle du TP 1.

---

## Partie 1 — Expérimentation : se faire passer pour quelqu'un d'autre (facultative)

**Vous pouvez passer à la partie 2 si vous savez répondre à ces questions :**

- Quelle est la différence entre un `401` et un `403` ?
- Pourquoi ne stocke-t-on jamais un mot de passe, même chiffré ? Que stocke-t-on à la place ?
- Un JWT est-il chiffré ? Qu'est-ce qui empêche un client de modifier son contenu ?
- Que se passe-t-il quand on ajoute `spring-boot-starter-security` à un projet, sans rien configurer ?

### Étape 1 — Jouez Mallory

Mallory est le prénom traditionnel de l'attaquant dans les exemples de sécurité. Avec la collection Bruno du projet :

1. Créez Alice, Bob et un troisième utilisateur, Mallory (dossier **Utilisateurs** : Alice a l'`id` 1, Bob le 2, Mallory le 3).
2. Créez un objet pour Alice et un pour Bob (**Objets > Créer objet (Alice)** et **Créer objet (Bob)** : 10 tomates, objet 1 ; 5 carottes, objet 2).
3. Vous êtes maintenant Mallory. Votre `id` est 3, mais rien ne vous empêche d'écrire autre chose. Faites proposer par **Bob** un troc à Alice : modifiez le corps de **Trocs > Proposer troc**, avec `"proposerId": 2`, `"offeredItemId": 2` (ses carottes), `"recipientId": 1`, `"requestedItemId": 1` (les tomates d'Alice).
4. Toujours en tant que Mallory, renommez Bob (**Utilisateurs > Modifier utilisateur** sur `/users/2`), puis supprimez son compte (**Supprimer utilisateur** sur `/users/2`).

Puis répondez :

- Quels codes reçoit Mallory ? Bob a-t-il un moyen de prouver qu'il n'a jamais proposé ce troc ?
- Quelle règle du TP 4 aurait dû l'en empêcher ? Pourquoi ne protège-t-elle rien ?
- Que faudrait-il que le serveur sache, et qu'il ne sait pas aujourd'hui ?

Les règles métier sont bonnes : c'est l'information d'entrée, l'identité, qui n'est pas fiable.

### Étape 2 — Ajouter Spring Security, sans rien configurer

Ajoutez cette dépendance dans `pom.xml`, rechargez Maven et relancez l'application :

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Observez, et notez ce que vous constatez :

| Essai | Ce que vous observez |
|---|---|
| La console au démarrage | une ligne `Using generated security password: …` |
| `GET /users` dans Bruno | ? |
| <http://localhost:8080/users> dans un navigateur | ? |
| `GET /users` dans Bruno, onglet **Auth** > **Basic Auth**, identifiant `user`, mot de passe de la console | ? |
| `POST /users` avec le même Basic Auth | ? |

Pour comprendre le refus du `POST`, ajoutez `logging.level.org.springframework.security=DEBUG` dans `application.properties`, relancez, et rejouez la requête : cherchez `CSRF` dans la console.

> Spring Security est **sécurisé par défaut** : tout est fermé, avec un compte de démonstration et une protection contre les attaques CSRF (un site malveillant qui ferait envoyer une requête par le navigateur de la victime). C'est ensuite à vous d'ouvrir ce qui doit l'être.

### Étape 3 — Hacher un mot de passe

La dépendance de l'étape 2 contient aussi les outils de hachage. Créez la classe suivante dans `fr.cours.troc.sandbox.security`, puis lancez son `main` (flèche verte dans la marge) :

```java
package fr.cours.troc.sandbox.security;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

public class HashDemo {

    public static void main(String[] args) {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        String hash1 = encoder.encode("tomate42");
        String hash2 = encoder.encode("tomate42");
        System.out.println("haché 1 : " + hash1);
        System.out.println("haché 2 : " + hash2);

        System.out.println("tomate42 correspond ? " + encoder.matches("tomate42", hash1));
        System.out.println("tomate43 correspond ? " + encoder.matches("tomate43", hash1));

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            encoder.encode("essai" + i);
        }
        System.out.println("10 hachages : " + (System.currentTimeMillis() - start) + " ms");
    }
}
```

Répondez :

1. Le même mot de passe donne deux hachés différents. Comment `matches` peut-il quand même répondre `true` ? (Indice : le haché contient un **sel**, une valeur aléatoire.)
2. Combien de temps prend un hachage ? Pourquoi est-ce une bonne chose ? Pensez à un attaquant qui a volé les données et teste des milliards de mots de passe.
3. À quoi sert le préfixe `{bcrypt}` ?

### Étape 4 — Lire un jeton

Voici un vrai JWT, sur une seule ligne :

```
eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0cm9jLWFwaSIsInN1YiI6IjEiLCJ1c2VybmFtZSI6ImFsaWNlIiwicm9sZXMiOlsiVVNFUiJdLCJpYXQiOjE3OTI2NjMyMDAsImV4cCI6MTc5MjY2NjgwMH0.DBlqUDT6esOk_0nL5sONZGZgYbHN0fF7JWTQWDgTmGo
```

1. Collez-le dans le débogueur de [jwt.io](https://jwt.io/). Quelles sont ses trois parties ? Qui est l'utilisateur, et jusqu'à quand le jeton est-il valable ? (`iat` et `exp` sont des secondes depuis le 1er janvier 1970.)
2. Dans le champ de la clé de vérification, saisissez `dev-secret-de-32-caracteres-min!!` : la signature est déclarée valide.
3. Remplacez `"sub": "1"` par `"sub": "2"` dans le contenu. Que devient la signature ? Mallory peut-elle fabriquer un jeton au nom de Bob sans connaître la clé ?

### Ce qu'il faut retenir

- **Le client ne doit jamais déclarer son identité** dans le corps de la requête : il doit la **prouver**. Dans la vraie vie, une API qui croit le client sur parole laisse n'importe qui passer une commande au nom d'un autre, ou clôturer le compte d'un client.
- **Un mot de passe ne se stocke pas, seul son haché se stocke.** Les données finissent par fuiter (sauvegarde perdue, faille), et les gens réutilisent leurs mots de passe partout. BCrypt est lent exprès et salé : même volés, les hachés ne révèlent pas les mots de passe.
- **Un JWT est encodé, pas chiffré** : tout le monde peut le lire, donc jamais de secret dedans. Mais personne ne peut le modifier sans la clé : sa signature garantit son contenu.
- **Sécurisé par défaut** : ajouter Spring Security ferme tout. La configuration consiste à dire ce qu'on ouvre, et comment on s'authentifie.

Supprimez le package `sandbox`, ainsi que la ligne `logging.level.org.springframework.security=DEBUG` si vous ne voulez pas la garder. Vous pouvez garder la dépendance : la mise en place en a besoin.

---

## Partie 2 — Mise en place : des comptes, des jetons, des routes protégées (obligatoire)

### Objectifs

| Route | Accès | Comportement attendu |
|---|---|---|
| `POST /users` | ouvert | **inscription** : `{ "name", "username", "password" }` → `201`. La réponse ne contient **jamais** le mot de passe ni son haché. `username` déjà pris → `409`. |
| `POST /auth/token` | ouvert | **connexion** : `{ "username", "password" }` → `200` et `{ "token": "eyJ…", "expiresIn": 3600 }`. Identifiants faux → `401`, avec le **même message** que le compte existe ou non. |
| toutes les autres | jeton exigé | sans jeton, avec un jeton modifié ou expiré → `401`. |
| `PUT /users/{id}`, `DELETE /users/{id}` | jeton exigé | seulement sur **son propre** compte : l'`id` du chemin doit être celui du jeton, sinon `403`. |
| `POST /trades` | jeton exigé | le proposeur est l'utilisateur **du jeton** : `proposerId` disparaît du corps. |

Et aussi :

1. Le mot de passe est conservé **haché** (BCrypt), jamais en clair.
2. **Aucune** réponse JSON ne contient de haché : ni `POST /users`, ni `GET /users`, ni `GET /users/{id}`, ni `PUT /users/{id}`.
3. La clé qui signe les jetons **n'est pas dans git** : elle vient de la variable d'environnement `TROC_JWT_SECRET`.
4. Dans Bruno, une requête de connexion enregistre le jeton, et toutes les autres requêtes l'envoient automatiquement.

Les données restent **en mémoire** : les comptes disparaissent au redémarrage, comme le reste. La base arrive au TP 6.

> Ordre conseillé : dépendances, puis comptes et inscription, puis jetons et connexion, puis règles d'accès, puis l'identité lue dans le jeton (son compte, puis le proposeur), puis Bruno. Organisation possible pour un groupe de 4 : comptes et inscription (1) ; clé, jetons et `POST /auth/token` (2) ; règles d'accès, puis `POST /trades` (3) ; `PUT` et `DELETE /users/{id}` sur son compte, puis Bruno et les vérifications (4). Les personnes 1 et 2 se mettent d'accord d'abord sur l'entité `User` et le `UserDetailsService`, qui servent aux deux.

### Les dépendances

```xml
<!-- Authentification et autorisation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<!-- Vérifier les jetons JWT reçus dans l'en-tête "Authorization: Bearer" -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

Avec Spring Boot 4, les starters liés à la sécurité commencent par `spring-boot-starter-security-`. Les tutoriels pour Boot 3 écrivent `spring-boot-starter-oauth2-resource-server` : ce nom n'existe plus.

> Le vocabulaire **OAuth2** vient de la norme qui a popularisé ces jetons. On n'en utilise qu'une petite partie : un *resource server* est une API qui accepte des jetons.

La sécurité est une fonctionnalité transverse : elle n'appartient ni à `user`, ni à `item`, ni à `trade`. Rangez ses classes dans un package `fr.cours.troc.security`.

### Syntaxes utiles

#### Les comptes

Un compte, c'est deux champs de plus dans l'entité `User` :

| Champ | Rôle | Règle |
|---|---|---|
| `username` | l'identifiant de connexion (`alice`) ; `name` reste le nom affiché (`Alice Martin`) | obligatoire, **unique** |
| `passwordHash` | le haché BCrypt, jamais le mot de passe | obligatoire |

Il faudra retrouver un compte par son identifiant. Ajoutez au repository une méthode qui renvoie un `Optional<User>` (vide si personne n'a cet identifiant), et nommez-la `findByUsername` : comme `findByOwnerId` au TP 2, ce nom servira au TP 6. En mémoire, un stream suffit, terminé par `.findFirst()`.

#### Où Spring trouve les comptes : `UserDetailsService`

Spring Security ne connaît pas votre repository. Il appelle un bean `UserDetailsService`, que vous fournissez :

```java
// org.springframework.security.core.userdetails
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

Il est appelé **uniquement à la connexion**. `UserDetails` se construit avec un builder de Spring Security, qui porte lui aussi le nom `User`. Comme votre entité s'appelle déjà `User`, écrivez le nom complet :

```java
org.springframework.security.core.userdetails.User
        .withUsername(...)
        .password(...)   // le haché enregistré
        .roles("USER")   // un droit, ROLE_USER : tous les comptes sont des utilisateurs ordinaires
        .build();
```

Si le compte n'existe pas, levez une `UsernameNotFoundException`. Vous pouvez écrire une classe `@Service` qui implémente l'interface, ou un `@Bean` qui renvoie une lambda : l'interface n'a qu'une méthode.

#### Hacher et vérifier les mots de passe

Deux beans, à déclarer dans une classe `@Configuration` (par exemple `SecurityConfig`) :

```java
@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // BCrypt, avec le préfixe {bcrypt}
}

@Bean
AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
    var provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
}
```

- `passwordEncoder.encode(motDePasse)` donne le haché à enregistrer à l'inscription.
- L'`AuthenticationManager` sert à la connexion : il charge le compte avec votre `UserDetailsService`, puis compare le mot de passe reçu au haché. Vous l'appellerez depuis `POST /auth/token`.

#### L'inscription

`POST /users` reçoit un **objet de requête** (TP 4) avec `name`, `username` et `password`, et renvoie un **objet de réponse** sans haché. Trois questions à vous poser avant d'écrire le code :

- Le client choisit son mot de passe. Que doit-il pouvoir choisir d'autre ? Que se passerait-il si l'objet de requête recevait directement l'entité `User`, avec son champ `passwordHash` ?
- Aujourd'hui, `GET /users` et `PUT /users/{id}` renvoient l'entité `User`. Que contiendra leur JSON une fois `passwordHash` ajouté à l'entité ?
- Si `PUT /users/{id}` enregistre l'entité reçue dans le corps, qui ne contient pas de haché, que devient le mot de passe de l'utilisateur ?

#### La clé et les jetons

La clé de signature est un **secret** : qui la connaît peut fabriquer un jeton au nom de n'importe qui. Elle vient donc de l'environnement, jamais d'un fichier versionné :

```properties
# application.properties
troc.jwt.secret=${TROC_JWT_SECRET}
troc.jwt.ttl=1h
```

`${TROC_JWT_SECRET}` lit une variable d'environnement. Sans elle, l'application ne démarre pas : c'est voulu. Dans IntelliJ, renseignez-la dans **Run > Edit Configurations…** > **Environment variables**, avec une valeur d'au moins 32 caractères : HS256 exige une clé de 256 bits. Si vous avez fait le bonus « profils » du TP 3, un `application-dev.properties` non versionné convient aussi.

Lisez ces deux clés avec un record `@ConfigurationProperties("troc.jwt")`, comme au TP 3. Un `Duration` comprend directement `1h`, `30m`…

Les beans qui signent et vérifient les jetons :

```java
@Bean
SecretKey jwtKey(...) {
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"); // javax.crypto.spec
}

@Bean
JwtEncoder jwtEncoder(SecretKey key) {
    return NimbusJwtEncoder.withSecretKey(key).build();
}

@Bean
JwtDecoder jwtDecoder(SecretKey key) {
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
}
```

Fabriquer un jeton (tous ces types sont dans `org.springframework.security.oauth2.jwt`) :

```java
JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("troc-api")
        .subject(...)                    // qui : l'id de l'utilisateur, en String
        .claim("username", ...)          // un champ libre, lisible par le client
        .issuedAt(maintenant)
        .expiresAt(maintenant.plus(...)) // Instant.plus(Duration)
        .build();
JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build(); // sans lui, l'encodeur attend une clé RSA
String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
```

#### La connexion

```java
authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(username, password));
```

Si les identifiants sont faux, `authenticate` lève une `AuthenticationException` : transformez-la en `401`. Sinon, le compte existe et le mot de passe est bon. Fabriquez le jeton, et renvoyez-le avec sa durée de vie en secondes (`Duration.toSeconds()`).

#### Les règles d'accès : `SecurityFilterChain`

Les règles sont décrites par un bean :

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/chemin", "/autre").permitAll() // ouvert à tous
            .requestMatchers("/un-dossier/**").permitAll()
            .anyRequest().authenticated())                                     // toujours en dernier
        .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))        // jeton Bearer exigé
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return http.build();
}
```

- Les règles se lisent de haut en bas : la **première** qui correspond s'applique.
- `oauth2ResourceServer(… jwt …)` installe le filtre qui lit `Authorization: Bearer …` et vérifie le jeton avec votre `JwtDecoder`.
- `csrf.disable()` : la protection CSRF protège les cookies, que le navigateur envoie tout seul. Un jeton n'est jamais envoyé tout seul, donc la protection n'a pas d'objet ici. `STATELESS` : Spring ne crée pas de session, le jeton suffit.
- Dès que ce bean existe, le compte `user` et son mot de passe généré disparaissent, ainsi que le formulaire de connexion et HTTP Basic.

À vous de décider quelles routes ouvrir : relisez le tableau des objectifs. Si vous avez fait l'annexe 1, pensez à Swagger UI (elle donne aussi le bouton **Authorize**, pour essayer les routes protégées).

#### Lire l'identité dans un contrôleur

```java
@PostMapping
public ResponseEntity<Trade> propose(@RequestBody ProposeTradeRequest request, @AuthenticationPrincipal Jwt jwt) {
    Long proposerId = Long.valueOf(jwt.getSubject());
    ...
}
```

`@AuthenticationPrincipal` injecte le jeton **déjà vérifié** en paramètre, comme `@PathVariable` injecte un morceau du chemin (`Jwt` vient de `org.springframework.security.oauth2.jwt`). Aucune recherche n'est nécessaire : l'id est dans le jeton, et la signature garantit qu'il n'a pas été modifié.

#### Refuser d'agir sur le compte d'un autre : `403`

`PUT /users/{id}` et `DELETE /users/{id}` reçoivent deux identifiants : celui du chemin (le compte visé) et celui du jeton (qui le demande). S'ils diffèrent, la réponse est `403 Forbidden` : « je sais qui vous êtes, mais ce compte n'est pas le vôtre ». Comme les autres règles, c'est une décision métier : passez l'identifiant du jeton au service, qui lève `new ResponseStatusException(HttpStatus.FORBIDDEN, "…")`.

Dans quel ordre vérifier « le compte existe » (`404`) et « c'est le vôtre » (`403`) ? Que révèle à Mallory un `404` sur `DELETE /users/42` ? Choisissez, et justifiez dans un commentaire.

#### Bruno

1. Créez une requête `POST {{baseUrl}}/auth/token` dans un nouveau dossier **Authentification**. Dans son onglet **Script** > **Post Response**, enregistrez le jeton reçu dans une variable :

   ```javascript
   bru.setVar("token", res.body.token);
   ```

2. Dans les réglages de la collection (clic sur la collection > **Settings** > **Auth**), choisissez **Bearer Token** avec la valeur `{{token}}`.
3. Dans chaque requête, onglet **Auth**, choisissez **Inherit** : la requête utilise l'authentification de la collection.
4. Les requêtes de création d'utilisateur envoient maintenant un `username` et un `password` ; **Proposer troc** n'envoie plus de `proposerId`.

Documentation : [Bruno — Authentication](https://docs.usebruno.com/auth/overview) et [Bruno — Script](https://docs.usebruno.com/testing/script/getting-started).

### Indices

- L'ordre `UserDetailsService` → `PasswordEncoder` → `AuthenticationManager` → `JwtEncoder` → `POST /auth/token` : chaque étape se vérifie avant de passer à la suivante. Avant d'écrire `SecurityFilterChain`, tout est encore fermé par défaut : testez l'inscription et la connexion en ajoutant une première version de la chaîne qui ouvre ces deux routes.
- Le service de troc **ne change presque pas** : il reçoit toujours un identifiant de proposeur. Seule la provenance de cet identifiant change, dans le contrôleur. `ProposeTradeRequest` perd son composant `proposerId` : le compilateur vous montre où.
- Le `400` « troc avec soi-même » et le `404` « proposeur inconnu » existent toujours. Le second peut-il encore arriver ? (Pensez à un compte supprimé après la connexion.)
- `401` vient de Spring Security, avant même votre contrôleur. Les `400`, `403`, `404`, `409` viennent de votre service, après le passage de Spring Security : un jeton valide ne dispense d'aucune règle métier.
- Comparez les deux identifiants avec `Objects.equals` (ou `.equals`), jamais avec `==` (TP 2). Le `subject` du jeton est un texte : convertissez-le avec `Long.valueOf(…)`.

### Vérifier

Lancez l'application avec `TROC_JWT_SECRET`. Puis, dans Bruno :

1. Inscrivez Alice, Bob et Mallory : `201`, et aucun haché dans les réponses. Une deuxième inscription de `bob` : `409`.
2. `GET /users` sans jeton (Auth : **No Auth**) : `401`. Lisez l'en-tête `WWW-Authenticate` de la réponse.
3. Connectez-vous en tant qu'Alice, puis `GET /users` : `200`, et toujours aucun haché.
4. Connexion avec un mauvais mot de passe, puis avec un identifiant qui n'existe pas : `401`, avec le même message.
5. Toujours en tant qu'Alice : créez un objet. Connectez-vous en tant que Bob, créez-en un, et proposez un troc à Alice, **sans** `proposerId` : `201`, et le proposeur est bien Bob.
6. Connectez-vous en tant que Mallory et rejouez l'étape 1 de l'expérimentation : ajouter `"proposerId": 2` au corps ne change plus rien, le troc est proposé par Mallory (et refusé si l'objet offert n'est pas à elle). Renommer ou supprimer le compte de Bob (`/users/2`) : `403`. Renommer son propre compte (`/users/3`) : `200`.
7. Copiez le jeton de Bob dans [jwt.io](https://jwt.io/), modifiez un caractère au milieu du contenu, et utilisez ce jeton modifié : `401`.

- [ ] Inscription `201` sans haché dans la réponse, `409` si l'identifiant est pris.
- [ ] Les mots de passe sont conservés hachés.
- [ ] Connexion : un jeton si les identifiants sont bons, `401` sinon, avec le même message.
- [ ] Toutes les routes, sauf l'inscription et la connexion, répondent `401` sans jeton valide.
- [ ] `proposerId` a disparu du corps de `POST /trades` : Mallory ne peut plus proposer au nom de Bob.
- [ ] `PUT` et `DELETE /users/{id}` répondent `403` sur le compte d'un autre, et fonctionnent sur le sien.
- [ ] L'ordre `404` / `403` est justifié dans un commentaire.
- [ ] Aucun JSON de réponse ne contient `passwordHash`.
- [ ] La clé n'est écrite dans aucun fichier versionné.
- [ ] Dans Bruno, toutes les requêtes passent après une seule connexion.

> **Question pour le checkpoint** : Mallory, connectée avec **son** jeton, peut encore créer des objets au nom de Bob (`ownerId` dans le corps de `POST /items`), ou supprimer ceux de Bob. Est-ce un problème d'authentification ou d'autorisation ? Le bonus 2 le corrige.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `401` partout, même sur `GET /users` avec Basic Auth | normal tant que `SecurityFilterChain` n'est pas déclarée : tout est fermé. Une fois déclarée, Basic Auth n'est plus accepté. |
| L'inscription d'un `username` déjà pris répond `401` au lieu de `409`, avec un corps vide | En cas d'erreur, Spring redirige la requête vers la route `/error`, qui construit le JSON d'erreur… et cette route est protégée. Ouvrez `/error`. |
| `401` avec un jeton | jeton expiré, mal copié, ou mot `Bearer` oublié. Lisez l'en-tête `WWW-Authenticate` de la réponse. Après un redémarrage, les comptes ont disparu (données en mémoire) : réinscrivez-vous et reconnectez-vous. |
| `403` sur chaque `POST`, `PUT`, `DELETE` | protection CSRF encore active : `csrf.disable()` oublié. |
| `403` sur `PUT /users/{id}` de **son propre** compte | identifiants comparés avec `==`, ou `subject` (un texte) comparé à un `Long` sans conversion ; ou jeton d'un autre compte (dans Bruno, c'est la dernière connexion qui compte). |
| L'application ne démarre pas : `The secret length must be at least 256 bits` | clé de moins de 32 caractères, ou variable `TROC_JWT_SECRET` absente de la configuration de lancement : Spring garde alors le texte `${TROC_JWT_SECRET}` tel quel, qui est trop court. |
| `There is no PasswordEncoder mapped for the id "null"` | un mot de passe enregistré sans passer par `passwordEncoder.encode(…)` : il n'a ni haché ni préfixe `{bcrypt}`. |
| Le contrôleur reçoit `jwt == null` | la route est ouverte (`permitAll`) et la requête est partie sans jeton. Une route qui a besoin de l'identité doit exiger le jeton. |
| `passwordHash` apparaît dans un JSON | une route renvoie encore l'entité `User` au lieu de l'objet de réponse. |
| Un tutoriel utilise `WebSecurityConfigurerAdapter`, `antMatchers` ou `.and()` | il est écrit pour Spring Security 5. En version 7 : un bean `SecurityFilterChain`, `requestMatchers`, des lambdas. Beaucoup de tutoriels JWT écrivent aussi un filtre à la main avec la bibliothèque `jjwt` : inutile, `oauth2ResourceServer` le fait. |

Pour comprendre un refus : `logging.level.org.springframework.security=DEBUG` affiche chaque filtre traversé et la raison de la décision.

## Bonus

### Bonus 1 — Un administrateur

Un administrateur peut supprimer **n'importe quel** compte, pas seulement le sien. Ajoutez un champ `role` à `User` (`USER` par défaut), utilisez-le dans le `UserDetailsService` (`.roles(…)`), et mettez-le dans le jeton : `.claim("roles", List.of(…))`. Spring ne sait pas où lire les rôles d'un jeton :

```properties
# lire les droits dans le claim "roles", préfixés par ROLE_ ("ADMIN" devient ROLE_ADMIN)
spring.security.oauth2.resourceserver.jwt.authorities-claim-name=roles
spring.security.oauth2.resourceserver.jwt.authority-prefix=ROLE_
```

Le jeton vérifié porte alors le droit `ROLE_ADMIN`. Pour une route **entièrement** réservée aux administrateurs, une règle suffit : `.requestMatchers(HttpMethod.GET, "/admin/**").hasRole("ADMIN")`. Pourquoi ne convient-elle pas pour `DELETE /users/{id}`, que chacun peut appeler sur son propre compte ? La règle devient « c'est son compte, **ou** c'est un administrateur » : le service a besoin de savoir si l'utilisateur est administrateur (`jwt.getClaimAsStringList("roles")`). Le rôle n'est jamais choisi par le client : pourquoi l'objet de requête d'inscription ne doit-il pas avoir de champ `role` ? Pour avoir un administrateur, créez-le au démarrage (un bean `CommandLineRunner`, qui inscrit un compte `admin` si aucun n'existe). Que se passe-t-il pour un jeton délivré **avant** qu'un compte change de rôle ?

Autre syntaxe : `@EnableMethodSecurity` sur la configuration, puis `@PreAuthorize("hasRole('ADMIN')")` sur la méthode du service.

### Bonus 2 — Chacun ses objets

Appliquez aux objets la logique des comptes : le propriétaire d'un objet créé est l'utilisateur du jeton (`ownerId` disparaît du corps), et seul le propriétaire peut modifier ou supprimer son objet. Où placer la vérification : dans le contrôleur, dans le service, dans `SecurityFilterChain` ? Justifiez.

### Bonus 3 — Mes trocs

Remplacez `GET /trades?userId=…` par `GET /trades/mine`, qui renvoie les trocs de l'utilisateur connecté. Pourquoi l'ancienne route posait-elle un problème de confidentialité ?

## Pour aller plus loin

- [Spring Security — Architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html) : la chaîne de filtres
- [Spring Security — OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Security — Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [OWASP — Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [jwt.io — Introduction aux JSON Web Tokens](https://jwt.io/introduction)
- L'annexe du cours : `doc/cours/05-annexe-spring-security.html` (pourquoi un jeton plutôt que Basic ou une session, les pièges courants)
