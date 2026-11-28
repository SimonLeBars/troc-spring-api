# Annexe 1 — Documenter l'API : OpenAPI et Swagger UI

> Bonus du TP 1. Point de départ : la mise en place du TP 1 terminée (`GET` et `POST /users` répondent). Aucune nouvelle route n'est à écrire : on documente l'existant. Une fois en place, la documentation suit l'API toute seule : chaque route ajoutée dans les TP suivants y apparaît sans rien faire.

## Le concept en bref

Votre API sera utilisée par d'autres : le développeur front qui affiche la liste des utilisateurs, le collègue qui reprend le projet dans six mois, le client qui veut l'essayer depuis son navigateur. Ils ont besoin de savoir quelles routes existent, quel JSON envoyer, quels champs et quels codes reviennent.

**OpenAPI** est le format standard pour décrire une API : routes, paramètres, corps JSON, codes de réponse. **Générée à partir du code**, cette description ne se périme jamais. **Swagger UI** l'affiche sous forme de page web, avec un bouton pour essayer chaque route.

---

## Mise en place : springdoc-openapi

### Objectifs

1. La page `http://localhost:8080/swagger-ui.html` liste toutes les routes de l'API, groupées par contrôleur.
2. Depuis Swagger UI, vous avez créé un utilisateur, puis l'avez retrouvé avec `GET /users`.
3. Le bouton **Try it out** est actif par défaut, et les opérations sont triées par méthode HTTP.
4. Vous savez où trouver la description OpenAPI brute (`/v3/api-docs`) et à quoi elle sert.

### La dépendance

[springdoc-openapi](https://springdoc.org/) lit vos contrôleurs au démarrage et en déduit la description OpenAPI. Elle embarque Swagger UI. Ajoutez-la dans le `pom.xml`, à côté de `spring-boot-starter-webmvc`, puis rechargez Maven (icône **M** en haut à droite de l'éditeur dans IntelliJ) :

```xml
<!-- Documentation de l'API et Swagger UI (http://localhost:8080/swagger-ui.html) -->
<!-- Pas gérée par Spring Boot : la version 3.2.x correspond à Spring Boot 4.1 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.2.0</version>
</dependency>
```

Remarquez la balise `<version>`, absente de `spring-boot-starter-webmvc`. Les dépendances `spring-boot-starter-*` viennent de Spring Boot, qui choisit des versions compatibles entre elles (le rôle du parent `spring-boot-starter-parent`). springdoc est un projet indépendant : c'est à nous de choisir la version qui correspond à notre Spring Boot, d'après le tableau de compatibilité de la FAQ du [site de springdoc](https://springdoc.org/).

> **Piège** : une version de springdoc prévue pour une autre version de Spring Boot compile souvent sans erreur, puis plante au démarrage avec une `NoSuchMethodError` ou une `ClassNotFoundException`. Vérifiez d'abord la compatibilité des versions.

### Syntaxes utiles

#### Régler Swagger UI

springdoc se règle dans `src/main/resources/application.properties`, le fichier de configuration créé avec le projet (il sera détaillé au TP 3), avec des clés `springdoc.*`. Les deux réglages des objectifs :

```properties
springdoc.swagger-ui.try-it-out-enabled=true
springdoc.swagger-ui.operations-sorter=method
```

Les autres réglages sont listés dans la [documentation de springdoc](https://springdoc.org/#properties).

#### Ce que springdoc lit dans votre code

Sans rien écrire, springdoc déduit la documentation des annotations de vos contrôleurs (`@RestController`, `@GetMapping`, `@PostMapping`, `@RequestBody`…), des types des paramètres et des valeurs de retour. L'exemple de JSON de `POST /users` vient de la classe `User` : ses getters et setters déterminent les champs affichés.

### Vérifier

Relancez l'application et ouvrez <http://localhost:8080/swagger-ui.html>. Dépliez `POST /users` : le corps attendu et la réponse sont décrits. Puis, entièrement depuis Swagger UI :

1. `POST /users` avec `{ "name": "Alice" }`, puis `{ "name": "Bob" }`. **Execute** affiche la réponse, le code HTTP et la commande `curl` équivalente.
2. `GET /users` : Alice et Bob apparaissent.

Ouvrez ensuite <http://localhost:8080/v3/api-docs> : la même information, en JSON. Cherchez-y `/users` et le schéma `User`. Swagger UI n'est qu'un afficheur de ce fichier ; d'autres outils le lisent :

- **générer un client** : une équipe Angular ou mobile génère le code qui appelle votre API, avec les bons types ([OpenAPI Generator](https://openapi-generator.tech/)) ;
- **importer dans un client HTTP** : Bruno et Postman créent une collection à partir de cette URL ;
- **comparer deux versions** de l'API pour repérer un changement qui casserait les applications existantes.

Swagger UI ne remplace pas Bruno : Bruno garde vos requêtes et vos environnements dans le projet, versionnés avec le code. Swagger UI sert à **découvrir** une API et à l'essayer sans rien installer.

#### Liste de vérification

- [ ] `swagger-ui.html` affiche `GET` et `POST /users`, groupées sous le contrôleur des utilisateurs.
- [ ] **Try it out** est actif sans clic, et les opérations sont triées par méthode.
- [ ] Un utilisateur créé depuis Swagger UI apparaît dans `GET /users`.
- [ ] Vous savez à quoi sert `/v3/api-docs`, en dehors de Swagger UI.

## Bonus — Enrichir la documentation

springdoc déduit beaucoup de choses, mais pas l'intention. Des annotations complètent la description d'une route :

```java
import io.swagger.v3.oas.annotations.Operation;

@Operation(summary = "Un résumé court",
           description = "Les règles qui ne se devinent pas à la lecture de la signature.")
```

Essayez-en un sur `POST /users`, et regardez le résultat. N'en mettez pas partout : une annotation qui répète le nom de la méthode n'apporte rien. Réservez-les aux routes dont les règles ne se devinent pas ; il y en aura davantage dans les TP suivants (qui peut accepter un troc ? que se passe-t-il pour les stocks ?).

Pour aller plus loin : `@ApiResponse` documente chaque code de réponse possible, avec sa signification.

## Et pendant la suite du cours

Rien à refaire : la documentation se met à jour à chaque démarrage. Quelques rendez-vous à garder en tête :

- **TP 2 et suivants** : chaque nouvelle route apparaît dans Swagger UI. Revenez-y de temps en temps pour vérifier que les codes de réponse affichés sont ceux que vous attendez.
- **TP 4** : les objets de requête (DTO) documentent exactement ce que le client doit envoyer ; comparez le schéma affiché avant et après.
- **TP 5 (Spring Security)** : Swagger UI et la description OpenAPI deviennent des routes comme les autres, donc fermées par défaut. Ouvrez-les dans votre `SecurityFilterChain` : `/swagger-ui.html`, `/swagger-ui/**` et `/v3/api-docs/**`. Pour essayer les routes protégées depuis Swagger UI, déclarez à springdoc que l'API attend un jeton Bearer, avec une annotation sur une classe `@Configuration` :

  ```java
  import io.swagger.v3.oas.annotations.OpenAPIDefinition;
  import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
  import io.swagger.v3.oas.annotations.security.SecurityRequirement;
  import io.swagger.v3.oas.annotations.security.SecurityScheme;

  @OpenAPIDefinition(security = @SecurityRequirement(name = "bearer"))
  @SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
  ```

  Un bouton **Authorize** apparaît en haut de Swagger UI : collez-y un jeton obtenu avec `POST /auth/token`, et toutes les requêtes suivantes l'envoient. Documentation : [springdoc — Security](https://springdoc.org/#how-do-i-add-authorization-header-in-requests).

## Pour aller plus loin

- [springdoc-openapi — Documentation](https://springdoc.org/)
- [Spécification OpenAPI](https://spec.openapis.org/oas/latest.html) et l'[introduction de la OpenAPI Initiative](https://learn.openapis.org/)
- [Swagger UI — Configuration](https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/)
