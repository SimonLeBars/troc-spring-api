# 🔒 Fiche 5 — Sécurité

> `TP N` = notion vue à partir du TP N · ✅ à faire · ❌ à éviter · ⚠️ piège · 💡 astuce

⚠️ noms de classes tirés du TP 5, *à adapter* à votre projet.

## ❓ Deux questions, dans cet ordre · `TP 5`

| | Question | Si non | Qui répond |
|:-:|---|---|---|
| 1️⃣ | 👤❓ **Authentification** : qui es-tu ? | `401 Unauthorized` (= non authentifié) | Spring Security, **avant** le contrôleur |
| 2️⃣ | ⛔ **Autorisation** : as-tu le droit ? | `403 Forbidden` | Spring Security (rôles, bonus) ou **votre service** (son propre compte, TP 5 · destinataire, TP 7) |

❌ le client ne **déclare** pas son identité (`"proposerId": 1` dans le corps) · ✅ il la **prouve** (jeton signé)

## 🎟️ Le parcours d'un jeton · `TP 5`

```mermaid
sequenceDiagram
    participant C as 🙋 Client
    participant S as 🔒 Spring Security
    participant A as POST /auth/token
    participant T as TradeController
    C->>A: {"username": "bob", "password": "…"}
    A->>A: AuthenticationManager vérifie le haché
    A-->>C: 200 {"token": "eyJ…", "expiresIn": 3600}
    C->>S: POST /trades/1/accept (TP 7)<br/>Authorization: Bearer eyJ…
    S->>S: JwtDecoder vérifie signature + expiration
    alt jeton absent, modifié, expiré
        S-->>C: 401
    else jeton valide
        S->>T: @AuthenticationPrincipal Jwt (sub = "2")
        T-->>C: 200, ou 403 si pas le destinataire
    end
```

## 📦 Dépendances · `TP 5`

| Starter | Rôle |
|---|---|
| `spring-boot-starter-security` | filtres, hachage, tout est **fermé** par défaut |
| `spring-boot-starter-security-oauth2-resource-server` | lire et vérifier `Authorization: Bearer …` |
| `spring-boot-starter-security-test` (`test`) | `jwt()` dans MockMvc · `TP 9` |

❌ `spring-boot-starter-oauth2-resource-server` : nom Boot 3, introuvable en Boot 4

**Sans configuration :** tout en `401` · compte `user` + `Using generated security password: …` · `POST` refusés (CSRF)

## 🔑 Mots de passe · `TP 5`

```java
@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();   // BCrypt, préfixe {bcrypt}
}
```

| Appel | Rôle |
|---|---|
| `encoder.encode("tomate42")` | haché à enregistrer : `{bcrypt}$2a$10$…` (différent à chaque fois : **sel**) |
| `encoder.matches("tomate42", hache)` | vérifier (fait par l'`AuthenticationManager`) |

- ❌ jamais le mot de passe enregistré, même chiffré · ✅ son **haché**, lent exprès
- ❌ jamais le haché dans une réponse JSON → **DTO de réponse** (fiche 4)
- ⚠️ `PUT /users/{id}` qui enregistre l'entité reçue → le haché est **effacé**

| Champ de `User` | Colonne `users` · `TP 6` |
|---|---|
| `username` (unique, `findByUsername` → `Optional<User>`) | `VARCHAR(50) NOT NULL UNIQUE` |
| `passwordHash` | `password_hash VARCHAR(100) NOT NULL` |
| `role` (bonus) | `VARCHAR(20) NOT NULL DEFAULT 'USER'` · ❌ jamais choisi par le client |

## 🧱 Les beans à fournir · `TP 5`

| Bean | Rôle | Appelé |
|---|---|---|
| `UserDetailsService` | trouver un compte par `username` (`UsernameNotFoundException` sinon) | à la **connexion** seulement |
| `PasswordEncoder` | hacher, vérifier | inscription, connexion |
| `AuthenticationManager` | `UserDetailsService` + `PasswordEncoder` | `POST /auth/token` |
| `SecretKey` | clé HMAC lue dans `TROC_JWT_SECRET` (≥ 32 caractères) | |
| `JwtEncoder` | **signer** un jeton | `POST /auth/token` |
| `JwtDecoder` | **vérifier** un jeton | chaque requête |
| `SecurityFilterChain` | les règles d'accès | chaque requête |

```java
// UserDetails : le User de Spring Security, à ne pas confondre avec votre entité
org.springframework.security.core.userdetails.User
        .withUsername(user.getUsername()).password(user.getPasswordHash()).roles("USER").build();  // bonus : user.getRole()
```

```java
var provider = new DaoAuthenticationProvider(userDetailsService);
provider.setPasswordEncoder(encoder);
return new ProviderManager(provider);                                      // l'AuthenticationManager
```

```java
new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");  // javax.crypto.spec
NimbusJwtEncoder.withSecretKey(key).build();
NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
```

## 🎫 Le JWT · `TP 5`

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiIxIiwiZXhwIjoxNzkyNjY2ODAwfQ . DBlqUDT6esOk…
──────────────────     ──────────────────────────────────────     ────────────
 en-tête : HS256        contenu (claims) : sub, exp, roles…        signature
```

| Claim | Sens | Méthode du builder |
|---|---|---|
| `iss` | émetteur (`troc-api`) | `.issuer(…)` |
| `sub` | **qui** : id de l'utilisateur, en `String` | `.subject(…)` |
| `iat` · `exp` | émis à · expire à (secondes depuis 1970) | `.issuedAt(…)` · `.expiresAt(…)` |
| libres | `username`, `roles` (bonus) | `.claim("username", …)` |

- 🔓 **encodé, pas chiffré** : lisible par tous (jwt.io) → ❌ jamais de secret dedans
- 🔏 **signé** : modifié sans la clé → signature fausse → `401`
- ⚠️ `JwsHeader.with(MacAlgorithm.HS256)` obligatoire, sinon l'encodeur attend une clé RSA

```java
authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
// AuthenticationException → 401, même message que le compte existe ou non
```

## 🚧 Les règles d'accès · `TP 5`

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/chemin", "/autre").permitAll()
            .requestMatchers("/un-dossier/**").permitAll()
            .anyRequest().authenticated())                               // ✅ toujours en dernier
        .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))  // Bearer exigé
        .csrf(csrf -> csrf.disable())                                    // pas de cookie → pas de CSRF
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(h -> h.frameOptions(f -> f.sameOrigin()));              // console H2 · TP 6
    return http.build();
}
```

- les règles se lisent de haut en bas : la **première** qui correspond gagne
- 🔍 à ouvrir aussi : `/error` · `/h2-console/**` (`TP 6`) · Swagger UI (`/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`)
- 👑 bonus : `.requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")`, ou `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` (un proxy, comme `@Transactional`)

## 🪪 Lire l'identité · `TP 5`

```java
@PostMapping
public ResponseEntity<Trade> propose(@RequestBody ProposeTradeRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {          // oauth2.jwt.Jwt
    Long proposerId = Long.valueOf(jwt.getSubject());                             // déjà vérifié
    …
}
```

✅ le service ne change presque pas : seule la **provenance** de l'id change · `proposerId` disparaît du corps (TP 5) · `accept` / `reject` n'ont pas de corps (TP 7)

## 🐶 Bruno · `TP 5`

1. Requête `POST {{baseUrl}}/auth/token` dans un dossier **Authentification**, onglet **Script > Post Response** : `bru.setVar("token", res.body.token);`
2. Collection > **Settings > Auth** : **Bearer Token** `{{token}}`
3. Chaque requête, onglet **Auth** : **Inherit**

## ⌨️ IntelliJ · `TP 5`

⭐ = aide Spring, IntelliJ **Ultimate** (licence étudiante gratuite) · raccourcis Windows / Linux ; macOS et liste générale : [fiche 10](10-raccourcis-intellij.md)

| Raccourci | Effet |
|---|---|
| `Ctrl+N` **deux fois** + `BearerTokenAuthenticationFilter` | ouvrir une classe de Spring Security (hors projet) · **Download Sources** si proposé |
| point d'arrêt dans ce filtre + **Debug** | voir pourquoi un jeton est refusé (`401`) |
| `Ctrl+Q` sur `requestMatchers` / `permitAll` | la documentation de la règle |
| `Ctrl+Alt+V` sur `jwt.getClaim("…")` | nommer la valeur lue dans le jeton avant de l'utiliser |

## 🚨 Erreurs fréquentes

| Symptôme | 🔎 Cause | TP |
|---|---|:-:|
| `401` partout | pas encore de `SecurityFilterChain` · route non ouverte | 5 |
| `409` attendu, `401` reçu avec un corps vide | `/error` est protégé : l'ouvrir | 5 |
| `401` avec un jeton | expiré, mal copié, `Bearer ` oublié → lire l'en-tête `WWW-Authenticate` | 5 |
| `403` sur chaque `POST`, `PUT`, `DELETE` | CSRF encore actif | 5 |
| `The secret length must be at least 256 bits` | clé < 32 caractères · `TROC_JWT_SECRET` absente | 5 |
| `There is no PasswordEncoder mapped for the id "null"` | mot de passe enregistré sans `encode(…)` · inséré à la main, sans `{bcrypt}` | 5, 6 |
| `jwt == null` | route ouverte, requête sans jeton · dans un test : `@WithMockUser` au lieu de `.with(jwt())` | 5, 9 |
| console H2 : `401` ou page blanche | `/h2-console/**` non ouvert · `frameOptions(… sameOrigin())` oublié | 6 |
| `passwordHash` dans un JSON | l'entité est renvoyée au lieu d'un DTO de réponse | 5 |
| tutoriel avec `WebSecurityConfigurerAdapter`, `antMatchers`, `.and()`, `jjwt` | Spring Security 5 : en version 7, bean `SecurityFilterChain`, `requestMatchers`, lambdas, `oauth2ResourceServer` | 5 |

💡 `logging.level.org.springframework.security=DEBUG` : chaque filtre traversé et la raison du refus.

## 🔗 Pour aller plus loin

[Spring Security — Architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html) · [Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) · [Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html) · [OWASP — Passwords](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) · [jwt.io](https://jwt.io/introduction) · `doc/cours/05-annexe-spring-security.html`
