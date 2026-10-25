# Plan du cours Spring Framework — 4 sessions + 1 jour d'autonomie

## Contexte

- **Public** : étudiants du supérieur avec notions Java (POO, exceptions, collections), Maven, SQL de base, IntelliJ/JDK 25, git/gitlab.
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
- Objectif final commun (l'API de troc complète), décliné en jalons obligatoires par session pour garder le groupe synchronisé.
- Les groupes en avance reçoivent des **features bonus**, pas un accès anticipé au programme suivant — pour que les checkpoints collectifs restent utiles à tout le monde.
- Checkpoints collectifs à heure fixe (pas déclenchés à la volée), pour que les groupes s'organisent autour.

## Structure type d'une session (3h45 = 225 min)

| Bloc | Durée | Contenu |
|---|---|---|
| Théorie | 30 min | Concepts schématiques + mise en place Spring (diapo) |
| Pause | 15 min | |
| TP — bloc 1 | 60–75 min | Étapes obligatoires (noyau) |
| **Checkpoint collectif #1** | 10 min | Lever blocages communs + transition vers la notion suivante |
| TP — bloc 2 | 60–75 min | Suite du noyau, ou features bonus pour les groupes en avance |
| **Checkpoint collectif #2** (si besoin) | 5–10 min | Bilan rapide, attentes pour la session suivante |

---

## Session 1 — Fondations Spring + IoC

**Objectif obligatoire de fin de session : une API qui répond à `GET /users` et `POST /users` (données en mémoire).**

Domaine codé en anglais : `User`, `Item` (un objet et sa quantité ; type d'objet libre, les exemples utilisent des légumes), `Trade`. Package racine `fr.cours.troc`, découpé par fonctionnalité (`user/`, `item/`, `trade/`).

- **Présentation du cours (10 min)** : fil rouge, API REST, feuille de route, organisation, évaluation.
- **Théorie A (20 min)** : SOLID en survol, architecture en couches, couplage fort (`UserService` qui fait `new InMemoryUserRepository()`), inversion des dépendances. Pas une ligne de Spring.
- **TP bloc 1 (noyau)** : `User` + `UserService` couplé en dur, lancé par un `main` en Java pur, puis 3 évolutions demandées par le client, en comptant les fichiers modifiés : (1) `Item` + `ItemService` → bug « utilisateur introuvable » (deux stockages), (2) `LoggingUserRepository` → modifier chaque service, (3) préfixe passé au constructeur → effet cascade.
- **Checkpoint #1 + théorie B (20 min)** : blocages Maven/IntelliJ, relevé des chiffres par groupe ; injection par constructeur à la main, conteneur IoC, `@Service`/`@Repository`/`@RestController`, `ApplicationContext`, structure du projet.
- **TP bloc 2 (noyau)** : interfaces + injection manuelle (évolution 2 refaite : 1 fichier), puis Spring (`@Repository`/`@Service`, injection par constructeur sans `@Autowired`), `UserController` (`GET`/`POST /users`), évolution 2 refaite avec Spring (déplacer `@Repository`).
- **Bonus (groupes avancés)** : profils Spring (`@Profile("dev")`) ; endpoint supplémentaire `GET /users/{id}`.
- La testabilité (4e problème du couplage fort) est volontairement gardée pour la séance 4.

Supports : `doc/cours/01-seance-1-fondations.html`, `doc/tp/01-session1-initialisation.md`, `doc/tp/02-session1-tp-bloc1-couplage-fort.md`, `doc/tp/03-session1-tp-bloc2-ioc.md`.

### À faire avant la session
- [x] Rédiger le support de TP détaillé (étapes couplage fort → IoC → Spring Boot)
- [x] Préparer la diapo théorie IoC/DI
- [ ] Vérifier le prérequis setup (JDK 25, IntelliJ, git/gitlab) — potentiel temps de setup à prévoir avant le TP

---

## Session 2 — Externalisation + REST

**Objectif : approfondir le Hello World avec de vrais endpoints REST et de la config externalisée.**

- **Théorie (30 min)** : `application.properties`/`yml`, `@Value`/`@ConfigurationProperties` ; `@PathVariable`/`@RequestParam`/`@RequestBody` ; codes de statut HTTP.
- **TP bloc 1 (noyau)** : endpoints CRUD basiques (users, objets) sur données en mémoire ; externalisation d'un paramètre métier (ex : nombre max d'objets par échange).
- **Checkpoint #1** : blocages REST/JSON courants.
- **TP bloc 2 (noyau)** : endpoints pour proposer/consulter un troc (sans persistance ni double validation pour l'instant).
- **Bonus** : gestion d'erreurs globale (`@ControllerAdvice`) ; validation des entrées (`@Valid`).

### À faire avant la session
- [ ] Rédiger le support de TP (externalisation + endpoints CRUD + troc)
- [x] Préparer la diapo théorie config/REST

---

## Session 3 — Persistance + Transactions

**Objectif : données réelles en base, avec un workflow de troc transactionnellement correct.**

- **Théorie (30 min)** : Spring Data JDBC, entités/PK-FK ; lien explicite entre le workflow métier (double validation d'un échange) et la transaction technique (`@Transactional`, rollback si une opération échoue).
- **TP bloc 1 (noyau)** : brancher une vraie base (H2), persister users/objets/trocs.
- **Checkpoint #1** : blocages config datasource/mapping.
- **TP bloc 2 (noyau)** : workflow de troc complet — validation par les deux parties, puis exécution atomique du double transfert d'objets dans une transaction unique (si le retrait de l'objet A réussit mais l'ajout de l'objet B échoue, tout est annulé).
- **Bonus** : gestion de la concurrence (deux trocs simultanés sur le même objet) ; requêtes personnalisées ; index.

### À faire avant la session
- [ ] Rédiger le support de TP (Spring Data JDBC + transaction du workflow de troc)
- [x] Préparer la diapo théorie transactions/PK-FK

---

## Session 4 — AOP + Tests (session dense, à surveiller)

**Objectif : logging transverse + couverture de tests minimale.**

- **Théorie (30 min)** : AOP (cross-cutting concerns, ex : logger/auditer chaque tentative de troc) ; tests Spring (`@SpringBootTest`, `MockMvc`, unitaire vs intégration).
- **TP bloc 1 (noyau)** : aspect de logging sur le service de troc.
- **Checkpoint #1** : blocages config AOP/tests.
- **TP bloc 2 (noyau)** : tests unitaires du service + un test d'intégration sur l'API.
- **Bonus / si le temps le permet** : introduction sécurité (Spring Security basique) ; aspect avancé (`@Around` avec mesure de temps d'exécution).

### ⚠️ Point de vigilance
Cette session concentre deux notions denses (AOP + tests) sur seulement ~2h20 de TP net. Si du retard s'est accumulé sur les sessions précédentes, c'est ici qu'il faudra couper en premier — **ordre de priorité si le temps manque** :
1. Logging AOP (le noyau, non-négociable)
2. Tests unitaires
3. Test d'intégration
4. Sécurité (bonus, saute en premier)

À annoncer explicitement aux étudiants dès le départ pour gérer leurs attentes.

### À faire avant la session
- [ ] Rédiger le support de TP (AOP logging + tests)
- [x] Préparer la diapo théorie AOP/tests

---

## Session 5 — Autonomie de groupe

- Travail en autonomie sur le projet final, sans cours magistral.
- Simon disponible pour support technique ponctuel (à cadrer : combien de temps de présence prévu ?).

---

## Sujets non encore tranchés / à trancher plus tard

- Contenu exact et volume des features bonus par session (à détailler session par session).
- Format précis des supports de TP auto-suffisants (structure des étapes, niveau de détail des liens de doc externe).
- Présence/disponibilité de Simon pendant la session 5 (autonomie).
- Sécurité : contenu minimal si elle est abordée (actuellement en bonus session 4).
