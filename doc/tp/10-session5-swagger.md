# Session 5 — Documenter l'API avec OpenAPI et Swagger UI

## Objectif

Depuis la séance 2, vous testez l'API avec Bruno. C'est très pratique pour **vous**, qui avez le projet et la collection sous la main. Mais imaginez les autres personnes qui doivent utiliser votre API :

- le développeur front qui doit afficher la liste des objets : quelles routes existent ? Quel JSON envoyer pour créer un objet ? Quels champs reviennent ?
- le collègue qui reprend le projet dans six mois, sans vous pour lui expliquer ;
- le client qui veut essayer l'API depuis son navigateur, ou depuis son téléphone, sans rien installer.

Dans la vraie vie, une API sans documentation à jour devient vite un problème :

- **Les questions arrivent sur Slack** plutôt que dans une doc : « c'est `ownerId` ou `owner_id` ? », « la route renvoie `200` ou `201` ? ».
- **La doc écrite à la main se périme** : un wiki écrit au début du projet décrit des routes qui ont changé depuis, et personne ne le sait.
- **Les intégrations se font à tâtons** : l'équipe qui consomme l'API découvre les erreurs en production plutôt qu'en lisant un contrat.

La réponse standard du métier, c'est **OpenAPI** : une description de l'API (routes, paramètres, corps JSON, codes de réponse) dans un format normalisé, **générée à partir du code** pour qu'elle ne se périme jamais. **Swagger UI** affiche cette description sous forme de page web, avec un bouton pour essayer chaque route.

**Objectif obligatoire de fin de bloc** :

1. La page `http://localhost:8080/swagger-ui.html` liste les routes `users`, `items` et `trades`.
2. Depuis Swagger UI, vous avez créé un utilisateur, puis vous l'avez retrouvé avec `GET /users`.
3. Vous savez où trouver la description OpenAPI brute (`/v3/api-docs`) et à quoi elle sert.

**Point de départ** : le projet de fin de séance 4. Aucune nouvelle route n'est à écrire : on documente l'existant.

## Étape 1 — Ajouter springdoc-openapi

[springdoc-openapi](https://springdoc.org/) est la bibliothèque qui lit vos contrôleurs Spring au démarrage et en déduit la description OpenAPI. Elle embarque aussi Swagger UI.

Ajoutez dans `pom.xml`, dans `<dependencies>` :

```xml
<!-- Séance 5 : documentation de l'API et Swagger UI (http://localhost:8080/swagger-ui.html) -->
<!-- Pas gérée par Spring Boot : la version 3.2.x correspond à Spring Boot 4.1 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.2.0</version>
</dependency>
```

Rechargez Maven.

Remarquez la balise `<version>` : c'est la première fois qu'on en écrit une. Jusqu'ici, toutes nos dépendances venaient de Spring Boot, qui choisit pour nous des versions compatibles entre elles (c'est le rôle du parent `spring-boot-starter-parent`). springdoc est un projet indépendant : c'est à nous de choisir la version qui correspond à notre Spring Boot. Le tableau de compatibilité entre springdoc et Spring Boot est dans la FAQ du [site de springdoc](https://springdoc.org/).

> **Piège** : une version de springdoc prévue pour une autre version de Spring Boot compile souvent sans erreur, puis plante au démarrage avec une `NoSuchMethodError` ou une `ClassNotFoundException`. Si cela vous arrive, vérifiez d'abord la compatibilité des versions.

## Étape 2 — Découvrir Swagger UI

Relancez l'application, puis ouvrez [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

Vous voyez trois groupes : `user-controller`, `item-controller`, `trade-controller`. Ce sont vos trois classes `@RestController`. Dépliez `POST /users` :

- **Request body** : un exemple de JSON, déduit de la classe `User` (`id`, `name`) ;
- **Responses** : le code de retour et le JSON renvoyé.

Vous n'avez rien écrit pour obtenir tout cela : springdoc a lu `@RequestMapping`, `@PostMapping`, `@RequestBody`, les types des paramètres et des valeurs de retour. La documentation **découle du code**. Si vous ajoutez une route demain, elle apparaîtra ici au prochain démarrage.

## Étape 3 — Essayer l'API depuis le navigateur

Rendez la page plus pratique à utiliser. Ajoutez à `src/main/resources/application.properties` :

```properties
# Séance 5 : Swagger UI (http://localhost:8080/swagger-ui.html)
# Bouton « Try it out » déjà activé, et opérations triées par méthode HTTP
springdoc.swagger-ui.try-it-out-enabled=true
springdoc.swagger-ui.operations-sorter=method
```

Relancez, puis rejouez le début du scénario de la séance 3, entièrement depuis Swagger UI :

1. `POST /users` avec `{ "name": "Alice" }`, puis `{ "name": "Bob" }`. Cliquez sur **Execute** : la réponse s'affiche en dessous, avec le code HTTP et la commande `curl` équivalente.
2. `POST /items` avec `{ "name": "tomates", "quantity": 10, "ownerId": 1 }`.
3. `GET /users` : Alice et Bob apparaissent.
4. `GET /users/{id}` avec un id qui n'existe pas : vous retrouvez votre erreur `404` et son message.

Swagger UI ne remplace pas Bruno. Bruno garde vos requêtes et vos environnements dans le projet, versionnés avec le code. Swagger UI sert à **découvrir** une API et à l'essayer sans rien installer.

## Étape 4 — La description OpenAPI brute

Ouvrez [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs). C'est la même information que Swagger UI, mais en JSON : la **description OpenAPI** de votre API. Cherchez-y `/trades/{id}/accept` et le schéma `TradeDecisionRequest`.

Swagger UI n'est qu'un afficheur de ce fichier. D'autres outils savent le lire, et c'est tout l'intérêt d'un format standard :

- **générer un client** : une équipe Angular ou mobile génère automatiquement le code qui appelle votre API, avec les bons types ([OpenAPI Generator](https://openapi-generator.tech/)) ;
- **importer dans un client HTTP** : Bruno et Postman savent créer une collection à partir de cette URL ;
- **comparer deux versions** de l'API pour repérer un changement qui casserait les applications existantes.

## Étape 5 — Vérification

- [ ] `swagger-ui.html` affiche les trois groupes de routes.
- [ ] Un utilisateur créé depuis Swagger UI apparaît dans `GET /users`.
- [ ] `./mvnw test` : tous les tests sont toujours au vert.

## Bonus — Enrichir la documentation

springdoc déduit beaucoup de choses, mais pas l'intention. Des annotations permettent de compléter la description d'une route :

```java
import io.swagger.v3.oas.annotations.Operation;

@Operation(summary = "Accepter un troc",
           description = "Seul le destinataire peut accepter. Les objets sont échangés dans une même transaction.")
@PostMapping("/{id}/accept")
public Trade accept(@PathVariable Long id, @RequestBody TradeDecisionRequest request) {
```

Ajoutez un `@Operation` sur les routes de `TradeController`, relancez, et regardez le résultat dans Swagger UI. N'en mettez pas partout : une annotation qui répète le nom de la méthode n'apporte rien. Réservez-les aux routes dont les règles ne se devinent pas.

## Pour aller plus loin

- [springdoc-openapi — Documentation](https://springdoc.org/)
- [Spécification OpenAPI](https://spec.openapis.org/oas/latest.html) et l'[introduction de la OpenAPI Initiative](https://learn.openapis.org/)
- [Swagger UI — Configuration](https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/)
