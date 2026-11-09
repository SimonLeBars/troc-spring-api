# TP 9 — Documenter l'API : OpenAPI et Swagger UI

> Rythme indicatif : séance 5 (autonomie). Point de départ : n'importe quelle mise en place à partir du TP 4 ; plus l'API a de routes, plus ce TP est parlant. Aucune nouvelle route n'est à écrire : on documente l'existant.

## Le concept en bref

Votre API sera utilisée par d'autres : le développeur front qui affiche la liste des objets, le collègue qui reprend le projet dans six mois, le client qui veut l'essayer depuis son navigateur. Ils ont besoin de savoir quelles routes existent, quel JSON envoyer, quels champs et quels codes reviennent.

**OpenAPI** est le format standard pour décrire une API : routes, paramètres, corps JSON, codes de réponse. **Générée à partir du code**, cette description ne se périme jamais. **Swagger UI** l'affiche sous forme de page web, avec un bouton pour essayer chaque route.

---

## Partie 1 — Expérimentation : utiliser une API sans sa documentation (facultative)

**Vous pouvez passer à la partie 2 si vous savez répondre à ces questions :**

- Pourquoi une documentation d'API écrite à la main finit-elle toujours par être fausse ?
- Qu'est-ce qu'une description OpenAPI, et à quoi peut-elle servir en dehors d'une page web ?

### Étape 1 — Jouez le nouveau venu

Formez un binôme avec une personne d'un **autre groupe** (ou, à défaut, d'un autre sous-groupe du vôtre). Chacun lance son application ; l'autre, **sans ouvrir le code ni la collection Bruno du projet**, en dix minutes :

1. dresse la liste des routes de l'API ;
2. crée un utilisateur, puis un objet à son nom ;
3. trouve le JSON exact à envoyer pour proposer un troc.

Il peut poser des questions à l'oral, comme on les poserait sur Slack. Comptez-les.

### Étape 2 — La documentation écrite à la main

Écrivez maintenant, à la main, dans un fichier texte, la documentation de `POST /items` : chemin, corps attendu, codes de réponse possibles. Puis répondez : que devient ce fichier le jour où quelqu'un renomme `ownerId`, ou ajoute un code `409` ?

### Ce qu'il faut retenir

Dans la vraie vie, une API sans documentation à jour devient vite un problème :

- **les questions arrivent sur Slack** plutôt que dans une doc : « c'est `ownerId` ou `owner_id` ? », « la route renvoie `200` ou `201` ? » ;
- **la doc écrite à la main se périme** : un wiki écrit au début du projet décrit des routes qui ont changé depuis, et personne ne le sait ;
- **les intégrations se font à tâtons** : l'équipe qui consomme l'API découvre les erreurs en production plutôt qu'en lisant un contrat.

La réponse : une documentation **déduite du code**, régénérée à chaque démarrage.

---

## Partie 2 — Mise en place : springdoc-openapi (obligatoire)

### Objectifs

1. La page `http://localhost:8080/swagger-ui.html` liste toutes les routes de l'API, groupées par contrôleur.
2. Depuis Swagger UI, vous avez créé un utilisateur, puis l'avez retrouvé avec `GET /users`.
3. Le bouton **Try it out** est actif par défaut, et les opérations sont triées par méthode HTTP.
4. Vous savez où trouver la description OpenAPI brute (`/v3/api-docs`) et à quoi elle sert.

### La dépendance

[springdoc-openapi](https://springdoc.org/) lit vos contrôleurs au démarrage et en déduit la description OpenAPI. Elle embarque Swagger UI.

```xml
<!-- Documentation de l'API et Swagger UI (http://localhost:8080/swagger-ui.html) -->
<!-- Pas gérée par Spring Boot : la version 3.2.x correspond à Spring Boot 4.1 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.2.0</version>
</dependency>
```

Remarquez la balise `<version>` : c'est la première fois qu'on en écrit une. Jusqu'ici, toutes nos dépendances venaient de Spring Boot, qui choisit des versions compatibles entre elles (le rôle du parent `spring-boot-starter-parent`). springdoc est un projet indépendant : c'est à nous de choisir la version qui correspond à notre Spring Boot, d'après le tableau de compatibilité de la FAQ du [site de springdoc](https://springdoc.org/).

> **Piège** : une version de springdoc prévue pour une autre version de Spring Boot compile souvent sans erreur, puis plante au démarrage avec une `NoSuchMethodError` ou une `ClassNotFoundException`. Vérifiez d'abord la compatibilité des versions.

### Syntaxes utiles

#### Régler Swagger UI

springdoc se règle dans `application.properties`, avec des clés `springdoc.*` (TP 3). Les deux réglages des objectifs :

```properties
springdoc.swagger-ui.try-it-out-enabled=true
springdoc.swagger-ui.operations-sorter=method
```

Les autres réglages sont listés dans la [documentation de springdoc](https://springdoc.org/#properties).

#### Ce que springdoc lit dans votre code

Sans rien écrire, springdoc déduit la documentation de `@RequestMapping`, `@GetMapping`, `@PostMapping`…, de `@PathVariable`, `@RequestParam`, `@RequestBody`, des types des paramètres et des valeurs de retour. Les exemples de JSON viennent des classes (`User`, `ProposeTradeRequest`…) : c'est un argument de plus pour les objets de requête du TP 4, qui documentent exactement ce que le client doit envoyer.

### Vérifier

Ouvrez <http://localhost:8080/swagger-ui.html>. Dépliez `POST /users` : le corps attendu et la réponse sont décrits. Puis rejouez le début d'un scénario, entièrement depuis Swagger UI :

1. `POST /users` avec `{ "name": "Alice" }`, puis `{ "name": "Bob" }`. **Execute** affiche la réponse, le code HTTP et la commande `curl` équivalente.
2. `POST /items` avec `{ "name": "tomates", "quantity": 10, "ownerId": 1 }`.
3. `GET /users` : Alice et Bob apparaissent.
4. `GET /users/{id}` avec un id inexistant : votre `404` et son message.

Ouvrez ensuite <http://localhost:8080/v3/api-docs> : la même information, en JSON. Cherchez-y `/trades/{id}/accept` (si vous avez fait le TP 6) et le schéma `ProposeTradeRequest`. Swagger UI n'est qu'un afficheur de ce fichier ; d'autres outils le lisent :

- **générer un client** : une équipe Angular ou mobile génère le code qui appelle votre API, avec les bons types ([OpenAPI Generator](https://openapi-generator.tech/)) ;
- **importer dans un client HTTP** : Bruno et Postman créent une collection à partir de cette URL ;
- **comparer deux versions** de l'API pour repérer un changement qui casserait les applications existantes.

Swagger UI ne remplace pas Bruno : Bruno garde vos requêtes et vos environnements dans le projet, versionnés avec le code. Swagger UI sert à **découvrir** une API et à l'essayer sans rien installer.

- [ ] `swagger-ui.html` affiche toutes les routes, groupées par contrôleur.
- [ ] **Try it out** est actif sans clic, et les opérations sont triées par méthode.
- [ ] Un utilisateur créé depuis Swagger UI apparaît dans `GET /users`.
- [ ] Si vous avez fait le TP 8 : `./mvnw test` est toujours au vert.

## Bonus — Enrichir la documentation

springdoc déduit beaucoup de choses, mais pas l'intention. Des annotations complètent la description d'une route :

```java
import io.swagger.v3.oas.annotations.Operation;

@Operation(summary = "Un résumé court",
           description = "Les règles qui ne se devinent pas à la lecture de la signature.")
```

Ajoutez un `@Operation` sur les routes de `TradeController`, et regardez le résultat. N'en mettez pas partout : une annotation qui répète le nom de la méthode n'apporte rien. Réservez-les aux routes dont les règles ne se devinent pas (qui peut accepter ? que se passe-t-il pour les stocks ?).

Pour aller plus loin : `@ApiResponse` documente chaque code de réponse possible (`403`, `409`…), avec sa signification.

## Pour aller plus loin

- [springdoc-openapi — Documentation](https://springdoc.org/)
- [Spécification OpenAPI](https://spec.openapis.org/oas/latest.html) et l'[introduction de la OpenAPI Initiative](https://learn.openapis.org/)
- [Swagger UI — Configuration](https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/)
