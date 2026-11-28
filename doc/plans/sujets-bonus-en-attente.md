# Sujets bonus en attente de validation

Candidats repérés en complément de la liste de la section 6 de
[`evolution-seance-2-securite.md`](evolution-seance-2-securite.md) (novembre 2026). À valider par
Simon avant d'être intégrés à `doc/tp/sujets-bonus.md`.

Les nouveautés de Spring Framework 7 / Spring Boot 4 (versionnement d'API, `@Retryable`,
`@ConcurrencyLimit`) sont à vérifier dans la documentation de Boot 4.1 avant rédaction.

## Candidats à ajouter

| Séance | Sujet | Application au troc |
|---|---|---|
| 1 | Virtual threads (`spring.threads.virtual.enabled`) | Une propriété, cohérent avec Java 25 ; ouvre la discussion sur la tenue en charge |
| 1 | Docker Compose support (`spring-boot-docker-compose`) | Démarrer PostgreSQL automatiquement en dev, dans la continuité du `Dockerfile` |
| 2 | Versionnement d'API (Spring Framework 7) | `/v1` et `/v2` des trocs |
| 2 | HTTP interface clients (`@HttpExchange`) | À regrouper avec RestClient : appeler une API externe (géolocalisation des objets…) |
| 2 | Spring HATEOAS | Liens `accept` / `refuse` dans la réponse d'un troc en attente, selon son état |
| 2 | Spring GraphQL | Autre exposition des utilisateurs, objets et trocs, à comparer avec REST |
| 3 | Spring Data Redis ou MongoDB | Cache distribué, ou autre modèle de persistance à comparer avec JDBC |
| 4 | `@Retryable` / `@ConcurrencyLimit` (Spring Framework 7) | À présenter avec Resilience4j : ce que le framework fait seul, ce qu'il ne fait pas |
| 4 | Messaging : Spring for Apache Kafka ou Spring AMQP | Publier « troc accepté » vers un autre service, prolonge les événements Spring |
| 4 | Spring AI | Générer la description d'un objet, suggérer des échanges |
| 4 | Image native GraalVM (AOT) et Buildpacks | Démarrage rapide, moins de RAM : concret avec Render (512 Mo, mise en veille) |

Avec ces ajouts, on passe à 7-8 sujets par séance au lieu de 5-6. Pour rester à 6, retirer en
priorité Spring GraphQL (doublon de REST) et Redis / MongoDB (doublon de JPA).

## À citer seulement (« pour aller plus loin »), sans en faire un sujet

- **Spring WebFlux / R2DBC** : autre modèle de programmation, pas un ajout au projet ; à mettre en
  regard des virtual threads.
- **Spring Cloud** (Gateway, Config, OpenFeign) : n'a de sens qu'avec plusieurs services.
- **Spring Batch** : pas de cas d'usage naturel dans le troc.
- **Spring Session**, **Spring Data REST**, **Spring REST Docs**, **Spring Shell**,
  **Spring Statemachine** : marginaux ou redondants avec ce qui est déjà au programme.

## Validés

- **MapStruct** et **Lombok** : retenus comme bonus de TP (voir la section 6 de
  [`evolution-seance-2-securite.md`](evolution-seance-2-securite.md)).
