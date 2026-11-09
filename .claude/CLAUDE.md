# SpringFILA1 — recommandations de travail

Contexte : ce dépôt sert à préparer les supports du cours Spring Boot (fil rouge "API de troc
d'objets entre utilisateurs", 4 sessions + 1 jour d'autonomie). C'est le **dépôt de référence
personnel de Simon** — pas le squelette distribué aux étudiants.

## Git

- Toujours demander une confirmation explicite avant `git commit` — montrer ce qui sera commité
  (fichiers + message proposé) et attendre un accord.
- Ne pas redemander à chaque petite étape si l'état actuel peut être commité : Simon dira
  lui-même quand une étape est prête à être commitée.

## Dépendances Maven

Ajouter les dépendances progressivement, en suivant les TP, pas tout d'un coup :
- TP 1 : `spring-boot-starter-web` seulement.
- TP 5 : Spring Data JDBC + H2 (persistance).
- TP 7-8 : `spring-boot-starter-aspectj`, tests.
- TP 9 : Spring Security (`spring-boot-starter-security`, `-security-oauth2-resource-server`, `-security-test`).
- TP 10 : springdoc-openapi.

Le `pom.xml` doit refléter ce que les étudiants ont vu à un instant donné du cours.

## Test des endpoints

- Bruno est le client HTTP utilisé pour tester les endpoints (collection versionnée dans
  `bruno/`, format texte `.bru`, un environnement `Local` avec `baseUrl`).
- Quand une nouvelle route est ajoutée dans le TP, ajouter la requête correspondante dans
  `bruno/` (numérotée via `seq`) plutôt que de ne documenter que des commandes `curl`.

## Supports de TP (`doc/`)

- **Un TP = un concept**, pas une session (`doc/tp/00-organisation-des-tp.md` décrit la structure pour
  les étudiants). La correspondance TP / sessions n'est qu'un rythme indicatif (tableau dans
  `doc/plans/plan-cours-4-sessions.md`) : les groupes en avance passent au TP suivant.
- Fichiers numérotés `NN-tpX-concept.md` (numérotation globale à deux chiffres pour l'ordre de lecture,
  `X` = numéro du TP), plus `00-organisation-des-tp.md` et `01-initialisation.md`.
- Chaque TP a deux parties :
  - **Expérimentation (facultative)** : faire ressentir le problème que le concept résout, en le
    provoquant. Commence par des questions qui permettent aux étudiants expérimentés de la sauter.
    Code jetable dans `fr.cours.troc.sandbox.<concept>`, supprimé à la fin ; le code peut y être
    donné en entier. Les routes temporaires vont dans `bruno/Expérimentation/`.
  - **Mise en place (obligatoire)** : objectifs (routes, règles, codes), syntaxes et indices, sans
    solution complète ; l'implémentation est laissée à la réflexion du groupe. Se termine par une
    liste de vérification. Ne doit jamais dépendre de l'expérimentation.
- Auto-suffisants : un étudiant doit pouvoir suivre les étapes sans avoir vu le cours en amont,
  avec des liens vers de la documentation externe en complément (pas en remplacement).
- Ne pas présumer de notions vues dans un TP ultérieur. En particulier, JUnit/les tests n'arrivent
  qu'au TP 8 — éviter d'y faire référence dans les TP précédents.
- Avancer par petites étapes : introduire une seule notion/entité à la fois plutôt que tout
  d'un bloc (ex. créer l'entité `User` seule avant d'ajouter `Objet`/`Troc` plus tard).
- Pour illustrer un problème technique (ex. couplage fort), préférer une liste de conséquences
  concrètes avec des exemples "dans la vraie vie" professionnels plutôt qu'un exercice qui
  nécessiterait des outils/notions pas encore vus.

## Sessions de grilling (`/grill-me`, skill `grilling`)

- Poser les questions de chaque round via l'outil `AskUserQuestion` plutôt qu'en texte libre.
- Une question par entrée (4 max par appel ; enchaîner plusieurs appels si le round en compte
  plus), la réponse recommandée en première option suffixée « (Recommandé) ».
- Garder les questions ouvertes : Simon peut toujours répondre en texte libre via « Other ».
