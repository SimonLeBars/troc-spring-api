# FAQ — questions fréquentes des étudiants

Questions posées (ou attendues) pendant les TP, rangées par TP. Réponses courtes, avec des liens
pour approfondir.

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

- C'est ce que fait le **Bonus 1** du TP 1 avec `LoggingUserRepository` et `@Profile("dev")`.
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
