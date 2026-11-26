# Évolution : Spring Security en séance 2 et sujets bonus

Plan issu d'une séance de grilling (novembre 2026). À mettre en place sur la branche
`claude/session-2-spring-security-k3dyj8`, en 6 lots, chacun présenté pour accord avant commit.

## Objectifs

- **Compresser la partie REST** de la séance 2 (les étudiants connaissent déjà REST) et utiliser le
  temps gagné pour **Spring Security**.
- **TP sécurité juste après les TP de la séance 2** (après le TP 4).
- **Groupes de 4** : rendre explicites les dépendances entre TP pour que les groupes se répartissent
  le travail.
- **Sujets bonus « environnement Spring »** proposés à chaque séance, non traités en cours, à creuser
  dans le projet de groupe.

## Décisions

### 1. Renumérotation des TP

Le TP sécurité devient le **TP 5**, sur données en mémoire. Les TP suivants avancent d'un numéro :

| Avant | Après | Séance |
|---|---|---|
| TP 9 Sécurité | **TP 5** | 2 |
| TP 5 Persistance | TP 6 | 3 |
| TP 6 Transactions | TP 7 | 3 |
| TP 7 AOP | TP 8 | 4 |
| TP 8 Tests | TP 9 | 4 |

À mettre à jour : noms de fichiers `doc/tp/NN-tpX-…`, liens et renvois (« comme au TP 5 »…),
diapos, requêtes Bruno d'expérimentation (`TP6 - Déplacer du stock` → `TP7 - …`), plan du cours,
FAQ, fiches, et la progression des dépendances dans `.claude/CLAUDE.md` :

- TP 1 : `spring-boot-starter-webmvc`.
- TP 5 : Spring Security (`spring-boot-starter-security`, `-security-oauth2-resource-server`).
- TP 6 : Spring Data JDBC + H2.
- TP 8-9 : `spring-boot-starter-aspectj`, tests (dont `spring-security-test`).
- Annexe 1 : springdoc-openapi.

**Fiches mémo** renumérotées dans le même ordre : sécurité → 05, persistance → 06, AOP → 07,
tests → 08.

### 2. Nouveau TP 5 — Sécurité

- **Périmètre JWT complet**, adapté au stockage en mémoire :
  - inscription avec mot de passe haché (BCrypt), stocké dans le repository en mémoire ;
  - `POST /auth/token` délivre un JWT (HS256) ;
  - jeton Bearer exigé sur toutes les autres routes ;
  - utilisateur lu dans le jeton : `proposerId` et `userId` disparaissent des corps de requête.
- **Mise en place réduite au noyau** ci-dessus. Passent en bonus : rôles ADMIN, « chacun ses
  objets », « mes trocs », test de bout en bout.
- **Expérimentation** : Mallory propose un troc au nom de Bob (`proposerId` dans le corps de
  `POST /trades`, TP 4), puis modifie ou supprime le compte de Bob. Le reste ne change pas : ajouter
  seulement le starter (`401` partout), hacher un mot de passe, lire et falsifier un JWT.
- Pas de référence aux tests (ils n'arrivent qu'au TP 9), ni à la console H2, ni au `schema.sql`.
- Lien vers l'annexe de cours sécurité pour lire plus en détail.

### 3. Jalon et adaptation des TP 6 à 9

- **Le TP 5 est un prérequis de la séance 3** (à terminer entre les séances si besoin). Jalon de
  fin de séance 2 : TP 2 à 5.
- Les TP 6 à 9 supposent la sécurité en place (une seule version des consignes) :
  - **TP 6 Persistance** : le mot de passe haché passe en base (`password_hash`), recherche du
    compte par une requête dérivée (`Optional<User>`), ouverture de `/h2-console/**` et
    `frameOptions(sameOrigin())`.
  - **TP 7 Transactions** : le `403` « seul le destinataire peut accepter » s'appuie sur
    l'utilisateur connecté.
  - **TP 8 AOP** : les traces peuvent inclure l'utilisateur connecté.
  - **TP 9 Tests** : tests MockMvc écrits avec `.with(jwt())` dès le départ, plus les cas `401`
    sans jeton et `403` d'un utilisateur qui n'est pas le destinataire. Le bonus « test de bout en
    bout » (vrai jeton) y trouve sa place.

### 4. Séance 2 : cours et organisation

- **Cours** : Simon allège lui-même le contenu existant (configuration, REST, trocs). Une partie
  « Sécurité » condensée, tirée de `05-annexe-spring-security.html`, est ajoutée au deck, renommé
  `02-seance-2-configuration-rest-securite.html`. Première version proposée, Simon la taille ensuite.
- **Annexe de cours sécurité** gardée comme lecture approfondie, référencée depuis le TP 5.
- **Répartition en groupe** (`doc/tp/00-organisation-des-tp.md`) :
  - schéma des dépendances entre TP : le TP 3 ne dépend d'aucun autre ; les TP 4 et 5 dépendent
    du TP 2 (`User`) ; etc. ;
  - répartition suggérée dans un groupe de 4, les groupes restent libres ;
  - en tête de chaque TP : « peut commencer dès … / en parallèle de … ».

### 5. Séance 4 et séance 5

- **Séance 4**, point de vigilance : les groupes en avance passent aux sujets bonus (plus au TP 9
  sécurité).
- **Séance 5** (autonomie), sans TP sécurité : rattrapage, sujets bonus dans le projet de groupe,
  amélioration du modèle `Item`, bilan du journal de bord. Annexe 1 (OpenAPI) : ouvrir les routes
  de springdoc et ajouter le bouton Authorize (désormais faisable dès la séance 2).
- **Question de journal de bord** remplacée par : « Qu'a coûté l'arrivée de la sécurité dans le
  code existant, et qu'a-t-elle facilité ensuite ? »

### 6. Sujets bonus

- Fichier unique **`doc/tp/sujets-bonus.md`**, une section par séance, 5 à 6 sujets chacune,
  dans la continuité de ce que la séance vient de voir :

  | Séance | Sujets |
  |---|---|
  | 1 | DevTools, Actuator, Initializr et les starters |
  | 2 | Bean Validation, Problem Details (RFC 9457), CORS, OAuth2 Authorization Server, RestClient |
  | 3 | Flyway ou Liquibase, Spring Data JPA, cache (`@Cacheable`), pagination, Testcontainers avec PostgreSQL |
  | 4 | Micrometer / observabilité, `@Async` / `@Scheduled`, événements Spring, Resilience4j, Spring Modulith, ArchUnit |

- Pour chaque sujet : à quoi il sert (2-3 phrases), une idée d'application au troc, la dépendance
  à ajouter, 2-3 liens (documentation officielle, tutoriel).
- Distinction explicite avec les bonus de TP (même concept, guidés) : ces sujets ne sont pas vus en
  cours, les groupes les explorent seuls.
- Référencé depuis `00-organisation-des-tp.md` et la diapo de clôture de chaque séance.
- **Notation** : valorisable (présenté dans les choix techniques, justifié dans `JOURNAL.md`), non
  obligatoire, aucune pénalité pour les groupes qui n'en font pas.

### 7. Code de référence

- Ajouter Spring Security au code : JWT, BCrypt, `SecurityFilterChain`, identité lue dans le jeton.
- Tests adaptés avec `jwt()` + tests `401`/`403`.
- Bruno : requête d'inscription et de connexion, jeton envoyé en Bearer sur les autres requêtes
  (variable d'environnement), dans `Local` et `Render`.
- Secret JWT : variable d'environnement (`TROC_JWT_SECRET`), valeur de test dans
  `application-test.properties` ; à configurer sur Render.
- `./mvnw verify` au vert.

## Lots de livraison

Chaque lot est présenté (fichiers + message de commit) pour accord avant commit.

1. **Renumérotation** : TP, fiches, Bruno, liens, plan du cours, FAQ, `.claude/CLAUDE.md`.
2. **Contenu des TP** : nouveau TP 5 et adaptation des TP 6 à 9, `00-organisation-des-tp.md`
   (schéma des dépendances, répartition), séances 4 et 5 dans le plan.
3. **Code de référence** : sécurité, tests, Bruno, configuration Render.
4. **Diapos** : partie sécurité dans le deck de la séance 2, renvois dans les autres decks.
5. **Sujets bonus** : `doc/tp/sujets-bonus.md` et ses renvois.
6. **PDF** : régénération (séance 2, TP, fiches), après la relecture du deck par Simon.

## Points restant ouverts

- Contenu exact du deck de la séance 2 après l'allègement par Simon.
- Présence de Simon pendant la séance 5 (déjà ouvert dans le plan du cours).
