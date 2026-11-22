# FAQ — questions fréquentes des étudiants

Questions posées (ou attendues) pendant les TP, rangées par TP. Réponses courtes, avec des liens
pour approfondir.

---

## Généralités — Pourquoi Spring

### 65 % des développeurs Java utilisent Spring. Que font les autres ?

Les 35 % restants n'utilisent pas tous un autre framework. L'enquête autorise plusieurs réponses,
et une bonne partie des développeurs Java n'écrit pas d'applications web côté serveur.

#### 1. Des backends construits avec un autre framework

| Framework | Où on le rencontre |
|---|---|
| **Jakarta EE** (ex-Java EE), sur un serveur d'applications (WildFly/JBoss, WebLogic, WebSphere, Payara) | Banque, assurance, administration : des applications anciennes, toujours maintenues |
| **Quarkus** (Red Hat), **Micronaut** | Applications pensées pour le cloud et Kubernetes : démarrage rapide, peu de mémoire, compilation native avec GraalVM |
| **Helidon**, **Vert.x**, **Javalin**, **Dropwizard**, **Play** (et **Ktor** en Kotlin) | Usages plus légers ou plus spécialisés (réactif, micro-services minimalistes) |

#### 2. Java sans framework web

- **Android** : des applications mobiles, historiquement en Java, aujourd'hui surtout en Kotlin.
- **Big data et streaming** : Kafka, Spark, Flink, Hadoop et Elasticsearch tournent sur la JVM,
  et on développe des traitements par-dessus.
- **Applications desktop** : JavaFX, Swing. Les IDE IntelliJ IDEA et Eclipse sont eux-mêmes
  écrits en Java.
- **Outillage et bibliothèques** : plugins Maven ou Gradle, plugins d'IDE, bibliothèques open
  source.
- **Jeux** : Minecraft (édition Java), et libGDX pour d'autres jeux.
- **Systèmes embarqués et cartes à puce** : Java Card, sur les cartes SIM et les cartes bancaires.
- **Finance haute performance** : du trading à faible latence, en Java très optimisé et souvent
  sans framework.

#### Ce qu'il faut en retenir

Pour les API et les backends d'entreprise, contexte du cours et de la plupart des offres
d'alternance, Spring domine largement. Ses concurrents directs sont Jakarta EE pour l'ancien code
et Quarkus/Micronaut pour le cloud-native. Les notions vues en cours (injection de dépendances,
inversion de contrôle, API REST) se retrouvent dans tous ces frameworks : les apprendre avec Spring
permet de passer facilement à un autre.

#### Pour aller plus loin

- [JetBrains — The State of Developer Ecosystem 2025](https://www.jetbrains.com/lp/devecosystem-2025/)
- [Jakarta EE](https://jakarta.ee/)
- [Quarkus](https://quarkus.io/)
- [Micronaut](https://micronaut.io/)

---

## TP 1 — Injection de dépendances

### On crée des interfaces pour pouvoir changer d'implémentation, mais Spring n'en injecte qu'une seule. A-t-on vraiment plusieurs implémentations d'une même interface dans la vraie vie ?

Oui, c'est courant. Et la prémisse est à nuancer : Spring injecte **une seule implémentation par
point d'injection** (un paramètre de constructeur), pas une seule dans toute l'application. On
rencontre trois situations.

#### 1. Une seule implémentation active, choisie selon l'environnement

C'est le cas le plus fréquent : plusieurs classes existent, mais une seule devient un bean au
démarrage.

| Interface | Implémentations | Comment Spring choisit |
|---|---|---|
| `EmailSender` | `SmtpEmailSender` (production), `ConsoleEmailSender` (poste du développeur : affiche l'email au lieu de l'envoyer) | `@Profile("prod")` / `@Profile("!prod")` |
| `FileStorage` | `S3FileStorage` (cloud), `LocalFileStorage` (disque local) | `@ConditionalOnProperty(name = "storage.type", havingValue = "s3")` |
| `UserRepository` | `InMemoryUserRepository`, puis une version avec base de données | on remplace la classe, les services ne bougent pas |

- C'est ce que fait le **Bonus 2** du TP 1 avec `LoggingUserRepository` et `@Profile("dev")`.
- Le **TP 5** en est un exemple grandeur nature : le stockage en mémoire est remplacé par une
  base de données, sans toucher aux services.

#### 2. Plusieurs implémentations actives, utilisées à des endroits différents

Toutes les implémentations sont des beans, et chaque classe indique celle qu'elle veut.

- Un site marchand accepte la carte bancaire (`StripePaymentGateway`) et PayPal
  (`PaypalPaymentGateway`), deux implémentations de `PaymentGateway`.
- Une application lit dans sa base principale et écrit des statistiques dans une base séparée :
  deux beans `DataSource`.

```java
public RefundService(@Qualifier("stripePaymentGateway") PaymentGateway gateway) { … }
```

- `@Qualifier("nom")` désigne un bean par son nom (par défaut, le nom de la classe avec une
  minuscule au début).
- `@Primary` sur une implémentation en fait le choix par défaut quand rien n'est précisé.

Sans l'un ou l'autre, Spring s'arrête au démarrage avec `required a single bean, but 2 were found`.

#### 3. Toutes les implémentations à la fois

Spring sait injecter **toutes** les implémentations d'une interface dans une liste ou une map :

```java
@Service
public class NotificationService {

    private final List<NotificationSender> senders; // email, SMS, notification mobile…

    public NotificationService(List<NotificationSender> senders) {
        this.senders = senders;
    }

    public void notify(User user, String message) {
        for (NotificationSender sender : senders) {
            sender.send(user, message);
        }
    }
}
```

```java
// clé = nom du bean : choix de l'implémentation à l'exécution, selon la requête
public PaymentService(Map<String, PaymentGateway> gateways) { … }
```

- Ajouter un canal de notification = **ajouter une classe**, sans modifier `NotificationService`.
- C'est le **pattern Strategy** : une famille d'algorithmes interchangeables derrière une même
  interface.
- Même usage pour une liste de règles de validation : chaque règle est une classe, le service les
  applique toutes.
- Spring lui-même fonctionne ainsi : la conversion objet ↔ JSON passe par une liste de
  `HttpMessageConverter` (JSON, XML, texte…), et Spring Security (TP 9) par une chaîne de filtres.
- Dans le troc : prévenir un utilisateur qu'il a reçu une proposition, à la fois par email et
  dans l'application.

#### Et les tests ?

Une autre implémentation très courante est la **fausse** : une passerelle de paiement qui ne
débite personne, un envoi d'email qui ne fait que mémoriser le message. Ce sera vu au TP 8.

#### La nuance honnête

En entreprise, **beaucoup d'interfaces n'ont qu'une seule implémentation**. Créer une interface
« au cas où » se discute (principe *YAGNI*, *You Aren't Gonna Need It*). Elle reste utile quand
elle sert de **contrat** entre couches, quand on veut pouvoir **substituer** l'implémentation
(environnement, tests) ou quand une **migration** est prévisible.

Clin d'œil au TP 5 : avec Spring Data, on écrit **seulement l'interface** du repository, et c'est
Spring qui fabrique l'implémentation.

#### Pour aller plus loin

- [Spring Framework — Fine-tuning Annotation-based Autowiring with Qualifiers](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html)
- [Spring Framework — Using `@Autowired`](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html) (injection de collections et de maps)
- [Spring Framework — Fine-tuning with `@Primary`](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-primary.html)
- [Wikipédia — Stratégie (patron de conception)](https://fr.wikipedia.org/wiki/Strat%C3%A9gie_(patron_de_conception))

### Quelle est la différence entre `@SpringBootApplication`, `@Service`, `@Repository`… puisque ce sont tous des beans au final ?

Ce sont bien tous des beans : Spring les détecte au démarrage, les crée et les injecte de la même
façon. Ces annotations se distinguent sur deux points :

1. **Le sens** : elles indiquent le rôle de la classe dans l'architecture.
2. **Le comportement** : certaines ajoutent un traitement en plus de « je suis un bean ».

#### La base : `@Component`

`@Service`, `@Repository` et `@Controller` sont des **stéréotypes** : chacune est elle-même
annotée `@Component`. Si on ouvre le code de `@Service`, on trouve à peu près ceci :

```java
@Component
public @interface Service { … }
```

Le scan de composants cherche `@Component`, directement ou à travers une autre annotation. C'est
pour ça qu'elles produisent toutes un bean.

#### Ce que chacune ajoute

| Annotation | Ce qu'elle apporte en plus de `@Component` |
|---|---|
| `@Service` | **Rien de technique.** Elle sert à documenter que la classe contient de la logique métier. Elle est aussi utile pour cibler « toutes les classes de service » d'un coup (aspects, TP 7). |
| `@Repository` | La **traduction des exceptions** : les exceptions propres à la base (`SQLException`, erreurs du pilote…) sont converties en `DataAccessException`, une hiérarchie commune à Spring. Le code appelant ne dépend donc pas de la base utilisée. |
| `@Controller` | Spring MVC repère la classe et enregistre ses méthodes `@GetMapping`, `@PostMapping`… comme des routes HTTP. |
| `@RestController` | `@Controller` + `@ResponseBody` : la valeur renvoyée est convertie en JSON au lieu d'être vue comme le nom d'une page HTML à afficher. |
| `@Configuration` | La classe sert de source de définitions `@Bean`. Spring l'entoure d'un proxy pour qu'un appel entre méthodes `@Bean` renvoie le même singleton au lieu de créer une nouvelle instance. |

Au TP 5, avec Spring Data, une interface qui étend `ListCrudRepository` n'a pas besoin de
`@Repository` : Spring Data la détecte et l'implémente lui-même.

#### `@SpringBootApplication` : un cas à part

Elle ne se range pas avec les autres. Ce n'est pas un rôle de couche mais un **raccourci pour
trois annotations** :

```java
@SpringBootConfiguration   // = @Configuration → la classe est bien un bean
@EnableAutoConfiguration   // active l'auto-configuration selon les dépendances présentes
@ComponentScan             // scanne le package de cette classe et ses sous-packages
```

C'est elle qui démarre tout le mécanisme :

- **`@ComponentScan`** explique pourquoi la classe principale doit être à la racine
  (`fr.cours.troc`). Un `@Service` placé dans un package frère, hors de `fr.cours.troc`, ne serait
  jamais trouvé.
- **`@EnableAutoConfiguration`** crée lui-même des beans selon les dépendances présentes. Par
  exemple, la dépendance web suffit pour obtenir un serveur Tomcat et la conversion JSON sans
  rien écrire.

#### En résumé

Toutes ces annotations produisent un bean. `@Service` indique seulement l'intention.
`@Repository` et `@Controller`/`@RestController` déclenchent en plus un traitement spécifique.
`@Configuration` déclare d'autres beans. `@SpringBootApplication` lance le scan et
l'auto-configuration qui font apparaître tous les autres.

Techniquement, mettre `@Component` partout fonctionnerait pour les services. En revanche, on
perdrait la traduction des exceptions des repositories, la lisibilité, et la possibilité de cibler
les classes selon leur rôle.

#### Pour aller plus loin

- [Spring Framework — Classpath Scanning and Managed Components](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html) (`@Component` et stéréotypes)
- [Spring Framework — Exception Translation](https://docs.spring.io/spring-framework/reference/data-access/orm/general.html#orm-exception-translation) (`@Repository`)
- [Spring Boot — Using the `@SpringBootApplication` Annotation](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html)
