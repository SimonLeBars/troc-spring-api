# Organisation des TP

## Un TP = un concept

Les TP ne sont pas découpés par séance, mais **par concept** : l'injection de dépendances, une API REST, la configuration, la persistance, les transactions… Chaque TP part de la fin du précédent et ajoute une brique à l'API de troc.

Conséquence : **votre groupe avance à son rythme**. Un groupe qui a terminé un TP passe au suivant sans attendre la séance d'après : chaque support est auto-suffisant, et les diapos de toutes les séances sont disponibles dès le début du cours.

| TP | Concept | Fichier | Rythme indicatif |
|---|---|---|---|
| — | Créer le projet | `01-initialisation.md` | séance 1 |
| 1 | Inversion de contrôle et injection de dépendances | `02-tp1-injection-dependances.md` | séance 1 |
| 2 | Une API REST : verbes, chemins, codes de statut | `03-tp2-api-rest.md` | séance 2 |
| 3 | La configuration externalisée | `04-tp3-configuration.md` | séance 2 |
| 4 | Objets de requête (DTO) et règles métier : proposer un troc | `05-tp4-dto-proposer-troc.md` | séance 2 |
| 5 | La persistance : H2 et Spring Data JDBC | `06-tp5-persistance.md` | séance 3 |
| 6 | Les transactions : accepter un troc, tout ou rien | `07-tp6-transactions.md` | séance 3 |
| 7 | La programmation orientée aspect (AOP) | `08-tp7-aop.md` | séance 4 |
| 8 | Les tests automatisés | `09-tp8-tests.md` | séance 4 |
| 9 | Sécuriser l'API : authentification et autorisation | `10-tp9-securite-authentification.md` | séance 5 |
| 10 | Documenter l'API : OpenAPI et Swagger UI | `11-tp10-documentation-openapi.md` | séance 5 |

Le **rythme indicatif** est celui que suit le cours magistral : c'est le minimum à atteindre pour suivre les checkpoints collectifs. Il n'est pas une limite.

## Deux parties par TP

### Partie 1 — Expérimentation (facultative)

Elle sert à **comprendre le problème** que le concept résout, en le provoquant. On y écrit du code volontairement naïf, on casse des choses, on compte, on observe. Le code y est souvent donné en entier : ce qui compte, c'est ce qu'on constate.

Chaque expérimentation commence par quelques questions. **Si votre groupe sait déjà y répondre, passez directement à la mise en place** : c'est le cas typique d'un étudiant qui a déjà utilisé Spring en entreprise ou en projet personnel.

Règles de l'expérimentation :

- le code d'expérimentation va dans le package **`fr.cours.troc.sandbox`** (un sous-package par TP), jamais dans les packages du projet ;
- il est **supprimé** à la fin de l'expérimentation (supprimez le package `sandbox`) ; s'il reste, il ne doit pas gêner la suite ;
- quand une expérimentation demande de modifier temporairement le projet, elle le dit, et vous remettez le code d'origine à la fin (`git restore` sur le fichier, ou **Rollback** dans l'onglet **Commit** d'IntelliJ).

Ainsi, la mise en place ne dépend **jamais** de l'expérimentation : on peut sauter l'une sans être bloqué dans l'autre.

### Partie 2 — Mise en place (obligatoire)

C'est l'implémentation propre du concept dans le projet. Elle donne :

- les **objectifs** : les routes à exposer, les règles à respecter, les codes de statut attendus ;
- les **syntaxes** : annotations, signatures, extraits de configuration, avec leur explication ;
- des **indices** et des questions pour les points délicats.

Elle ne donne **pas** la solution complète : c'est à votre groupe de concevoir l'implémentation à partir des objectifs et des syntaxes. Quand plusieurs réponses se défendent (quel code d'erreur ? refuser ou supprimer en cascade ?), le support le dit : choisissez, et justifiez votre choix dans un commentaire. Ces choix justifiés font partie de l'évaluation du projet.

Chaque mise en place se termine par une **liste de vérification** : tant qu'elle n'est pas entièrement cochée, le TP n'est pas terminé.

### Et ensuite : les bonus

Chaque TP propose quelques bonus, pour aller plus loin sur le même concept. Ils sont facultatifs : un groupe peut aussi bien faire les bonus que passer au TP suivant.

## Travailler en groupe

- Le projet est découpé **par fonctionnalité** (`user/`, `item/`, `trade/`) : c'est ce qui permet de se répartir le travail sans se marcher dessus. Plusieurs TP proposent une organisation pour un groupe de 4.
- Une personne peut faire l'expérimentation pendant qu'une autre attaque la mise en place : mettez ensuite en commun ce que vous avez constaté.
- Commitez à la fin de chaque TP (au minimum), avec un message qui dit ce qui a été fait. Le code d'expérimentation n'a pas besoin d'être commité.

## Le journal de bord : notez vos observations à chaque TP

Tenez un **journal de bord** de groupe : un fichier `JOURNAL.md` à la racine du dépôt, commité avec le reste. À la fin de chaque TP, prenez cinq minutes pour y noter, en quelques lignes :

- **ce qui a posé problème** : une erreur incompréhensible, une notion floue, une étape qui vous a bloqués longtemps ;
- **ce qui aurait pu être fait plus efficacement** : avec le recul, qu'auriez-vous fait autrement, ou plus tôt ?
- **ce qui a bien fonctionné** : une organisation du groupe, un outil, une méthode de débogage ;
- **ce qui était fluide ou intuitif** : ce qui s'est mis en place sans effort.

Notez-le **sur le moment** : trois séances plus tard, on ne se souvient plus que de la solution, pas du chemin.

Certaines questions ne prennent leur sens qu'avec le recul. Exemple : au TP 9, ajouter l'authentification oblige à retoucher des routes, des corps de requête et des tests écrits bien avant. Aurait-il fallu la mettre en place plus tôt pour limiter ces impacts ? Qu'est-ce que ça aurait coûté, à l'inverse, de la faire dès le départ ? Il n'y a pas de bonne réponse unique : ce qui compte, c'est l'argument.

Ce journal sert directement l'évaluation : il est la matière première pour **détailler et justifier vos choix techniques** et pour préparer la présentation finale. Il sera rappelé en début et en fin de chaque séance.

## Tester les routes : Bruno

Les routes sont testées avec [Bruno](https://www.usebruno.com/), un client HTTP dont les collections sont des fichiers texte versionnés dans le dépôt (dossier `bruno/`). La collection est rangée par dossiers : **Utilisateurs**, **Objets**, **Trocs**, et **Expérimentation** pour les routes temporaires des parties 1. Choisissez l'environnement **Local** (`baseUrl = http://localhost:8080`) en haut à droite.
