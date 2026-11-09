# TP 9 — Sécuriser l'API : authentification et autorisation

> Rythme indicatif : séance 5 (autonomie). Point de départ : la mise en place du TP 8 terminée. Ce TP fait volontairement passer les tests du TP 8 au rouge, puis vous les réparez. Comptez environ 2 h 30 pour la mise en place.
>
> Support complémentaire : l'annexe du cours `doc/cours/05-annexe-spring-security.html`, qui explique le mécanisme avec des schémas.

## Le concept en bref

Depuis le TP 4, le client **se déclare** lui-même : `"proposerId": 1` pour proposer un troc, `"userId": 2` pour l'accepter. Le serveur le croit sur parole. N'importe qui peut donc se faire passer pour n'importe qui.

Sécuriser une API, c'est répondre à deux questions, **toujours dans cet ordre** :

| Question | Nom | Si la réponse est non |
|---|---|---|
| Qui es-tu ? | **authentification** : on le prouve (mot de passe, puis jeton) | `401 Unauthorized` (le nom est trompeur : il signifie « non authentifié ») |
| As-tu le droit ? | **autorisation** : décidée une fois l'identité connue | `403 Forbidden` |

Le mécanisme de ce TP est celui de la plupart des API actuelles :

1. Le client envoie son identifiant et son mot de passe **une seule fois**, à `POST /auth/token`.
2. Le serveur vérifie le mot de passe, puis renvoie un **jeton** (JWT) : un petit texte qui dit « c'est l'utilisateur 1, jusqu'à 15 h », **signé** avec une clé que seul le serveur connaît.
3. Le client joint ce jeton à chaque requête suivante, dans l'en-tête `Authorization: Bearer <jeton>`. Le serveur vérifie la signature : si quelqu'un a modifié le jeton, la signature ne correspond plus, et la réponse est `401`.

[Spring Security](https://docs.spring.io/spring-security/reference/) fait tout le travail de vérification. Vous lui fournissez des beans (où trouver les comptes, comment vérifier un mot de passe, quelle clé utiliser, quelles routes protéger), et il les branche au bon endroit : c'est encore l'inversion de contrôle du TP 1.

---

## Partie 1 — Expérimentation : se faire passer pour quelqu'un d'autre (facultative)

**Vous pouvez passer à la partie 2 si vous savez répondre à ces questions :**

- Quelle est la différence entre un `401` et un `403` ?
- Pourquoi ne stocke-t-on jamais un mot de passe en base, même chiffré ? Que stocke-t-on à la place ?
- Un JWT est-il chiffré ? Qu'est-ce qui empêche un client de modifier son contenu ?
- Que se passe-t-il quand on ajoute `spring-boot-starter-security` à un projet, sans rien configurer ?

### Étape 1 — Jouez Mallory

Mallory est le prénom traditionnel de l'attaquant dans les exemples de sécurité. Avec la collection Bruno du projet :

1. Créez Alice, Bob et un troisième utilisateur, Mallory (dossier **Utilisateurs**).
2. Créez un objet pour Alice et un pour Bob, puis faites proposer un troc d'Alice à Bob (dossiers **Objets** et **Trocs**).
3. Vous êtes maintenant Mallory. Votre `id` est 3, mais rien ne vous empêche d'écrire autre chose. Acceptez le troc **à la place de Bob**, avec `"userId": 2`.

Puis répondez :

- Quel code reçoit Mallory ? Les stocks de Bob ont-ils changé ?
- Le `403` « seul le destinataire peut répondre » du TP 6 existe pourtant. Pourquoi ne protège-t-il rien ?
- Avec la même méthode, Mallory peut-elle proposer un troc au nom d'Alice ?

La règle métier est bonne : c'est l'information d'entrée, l'identité, qui n'est pas fiable.

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
| `./mvnw test` | ? |

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
2. Combien de temps prend un hachage ? Pourquoi est-ce une bonne chose ? Pensez à un attaquant qui a volé la base et teste des milliards de mots de passe.
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

- **Le client ne doit jamais déclarer son identité** dans le corps de la requête : il doit la **prouver**. Dans la vraie vie, une API qui croit le client sur parole laisse n'importe qui valider la commande d'un autre ou lire un dossier qui n'est pas le sien.
- **Un mot de passe ne se stocke pas, seul son haché se stocke.** Les bases finissent par fuiter (sauvegarde perdue, injection SQL), et les gens réutilisent leurs mots de passe partout. BCrypt est lent exprès et salé : même haché, la base ne révèle pas les mots de passe.
- **Un JWT est encodé, pas chiffré** : tout le monde peut le lire, donc jamais de secret dedans. Mais personne ne peut le modifier sans la clé : sa signature garantit son contenu.
- **Sécurisé par défaut** : ajouter Spring Security ferme tout. La configuration consiste à dire ce qu'on ouvre, et comment on s'authentifie.

Supprimez le package `sandbox`, ainsi que la ligne `logging.level.org.springframework.security=DEBUG` si vous ne voulez pas la garder. Vous pouvez garder la dépendance : la mise en place en a besoin.

---

## Partie 2 — Mise en place : des comptes, des jetons, des règles (obligatoire)

### Objectifs

| Route | Accès | Comportement attendu |
|---|---|---|
| `POST /users` | ouvert | **inscription** : `{ "name", "username", "password" }` → `201`. La réponse ne contient **jamais** le mot de passe ni son haché. `username` déjà pris → `409`. |
| `POST /auth/token` | ouvert | **connexion** : `{ "username", "password" }` → `200` et `{ "token": "eyJ…", "expiresIn": 3600 }`. Identifiants faux → `401`, avec le **même message** que le compte existe ou non. |
| toutes les autres | jeton exigé | sans jeton, avec un jeton modifié ou expiré → `401`. |
| `POST /trades` | jeton exigé | le proposeur est l'utilisateur **du jeton** : `proposerId` disparaît du corps. |
| `POST /trades/{id}/accept`, `/reject` | jeton exigé | l'utilisateur est celui du jeton : plus de corps. Un utilisateur qui n'est pas le destinataire → `403`, et il ne peut plus mentir. |

Et aussi :

1. Le mot de passe est stocké **haché** (BCrypt) : vérifiez-le dans la console H2.
2. **Aucune** réponse JSON ne contient de haché : ni `GET /users`, ni `GET /users/{id}`, ni `PUT /users/{id}`.
3. La clé qui signe les jetons **n'est pas dans git** : elle vient de la variable d'environnement `TROC_JWT_SECRET`.
4. `./mvnw test` est au vert : les tests du TP 8 sont adaptés, et deux nouveaux tests d'intégration vérifient le `401` sans jeton et le `403` d'un utilisateur qui n'est pas le destinataire.
5. Dans Bruno, une requête de connexion enregistre le jeton, et toutes les autres requêtes l'envoient automatiquement.

> Ordre conseillé : dépendances, puis comptes et inscription, puis jetons et connexion, puis règles d'accès et contrôleurs, puis tests et Bruno. Organisation possible pour un groupe de 4 : schéma, entité et inscription (1) ; clé, jetons et `POST /auth/token` (2) ; règles d'accès et contrôleurs (3) ; tests et Bruno (4).

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
<!-- Simuler un utilisateur connecté dans les tests -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Avec Spring Boot 4, les starters liés à la sécurité commencent par `spring-boot-starter-security-`. Les tutoriels pour Boot 3 écrivent `spring-boot-starter-oauth2-resource-server` : ce nom n'existe plus.

> Le vocabulaire **OAuth2** vient de la norme qui a popularisé ces jetons. On n'en utilise qu'une petite partie : un *resource server* est une API qui accepte des jetons.

La sécurité est une fonctionnalité transverse : elle n'appartient ni à `user`, ni à `item`, ni à `trade`. Rangez ses classes dans un package `fr.cours.troc.security`.

### Syntaxes utiles

#### Les comptes

Un compte, c'est trois colonnes de plus dans `users` :

| Colonne | Rôle | Contrainte |
|---|---|---|
| `username` | l'identifiant de connexion (`alice`) ; `name` reste le nom affiché (`Alice Martin`) | `VARCHAR(50)`, obligatoire, **unique** |
| `password_hash` | le haché BCrypt, jamais le mot de passe | `VARCHAR(100)`, obligatoire |
| `role` | `USER` ou `ADMIN` | `VARCHAR(20)`, obligatoire, `'USER'` par défaut |

Ajoutez-les au `schema.sql` et à l'entité `User` (`passwordHash` en Java, `password_hash` en base, comme au TP 5). Comme au TP 5, `CREATE TABLE IF NOT EXISTS` ne modifie pas une table existante : supprimez le dossier `data/` avant de relancer.

Il faudra retrouver un compte par son identifiant : c'est une requête dérivée (TP 5), qui renvoie un `Optional<User>`.

#### Où Spring trouve les comptes : `UserDetailsService`

Spring Security ne connaît pas votre table. Il appelle un bean `UserDetailsService`, que vous fournissez :

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
        .password(...)   // le haché lu en base
        .roles(...)      // "USER" devient le droit ROLE_USER
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

`POST /users` reçoit un **objet de requête** (TP 4) avec `name`, `username` et `password`, et renvoie un **objet de réponse** sans haché. Deux questions à vous poser avant d'écrire le code :

- Le client choisit son mot de passe. Doit-il pouvoir choisir son rôle ? Que se passerait-il si l'objet de requête avait un champ `role` ?
- Aujourd'hui, `GET /users` et `PUT /users/{id}` renvoient l'entité `User`. Que contiendra leur JSON une fois `passwordHash` ajouté à l'entité ? Et que devient le haché d'un utilisateur si `PUT /users/{id}` enregistre l'entité reçue dans le corps, qui n'en contient pas ?

#### La clé et les jetons

La clé de signature est un **secret** : qui la connaît peut fabriquer un jeton au nom de n'importe qui. Elle vient donc de l'environnement, jamais d'un fichier versionné :

```properties
# application.properties
troc.jwt.secret=${TROC_JWT_SECRET}
troc.jwt.ttl=1h
```

`${TROC_JWT_SECRET}` lit une variable d'environnement. Sans elle, l'application ne démarre pas : c'est voulu. Dans IntelliJ, renseignez-la dans **Run > Edit Configurations…** > **Environment variables**, avec une valeur d'au moins 32 caractères : HS256 exige une clé de 256 bits. Pour les tests, mettez une valeur de test dans `application-test.properties` (TP 8). Si vous avez fait le bonus « profils » du TP 3, un `application-dev.properties` non versionné convient aussi.

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
        .claim("roles", List.of(...))
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
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(h -> h.frameOptions(f -> f.sameOrigin()));                   // pour la console H2
    return http.build();
}
```

- Les règles se lisent de haut en bas : la **première** qui correspond s'applique.
- `oauth2ResourceServer(… jwt …)` installe le filtre qui lit `Authorization: Bearer …` et vérifie le jeton avec votre `JwtDecoder`.
- `csrf.disable()` : la protection CSRF protège les cookies, que le navigateur envoie tout seul. Un jeton n'est jamais envoyé tout seul, donc la protection n'a pas d'objet ici. `STATELESS` : Spring ne crée pas de session, le jeton suffit.
- Dès que ce bean existe, le compte `user` et son mot de passe généré disparaissent, ainsi que le formulaire de connexion et HTTP Basic.

À vous de décider quelles routes ouvrir : relisez le tableau des objectifs. Pensez à la console H2 (`/h2-console/**`), et à Swagger UI si vous avez fait le TP 10.

#### Lire l'identité dans un contrôleur

```java
@PostMapping("/{id}/accept")
public Trade accept(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    Long userId = Long.valueOf(jwt.getSubject());
    ...
}
```

`@AuthenticationPrincipal` injecte le jeton **déjà vérifié** en paramètre, comme `@PathVariable` injecte un morceau du chemin (`Jwt` vient de `org.springframework.security.oauth2.jwt`). Aucune requête en base n'est nécessaire : l'id est dans le jeton, et la signature garantit qu'il n'a pas été modifié.

#### Les tests

Les tests MockMvc du TP 8 envoient leurs requêtes sans jeton. `jwt()` simule un utilisateur connecté, sans signature à calculer :

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

mvc.perform(get("/users").with(jwt()))                              // un utilisateur connecté
mvc.perform(post("/trades/1/accept").with(jwt().jwt(j -> j.subject("3")))) // connecté en tant qu'utilisateur 3
```

N'utilisez pas `@WithMockUser`, fréquent dans les tutoriels : il ne fournit pas de `Jwt`, et `@AuthenticationPrincipal Jwt` recevrait `null`.

#### Bruno

1. Créez une requête `POST {{baseUrl}}/auth/token` dans un nouveau dossier **Authentification**. Dans son onglet **Script** > **Post Response**, enregistrez le jeton reçu dans une variable :

   ```javascript
   bru.setVar("token", res.body.token);
   ```

2. Dans les réglages de la collection (clic sur la collection > **Settings** > **Auth**), choisissez **Bearer Token** avec la valeur `{{token}}`.
3. Dans chaque requête, onglet **Auth**, choisissez **Inherit** : la requête utilise l'authentification de la collection.

Documentation : [Bruno — Authentication](https://docs.usebruno.com/auth/overview) et [Bruno — Script](https://docs.usebruno.com/testing/script/getting-started).

### Indices

- L'ordre `UserDetailsService` → `PasswordEncoder` → `AuthenticationManager` → `JwtEncoder` → `POST /auth/token` : chaque étape se vérifie avant de passer à la suivante. Avant d'écrire `SecurityConfig`, tout est encore fermé par défaut : testez l'inscription et la connexion en ajoutant une première version de la chaîne qui ouvre ces deux routes.
- Le service de troc **ne change presque pas** : il reçoit toujours un identifiant d'utilisateur. Seule la provenance de cet identifiant change, dans le contrôleur. Et `TradeServiceTest` ? Relancez-le pour voir.
- `UserServiceTest` ne compile plus si `UserService` reçoit un `PasswordEncoder` dans son constructeur : c'est un test unitaire, donnez-lui un mock.
- Le `403` « pas le destinataire » vient de votre service, après le passage de Spring Security. Le `401` vient de Spring Security, avant même votre contrôleur. Les deux sont des réponses correctes, à deux étapes différentes.

### Vérifier

Lancez l'application avec `TROC_JWT_SECRET`, après avoir supprimé `data/`. Puis, dans Bruno :

1. Inscrivez Alice, Bob et Mallory : `201`, et aucun haché dans les réponses. Une deuxième inscription de `bob` : `409`.
2. Dans la console H2 : `SELECT username, password_hash FROM users`. Vous devez voir des `{bcrypt}$2a$10$…`.
3. `GET /users` sans jeton (Auth : **No Auth**) : `401`. Lisez l'en-tête `WWW-Authenticate` de la réponse.
4. Connectez-vous en tant qu'Alice, puis `GET /users` : `200`.
5. Connexion avec un mauvais mot de passe, puis avec un identifiant qui n'existe pas : `401`, avec le même message.
6. Toujours en tant qu'Alice : créez un objet, puis proposez un troc à Bob, **sans** `proposerId`. Faites de même pour Bob.
7. Connectez-vous en tant que Mallory et acceptez le troc : `403`. Connectez-vous en tant que Bob et acceptez-le : `200`, `EXECUTED`.
8. Copiez le jeton de Bob dans [jwt.io](https://jwt.io/), modifiez un caractère au milieu du contenu, et utilisez ce jeton modifié : `401`.

- [ ] Inscription `201` sans haché dans la réponse, `409` si l'identifiant est pris.
- [ ] Les mots de passe sont hachés en base.
- [ ] Connexion : un jeton si les identifiants sont bons, `401` sinon.
- [ ] Toutes les routes, sauf l'inscription et la connexion, répondent `401` sans jeton valide.
- [ ] `proposerId` et `userId` ont disparu des corps de requête ; Mallory reçoit `403`.
- [ ] Aucun JSON de réponse ne contient `passwordHash`.
- [ ] La clé n'est écrite dans aucun fichier versionné, sauf la valeur de test.
- [ ] `./mvnw test` est au vert, avec un test `401` et un test `403`.
- [ ] Dans Bruno, toutes les requêtes passent après une seule connexion.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `401` partout, même sur `GET /users` avec Basic Auth | normal tant que `SecurityFilterChain` n'est pas déclarée : tout est fermé. Une fois déclarée, Basic Auth n'est plus accepté. |
| L'inscription d'un `username` déjà pris répond `401` au lieu de `409`, avec un corps vide | En cas d'erreur, Spring redirige la requête vers la route `/error`, qui construit le JSON d'erreur… et cette route est protégée. Ouvrez `/error`. |
| `401` avec un jeton | jeton expiré, mal copié, ou mot `Bearer` oublié. Lisez l'en-tête `WWW-Authenticate` de la réponse. |
| `403` sur chaque `POST`, `PUT`, `DELETE` | protection CSRF encore active : `csrf.disable()` oublié. |
| L'application ne démarre pas : `The secret length must be at least 256 bits` | clé de moins de 32 caractères, ou variable `TROC_JWT_SECRET` absente de la configuration de lancement : Spring garde alors le texte `${TROC_JWT_SECRET}` tel quel, qui est trop court. |
| `There is no PasswordEncoder mapped for the id "null"` | un mot de passe inséré en base à la main, sans haché ni préfixe `{bcrypt}`. |
| Le contrôleur reçoit `jwt == null` | dans un test : `@WithMockUser` au lieu de `.with(jwt())`. |
| Console H2 blanche | `frameOptions(… sameOrigin())` oublié. |
| Un tutoriel utilise `WebSecurityConfigurerAdapter`, `antMatchers` ou `.and()` | il est écrit pour Spring Security 5. En version 7 : un bean `SecurityFilterChain`, `requestMatchers`, des lambdas. Beaucoup de tutoriels JWT écrivent aussi un filtre à la main avec la bibliothèque `jjwt` : inutile, `oauth2ResourceServer` le fait. |

Pour comprendre un refus : `logging.level.org.springframework.security=DEBUG` affiche chaque filtre traversé et la raison de la décision.

## Bonus

### Bonus 1 — Un administrateur

Seul un administrateur peut supprimer un utilisateur. Les rôles sont dans le jeton (`"roles": ["USER"]`), mais Spring ne sait pas où les lire :

```properties
# lire les droits dans le claim "roles", préfixés par ROLE_ ("ADMIN" devient ROLE_ADMIN)
spring.security.oauth2.resourceserver.jwt.authorities-claim-name=roles
spring.security.oauth2.resourceserver.jwt.authority-prefix=ROLE_
```

Puis, dans les règles : `.requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")`. Le rôle n'est jamais choisi par le client : passez un compte en `ADMIN` avec un `UPDATE` dans la console H2, puis reconnectez-vous. Pourquoi faut-il se reconnecter ?

Autre syntaxe : `@EnableMethodSecurity` sur la configuration, puis `@PreAuthorize("hasRole('ADMIN')")` sur la méthode du service. Elle repose sur un proxy, comme `@Transactional` (TP 6) et vos aspects (TP 7) : quelle limite en découle ?

### Bonus 2 — Chacun ses objets

Appliquez la même logique aux objets : le propriétaire d'un objet créé est l'utilisateur du jeton (`ownerId` disparaît du corps), et seul le propriétaire peut modifier ou supprimer son objet (`403` sinon). Même question pour `PUT /users/{id}` et `DELETE /users/{id}` : qui doit pouvoir les appeler ?

### Bonus 3 — Mes trocs

Remplacez `GET /trades?userId=…` par `GET /trades/mine`, qui renvoie les trocs de l'utilisateur connecté. Pourquoi l'ancienne route posait-elle un problème de confidentialité ?

### Bonus 4 — Un test de bout en bout

`jwt()` contourne la fabrication et la vérification du jeton. Écrivez un test d'intégration qui traverse tout : inscription, `POST /auth/token`, lecture du jeton dans la réponse (`JsonPath.read`, bonus 2 du TP 8), puis `GET /users` avec l'en-tête `Authorization: Bearer …`. Ajoutez le cas d'un jeton modifié.

## Pour aller plus loin

- [Spring Security — Architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html) : la chaîne de filtres
- [Spring Security — OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Security — Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [Spring Security — Testing with JWT](https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html)
- [OWASP — Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [jwt.io — Introduction aux JSON Web Tokens](https://jwt.io/introduction)
- L'annexe du cours : `doc/cours/05-annexe-spring-security.html` (pourquoi un jeton plutôt que Basic ou une session, les pièges courants)
