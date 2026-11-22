# Aide à la présentation · Séance 1

## Mode d'emploi

Une section par diapo de `01-seance-1-fondations.html` : un discours oral à adapter, les
transitions avec les diapos voisines, et les réponses aux questions probables. Les notes
`<aside class="notes">` du support restent la référence pour les faits.

## Diapo 11 · Pourquoi Spring est né

Message à faire passer : Spring est né pour alléger les EJB, et son idée centrale (le conteneur
IoC) n'a pas changé en vingt ans. Durée indicative : 2 min 30.

### Transition depuis la diapo 10

« On vient de voir ce que Java vous apporte en tant que langage. Avant de toucher à Spring, je
veux vous expliquer *pourquoi* il existe, parce que le problème qu'il a résolu en 2004 est
exactement celui qu'on va résoudre dans ce cours. »

### La frise, de gauche à droite

« Fin des années 90, les entreprises veulent faire du Java côté serveur : des applications
bancaires, des systèmes de réservation… Sun publie une norme pour ça, J2EE, et son composant
phare, les **EJB**, les Enterprise JavaBeans.

Sur le papier, l'idée est excellente : vous écrivez votre code métier, et un serveur
d'application (WebLogic, WebSphere) s'occupe à votre place des transactions, de la sécurité et
des appels réseau. »

### Encadré rouge « Le problème »

« Dans la pratique, c'était un enfer. Pour écrire un simple service avec *une seule* méthode
utile, il fallait :

- deux interfaces techniques, avec des méthodes imposées comme `ejbCreate` ou `ejbRemove`, que
  vous laissiez presque toujours vides ;
- un fichier XML pour décrire ce composant ;
- un serveur d'application lourd, long à démarrer, juste pour que votre classe puisse exister.

Et la conséquence la plus grave : pour tester votre code, il fallait démarrer ce serveur. Donc
en pratique, on testait très peu. »

### 2002 : Rod Johnson

« En 2002, un développeur, Rod Johnson, publie un livre qui dit en substance : “on peut avoir
les mêmes services sans toute cette lourdeur”. Il fournit le code qui le prouve, et ce code
devient **Spring 1.0** en 2004. »

### Encadré de droite « L'idée de Spring »

« L'idée de Spring tient en trois points :

- vous écrivez des classes Java **ordinaires**, ce qu'on appelle des POJO : pas d'interface ni
  d'héritage imposés par le framework ;
- un conteneur **léger** se charge de les créer et de les brancher entre elles ;
- du coup, votre code se teste sans aucun serveur.

Ce mécanisme porte un nom, l'**inversion de contrôle**, et c'est le cœur de la partie 04, juste
après. »

### Fin de la frise, puis la légende

« Ensuite, ce qui a changé au fil des versions, c'est surtout la *façon de configurer* ce
conteneur : d'abord en XML, puis avec des annotations à partir de 2007, et enfin en 2014
**Spring Boot**, qui configure presque tout automatiquement : vous montez une API en quelques
minutes.

Aujourd'hui, on travaille en Java 25 et Spring Boot 4.1, mais retenez ceci : vingt ans plus
tard, l'idée au centre n'a pas bougé, c'est **le conteneur IoC**. Si vous la comprenez, vous
comprenez Spring. »

### Transition vers la diapo 12

« Justement, “Spring” et “Spring Boot”, on les confond souvent. Voyons comment ils
s'empilent. »

### Questions possibles

| Question | Réponse |
| --- | --- |
| « Les EJB, ça existe encore ? » | J2EE est devenu **Jakarta EE**, et Spring en utilise lui-même certains standards (Servlet, Bean Validation). |
| « Comment on récupérait un EJB ? » | Le code client allait le chercher lui-même dans un annuaire (JNDI), soit l'exact inverse de l'injection de dépendances. C'est une bonne accroche pour amener la partie 04. |
| « C'est quoi un POJO ? » | Plain Old Java Object : une classe Java normale, sans héritage ni interface imposés. Notre `UserService` en est un. |

## Diapo 12 · Un framework, un écosystème, et Boot par-dessus

Message à faire passer : Spring et Spring Boot ne sont pas concurrents, Boot est construit sur
Spring Framework et le configure à votre place. Durée indicative : 2 min.

Conseil : lire le schéma **de bas en haut**, comme une pile, en montrant chaque couche.

### Transition depuis la diapo 11

« On vient de voir l'idée de Spring. Mais dans les offres d'emploi, vous lirez “Spring”,
“Spring Boot”, parfois les deux, comme si c'était la même chose. Ce n'en est pas une : ce sont
des couches posées l'une sur l'autre. »

### La JVM, tout en bas

« Tout en bas, la JVM. Spring Boot 4 exige au minimum Java 17 ; nous, on travaille en Java 25,
la dernière version à support long. »

### Spring Framework, le cœur

« Au-dessus, **Spring Framework** : c'est le Spring de 2004 dont on vient de parler. Son cœur,
c'est le conteneur **IoC**, l'injection de dépendances : la partie 04 de cette séance.

Autour de ce cœur, des modules que vous allez croiser pendant le cours :

- **Web MVC**, pour écrire nos contrôleurs REST, dès aujourd'hui ;
- **Transactions** et **accès aux données**, à la séance 3 ;
- **AOP**, la programmation par aspects, à la séance 4. »

### Les projets de l'écosystème

« À côté, des projets séparés, construits sur le framework. Chacun répond à un besoin précis :
Spring Data pour la persistance, Spring Security pour l'authentification, Spring Cloud pour les
microservices, Spring Batch pour les traitements de masse, et plus récemment Spring AI.

Dans ce cours, on en utilisera deux : Spring Data JDBC à la séance 3, et Spring Security
ensuite. »

### Spring Boot, au-dessus

« Et Spring Boot, alors ? Boot n'apporte presque aucun concept nouveau. Il **assemble et
configure** tout ce qui est en dessous, à votre place :

- les **starters** : une seule dépendance Maven au lieu de dix ;
- l'**auto-configuration** : des réglages par défaut raisonnables, branchés automatiquement ;
- un **serveur embarqué** : votre application démarre avec un simple `main`, sans serveur
  d'application à installer, ce qui répond directement au problème des EJB ;
- **Actuator**, pour surveiller l'application en production.

C'est ce qu'on appelle “convention plutôt que configuration” : vous ne configurez que ce qui
s'écarte de l'habitude. »

### Votre code, tout en haut

« Et tout en haut, votre code : `UserController`, `UserService`, `UserRepository`. Ce sont des
classes ordinaires, et tout le reste de la pile travaille pour elles. »

### Transition vers la diapo 13

« Avant de voir comment Spring branche ces trois classes entre elles, il faut comprendre
pourquoi on les découpe ainsi et ce qui se passe quand on les branche mal. Dans la partie 03,
il n'y aura pas une ligne de Spring : on pose le problème. »

### Questions possibles

| Question | Réponse |
| --- | --- |
| « Peut-on utiliser Spring sans Boot ? » | Oui, c'était la norme avant 2014, et on le croise encore dans des applications anciennes. Mais il faut alors tout configurer soi-même (serveur, beans, dépendances compatibles entre elles). |
| « Boot, c'est plus lent ou plus lourd ? » | Non : Boot ne charge que ce que les starters présents réclament. La configuration automatique se fait au démarrage, pas à chaque requête. |
| « Spring Data, ce n'est pas l'accès aux données du framework ? » | Le module du framework fournit les bases (`JdbcTemplate`, transactions). Spring Data, au-dessus, génère les repositories à partir d'une interface. |
| « Quelle différence avec Jakarta EE ? » | Jakarta EE est une norme implémentée par plusieurs serveurs. Spring est un framework unique, qui utilise lui-même certains standards Jakarta (Servlet, Bean Validation). |
