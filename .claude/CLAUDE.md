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

Ajouter les dépendances progressivement, en suivant le programme du cours, pas tout d'un coup :
- Session 1 : `spring-boot-starter-web` seulement.
- Session 3 : Spring Data JDBC + H2 (persistance).
- Session 4 : `spring-boot-starter-aop`, tests.

Le `pom.xml` doit refléter ce que les étudiants ont vu à un instant donné du cours.

## Test des endpoints

- Bruno est le client HTTP utilisé pour tester les endpoints (collection versionnée dans
  `bruno/`, format texte `.bru`, un environnement `Local` avec `baseUrl`).
- Quand une nouvelle route est ajoutée dans le TP, ajouter la requête correspondante dans
  `bruno/` (numérotée via `seq`) plutôt que de ne documenter que des commandes `curl`.

## Supports de TP (`doc/`)

- Fichiers numérotés `NN-sessionX-sujet.md` (numérotation globale, deux chiffres) pour garder
  un ordre de lecture stable même quand les docs des sessions suivantes s'ajoutent.
- Auto-suffisants : un étudiant doit pouvoir suivre les étapes sans avoir vu le cours en amont,
  avec des liens vers de la documentation externe en complément (pas en remplacement).
- Ne pas présumer de notions pas encore enseignées. En particulier, JUnit/les tests n'arrivent
  qu'en session 4 — éviter d'y faire référence dans les exercices avant cette session.
- Avancer par petites étapes : introduire une seule notion/entité à la fois plutôt que tout
  d'un bloc (ex. créer l'entité `User` seule avant d'ajouter `Objet`/`Troc` plus tard).
- Pour illustrer un problème technique (ex. couplage fort), préférer une liste de conséquences
  concrètes avec des exemples "dans la vraie vie" professionnels plutôt qu'un exercice qui
  nécessiterait des outils/notions pas encore vus.
