# Plan du cours Spring Framework — 4 sessions + 1 jour d'autonomie

## Contexte

- **Public** : étudiants du supérieur avec notions Java (POO, exceptions, collections), Maven, SQL de base, IntelliJ/JDK 25, git/gitlab.
- **Acquis REST** : les étudiants ont déjà développé des API REST, dans une autre technologie que Spring. Les bases (verbes HTTP, codes de statut, conception des URLs, chemin/paramètre/corps, CRUD, clients HTTP) ne sont donc plus enseignées mais **rappelées brièvement** ; le cours se concentre sur leur équivalent Spring et sur ce qui est propre à Spring.
- **Groupe** : 35 étudiants, organisés en groupes de 4 (9 groupes).
- **Format** : 4 sessions de cours/TP de 3h45 chacune + 1 session finale en autonomie (travail de groupe sur le projet, sans cours magistral).
- **Fil rouge** : une API de troc d'objets entre utilisateurs (échanges validés par les deux parties avant exécution).
- **Choix technique** : Spring Data JDBC plutôt que JPA/Hibernate.
- **Critères de notation du projet final** : application fonctionnelle, respect des concepts SQL/transaction/PK/FK/index, détail des choix techniques, vulgarisation en présentation.

## Principes pédagogiques

- Un seul bloc théorique par session (max 30 min), pas d'alternance cours/TP — le reste du temps est dédié à la pratique.
- Diapo + support de cours/TP fournis en amont (lecture non obligatoire avant la session).
- Le support de TP doit être **assez détaillé pour être auto-suffisant** : les étudiants y cherchent comment réussir chaque étape, avec des liens vers de la documentation externe pédagogique en complément.
- Tous les supports (sessions 1 à 4) sont rédigés **en amont**, avant le début du cours.
- Objectif final commun (l'API de troc complète), décliné en jalons obligatoires (un rythme minimum par session) pour garder le groupe synchronisé.
- **Un TP = un concept, pas une session** (voir `doc/tp/00-organisation-des-tp.md`). Chaque TP se découpe en deux parties :
  - **Expérimentation** (facultative) : provoquer le problème que le concept résout, avec du code jetable dans le package `fr.cours.troc.sandbox`. Les étudiants qui savent déjà répondre aux questions d'entrée la sautent.
  - **Mise en place** (obligatoire) : objectifs, syntaxes et indices, sans solution complète ; l'implémentation est laissée à la réflexion du groupe. Se termine par une liste de vérification.
- Les groupes en avance **passent au TP suivant** sans attendre la session d'après (supports auto-suffisants, diapos disponibles dès le début) ; les bonus de chaque TP restent une alternative pour approfondir.
- **Journal de bord** de groupe (`JOURNAL.md`, décrit dans `doc/tp/00-organisation-des-tp.md`) : à chaque TP, noter ce qui a posé problème, ce qui aurait pu être fait plus efficacement, ce qui a bien fonctionné, ce qui était fluide/intuitif. Rappelé **en début et en fin de chaque séance** (diapo « Où en est-on ? » et diapo de synthèse). Exemple de question à faire émerger : l'authentification (TP 9) aurait-elle dû être mise en place plus tôt pour limiter ses impacts sur le code existant ? Sert de base aux choix techniques justifiés et à la présentation finale.
- Checkpoints collectifs à heure fixe (pas déclenchés à la volée), pour que les groupes s'organisent autour. Ils portent sur le TP du rythme indicatif : les groupes en avance y partagent ce qu'ils ont rencontré.

## Correspondance TP / sessions (rythme indicatif)

| TP | Concept | Support | Session |
|---|---|---|---|
| — | Initialisation du projet | `doc/tp/01-initialisation.md` | 1 |
| 1 | Inversion de contrôle et injection de dépendances | `doc/tp/02-tp1-injection-dependances.md` | 1 |
| 2 | API REST : verbes, chemins, codes de statut | `doc/tp/03-tp2-api-rest.md` | 2 |
| 3 | Configuration externalisée | `doc/tp/04-tp3-configuration.md` | 2 |
| 4 | DTO et règles métier : proposer un troc | `doc/tp/05-tp4-dto-proposer-troc.md` | 2 |
| 5 | Persistance : H2 et Spring Data JDBC | `doc/tp/06-tp5-persistance.md` | 3 |
| 6 | Transactions : accepter un troc | `doc/tp/07-tp6-transactions.md` | 3 |
| 7 | AOP : tracer chaque troc | `doc/tp/08-tp7-aop.md` | 4 |
| 8 | Tests automatisés | `doc/tp/09-tp8-tests.md` | 4 |
| 9 | Sécurité : authentification par jeton (Spring Security, JWT) | `doc/tp/10-tp9-securite-authentification.md` | 5 |
| 10 | Documentation OpenAPI / Swagger UI | `doc/tp/11-tp10-documentation-openapi.md` | 5 |

## Structure type d'une session (3h45 = 225 min)

| Bloc | Durée | Contenu |
|---|---|---|
| Théorie | 30 min | Rappel du journal de bord (1 min), concepts schématiques + mise en place Spring (diapo) |
| Pause | 15 min | |
| TP — 1er concept | 60–75 min | Expérimentation (facultative) puis mise en place (obligatoire) |
| **Checkpoint collectif #1** | 10 min | Lever blocages communs + transition vers la notion suivante |
| TP — concept suivant | 60–75 min | Suite du rythme indicatif ; les groupes en avance continuent sur les TP suivants ou les bonus |
| **Checkpoint collectif #2** | 5–10 min | Bilan rapide, attentes pour la session suivante, **5 min pour compléter le journal de bord** |

---

## Session 1 — Fondations Spring + IoC

**Objectif obligatoire de fin de session : une API qui répond à `GET /users` et `POST /users` (données en mémoire).**

Domaine codé en anglais : `User`, `Item` (un objet et sa quantité ; type d'objet libre, les exemples utilisent des légumes), `Trade`. Package racine `fr.cours.troc`, découpé par fonctionnalité (`user/`, `item/`, `trade/`).

- **Présentation du cours (10 min)** : fil rouge, rappel express de ce qu'est une API REST (déjà connu), feuille de route, organisation, évaluation, journal de bord (`JOURNAL.md`, à compléter à chaque TP).
- **Théorie A (20 min)** : SOLID en survol, architecture en couches, couplage fort (`UserService` qui fait `new InMemoryUserRepository()`), inversion des dépendances. Pas une ligne de Spring.
- **TP 1, expérimentation** (sandbox) : `User` + `UserService` couplé en dur, lancé par un `main` en Java pur, puis 3 évolutions demandées par le client, en comptant les fichiers modifiés : (1) `Item` + `ItemService` → bug « utilisateur introuvable » (deux stockages), (2) `LoggingUserRepository` → modifier chaque service, (3) préfixe passé au constructeur → effet cascade ; puis interfaces + injection manuelle (évolution 2 refaite : 1 fichier).
- **Checkpoint #1 + théorie B (20 min)** : blocages Maven/IntelliJ, relevé des chiffres par groupe (pour les groupes qui ont fait l'expérimentation) ; injection par constructeur à la main, conteneur IoC, `@Service`/`@Repository`/`@RestController`, `ApplicationContext`, structure du projet.
- **TP 1, mise en place** : `user`/`item` avec interfaces, `@Repository`/`@Service`, injection par constructeur sans `@Autowired`, `UserController` (`GET`/`POST /users`).
- **Bonus** : profils Spring (`@Profile("dev")`) ; règles de `ItemService`. Les groupes en avance peuvent aussi attaquer le TP 2.
- La testabilité (5e problème du couplage fort) est volontairement gardée pour le TP 8.

Supports : `doc/cours/01-seance-1-fondations.html`, `doc/tp/01-initialisation.md`, `doc/tp/02-tp1-injection-dependances.md`.

### À faire avant la session
- [x] Rédiger le support de TP détaillé (étapes couplage fort → IoC → Spring Boot)
- [x] Préparer la diapo théorie IoC/DI
- [ ] Vérifier le prérequis setup (JDK 25, IntelliJ, git/gitlab) — potentiel temps de setup à prévoir avant le TP

---

## Session 2 — Externalisation + REST

**Objectif : approfondir le Hello World avec de vrais endpoints REST et de la config externalisée.**

- **Théorie (30 min)** : configuration externalisée (~20 min, la vraie nouveauté) : `application.properties`/`yml`, `@Value`/`@ConfigurationProperties`, priorité des sources. Puis REST en Spring (~10 min) : une diapo de rappel REST (verbes, codes, URLs, 400 vs 409), `@PathVariable`/`@RequestParam`/`@RequestBody`, `ResponseEntity`/`@ResponseStatus`/`ResponseStatusException`.
- **TP 2 — API REST** : endpoints CRUD (users, objets) sur données en mémoire, moins guidés qu'en séance 1 (table de correspondance « vous connaissez → en Spring », solutions repliées). Expérimentation : jouer le client (codes renvoyés, ressource inconnue en `200`, `id` imposé par le client).
- **Checkpoint #1** : blocages REST/JSON courants côté Spring.
- **TP 3 — Configuration** : `troc.item.max-per-user` via `@ConfigurationProperties`, priorité des sources. Expérimentation : valeur en dur vs `--server.port` sur le `.jar`.
- **TP 4 — DTO et règles métier** : proposer/consulter un troc (sans persistance ni double validation). Expérimentation : *mass assignment* sur une entité reçue telle quelle.
- **Bonus** : gestion d'erreurs globale (`@RestControllerAdvice`) ; validation des entrées (`@Valid`) ; profils. Le temps gagné sur les rappels REST doit permettre à la plupart des groupes d'atteindre au moins le premier. Ils restent en bonus (pas dans le noyau) : les supports des séances 3 et 4 et le code de référence s'appuient sur `ResponseStatusException`, et `@Valid` demande une dépendance supplémentaire.

Supports : `doc/cours/02-seance-2-configuration-rest.html`, `doc/tp/03-tp2-api-rest.md`, `doc/tp/04-tp3-configuration.md`, `doc/tp/05-tp4-dto-proposer-troc.md`.

### À faire avant la session
- [x] Rédiger le support de TP (externalisation + endpoints CRUD + troc)
- [x] Préparer la diapo théorie config/REST

---

## Session 3 — Persistance + Transactions

**Objectif : données réelles en base, avec un workflow de troc transactionnellement correct.**

- **Théorie (30 min)** : Spring Data JDBC, entités/PK-FK ; lien explicite entre le workflow métier (double validation d'un échange) et la transaction technique (`@Transactional`, rollback si une opération échoue).
- **TP 5 — Persistance** : brancher une vraie base (H2), persister users/objets/trocs. Expérimentation : redémarrage, deux instances, données orphelines.
- **Checkpoint #1** : blocages config datasource/mapping.
- **TP 6 — Transactions** : workflow de troc complet — validation par les deux parties, puis exécution atomique du double transfert d'objets dans une transaction unique. Expérimentation : un déplacement de stock en sandbox qui échoue au milieu, sans puis avec `@Transactional`, et les pièges du proxy. La preuve sur `accept` reste dans la mise en place (attendue dans le rapport).
- **Bonus** : gestion de la concurrence (deux trocs simultanés sur le même objet) ; requêtes personnalisées ; index.

Supports : `doc/cours/03-seance-3-persistance-transactions.html`, `doc/tp/06-tp5-persistance.md`, `doc/tp/07-tp6-transactions.md`.

### À faire avant la session
- [x] Rédiger le support de TP (Spring Data JDBC + transaction du workflow de troc)
- [x] Préparer la diapo théorie transactions/PK-FK

---

## Session 4 — AOP + Tests (session dense, à surveiller)

**Objectif : logging transverse + couverture de tests minimale.**

- **Théorie (30 min)** : AOP (cross-cutting concerns, ex : logger/auditer chaque tentative de troc) ; tests Spring (`@SpringBootTest`, `MockMvc`, unitaire vs intégration).
- **TP 7 — AOP** : aspect de logging sur le service de troc. Expérimentation : le proxy de `@Transactional` (`getClass()`, pile d'appels au debugger), journalisation écrite à la main.
- **Checkpoint #1** : blocages config AOP/tests.
- **TP 8 — Tests** : tests unitaires du service + un test d'intégration sur l'API. Expérimentation : bug caché à retrouver avec Bruno, puis un test tout fait qui le trouve en une seconde.
- **Bonus / si le temps le permet** : aspect avancé (`@Around` avec mesure de temps d'exécution) ; les groupes en avance passent au TP 9 (sécurité).

Supports : `doc/cours/04-seance-4-aop-tests.html`, `doc/tp/08-tp7-aop.md`, `doc/tp/09-tp8-tests.md`.

### ⚠️ Point de vigilance
Cette session concentre deux notions denses (AOP + tests) sur seulement ~2h20 de TP net. Les groupes qui ont pris de l'avance aux sessions précédentes peuvent l'aborder plus tôt ; les autres peuvent sauter les expérimentations des TP 7 et 8. Si du retard s'est accumulé sur les sessions précédentes, c'est ici qu'il faudra couper en premier — **ordre de priorité si le temps manque** :
1. Logging AOP (le noyau, non-négociable)
2. Tests unitaires
3. Test d'intégration
4. Bonus (sautent en premier) ; la sécurité est reportée au TP 9, en séance 5

À annoncer explicitement aux étudiants dès le départ pour gérer leurs attentes.

### À faire avant la session
- [x] Rédiger le support de TP (AOP logging + tests)
- [x] Préparer la diapo théorie AOP/tests

---

## Session 5 — Autonomie de groupe

- Travail en autonomie sur le projet final, sans cours magistral, et rattrapage des TP non terminés.
- **TP 9 — Sécurité** (`doc/tp/10-tp9-securite-authentification.md`) : inscription avec mot de passe haché (BCrypt), `POST /auth/token` qui délivre un JWT, Bearer exigé partout ailleurs, l'identité lue dans le jeton (`proposerId` et `userId` disparaissent des corps). Expérimentation : jouer Mallory (accepter le troc de Bob), ajouter le starter seul (401 partout), hacher un mot de passe, lire et falsifier un JWT. Le cours est l'annexe `doc/cours/05-annexe-spring-security.html`, à lire en autonomie. Fait passer les tests du TP 8 au rouge, puis les répare (`.with(jwt())`).
- **TP 10 — Documentation OpenAPI** (`doc/tp/11-tp10-documentation-openapi.md`) : indépendant du TP 9 ; si la sécurité est en place, ouvrir les routes de springdoc et ajouter le bouton Authorize (bonus).
- Piste d'amélioration possible (pas obligatoire, les groupes peuvent aussi la traiter pendant les TP) : le modèle `Item`, signalé comme « pas idéal » dans les TP 1 et 5. Le genre d'objet n'existe que par son nom en texte libre, mêlé au stock de chaque utilisateur.
- **Fin du cours : rétrospective du journal de bord.** Chaque groupe relit son `JOURNAL.md` et en tire 2 ou 3 enseignements pour la présentation finale (qu'auriez-vous fait plus tôt, ou autrement ? ex. : l'authentification dès le TP 2 plutôt qu'au TP 9 ?).
- Simon disponible pour support technique ponctuel (à cadrer : combien de temps de présence prévu ?).

---

## Sujets non encore tranchés / à trancher plus tard

- Contenu exact et volume des features bonus par session (à détailler session par session).
- Présence/disponibilité de Simon pendant la session 5 (autonomie).
- Sécurité : contenu minimal si elle est abordée (actuellement en bonus session 4).
