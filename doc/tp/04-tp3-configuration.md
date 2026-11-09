# TP 3 — La configuration externalisée

> Rythme indicatif : séance 2. Point de départ : la mise en place du TP 2 terminée (CRUD `/users` et `/items`).

## Le concept en bref

Certaines valeurs changent **sans que le code change** : une limite métier (« 20 objets par utilisateur »), un port, l'adresse d'une base de données, un mot de passe. Les écrire dans le code oblige à modifier, recompiler et redéployer à chaque changement, et à livrer les secrets avec le code.

La **configuration externalisée** sort ces valeurs du code : elles sont lues au démarrage depuis un fichier (`application.properties`), une variable d'environnement ou un argument de ligne de commande. La **même** application compilée peut ainsi tourner en développement, en démonstration et en production, avec des réglages différents.

---

## Partie 1 — Expérimentation : changer une valeur sans recompiler (facultative)

**Vous pouvez passer à la partie 2 si vous savez répondre à ces questions :**

- Pourquoi une limite métier écrite en dur dans le code pose-t-elle problème, même si elle ne change « presque jamais » ?
- Une même clé est définie dans `application.properties` **et** dans une variable d'environnement : laquelle l'emporte ?
- Pourquoi un mot de passe de base de données n'a-t-il rien à faire dans le code, ni dans git ?

Le code temporaire va dans le package `fr.cours.troc.sandbox.config`.

### Étape 1 — Une valeur écrite en dur

Le client fixe une règle : un utilisateur ne peut pas déclarer plus de 20 objets. Écrivons-la naïvement, dans une route de démonstration :

```java
package fr.cours.troc.sandbox.config;

@RestController
public class ConfigDemoController {

    private static final int MAX_PER_USER = 20; // la règle, en dur

    @GetMapping("/sandbox/config")
    public Map<String, Object> config() {
        return Map.of("maxPerUser", MAX_PER_USER);
    }
}
```

Fabriquez l'application **telle qu'on la livrerait**, sous forme d'un fichier `.jar`, puis lancez ce fichier :

```bash
./mvnw package                                  # Windows : mvnw.cmd package
java -jar target/troc-api-0.0.1-SNAPSHOT.jar
```

Ouvrez <http://localhost:8080/sandbox/config> (ou **Expérimentation > TP3 - Lire la configuration** dans Bruno) : `{"maxPerUser": 20}`.

> *Le client* : « Pour la démonstration de demain, je voudrais une limite à 3 objets, pour montrer le message d'erreur. Mais en production, on garde 20. »

Faites-le. Notez chaque étape nécessaire, et ce qu'il faudra refaire après la démonstration.

### Étape 2 — Une valeur que Spring Boot lit dans la configuration

Arrêtez l'application. Sans modifier ni recompiler quoi que ce soit, relancez **le même fichier** avec un argument :

```bash
java -jar target/troc-api-0.0.1-SNAPSHOT.jar --server.port=9090
```

L'application écoute maintenant sur le port 9090 : <http://localhost:9090/sandbox/config>. Le port est une valeur que Spring Boot lit dans la configuration, avec une valeur par défaut (8080).

Recommencez avec une variable d'environnement :

```bash
# Linux / macOS
SERVER_PORT=9091 java -jar target/troc-api-0.0.1-SNAPSHOT.jar

# Windows PowerShell
$env:SERVER_PORT=9091; java -jar target/troc-api-0.0.1-SNAPSHOT.jar
```

Puis avec les deux à la fois (variable `SERVER_PORT=9091` **et** argument `--server.port=9090`). Quel port gagne ?

Enfin, ajoutez `server.port=9092` dans `src/main/resources/application.properties`, refabriquez le `.jar`, et relancez-le sans argument ni variable, puis avec l'argument. Pensez à retirer la variable d'environnement (`$env:SERVER_PORT=$null` sous PowerShell, ou fermez le terminal).

<details>
<summary>Ce que vous devriez observer</summary>

L'argument de ligne de commande l'emporte sur la variable d'environnement, qui l'emporte sur `application.properties`, qui l'emporte sur la valeur par défaut. La valeur la plus « proche » du lancement gagne : celui qui lance l'application a le dernier mot, sans toucher au code.

</details>

### Ce qu'il faut retenir

- Une valeur en dur dans le code se change en modifiant le code : recompiler, relivrer, et ne pas oublier de revenir en arrière. Personne ne saura dans six mois d'où vient ce `20`.
- Spring Boot lit ses propres réglages (`server.port`…) dans plusieurs sources, par ordre de priorité. Nos règles métier peuvent profiter **du même mécanisme** : c'est l'objet de la partie 2.
- C'est ce mécanisme qui permet de fournir les **secrets** (mot de passe de la base…) par l'environnement du serveur : ils ne sont jamais écrits dans le code, ni dans git.

**Supprimez le package `fr.cours.troc.sandbox`, et retirez `server.port` d'`application.properties`.**

---

## Partie 2 — Mise en place : les règles métier dans la configuration (obligatoire)

### Objectifs

1. La règle « un utilisateur ne peut pas déclarer plus de N objets » est appliquée par `POST /items` : `409 Conflict` si l'utilisateur a déjà N objets.
2. N est lu depuis la clé `troc.item.max-per-user` d'`application.properties` (valeur : `20`), avec `20` comme valeur par défaut si la clé est absente.
3. La valeur est lue par un record `ItemProperties` (`@ConfigurationProperties`), injecté dans `ItemService`.
4. On peut changer N **sans modifier une ligne de Java**, par le fichier ou par une variable d'environnement.

Pourquoi `409` et pas `400` ? La requête est parfaitement valide : déclarée par un utilisateur qui n'a encore rien, elle serait acceptée. C'est **l'état actuel des données** qui l'empêche (voir le tableau des codes du TP 2).

### Syntaxes utiles

#### Déclarer une clé

```properties
# Règles métier du troc
troc.item.max-per-user=20
```

- Les clés `spring.*` et `server.*` sont lues par Spring Boot ; les clés `troc.*` sont **les nôtres**. Le préfixe évite toute collision.
- Les mots sont séparés par des tirets (`max-per-user`) : c'est la convention recommandée.
- Le format `application.yml` fait la même chose avec une syntaxe indentée. Un seul des deux suffit : on garde `.properties`.

#### Lire une valeur isolée : `@Value`

```java
public UneClasse(@Value("${troc.une.cle:valeur-par-defaut}") int valeur) { … }
```

`@Value` (package `org.springframework.beans.factory.annotation`) se place sur un paramètre du constructeur : la valeur est une dépendance comme une autre. `${clé:défaut}` : la valeur de la clé, ou le défaut si la clé est absente. Spring convertit le texte dans le type du paramètre.

**Limite** : la clé est une simple chaîne. Une faute de frappe (`${troc.item.max-per-usr:20}`) ne provoque **aucune erreur** : l'application démarre et utilise le défaut, quelle que soit la valeur du fichier.

#### Regrouper les réglages d'une fonctionnalité : `@ConfigurationProperties`

```java
@ConfigurationProperties("troc.exemple")              // préfixe des clés à lire
public record ExempleProperties(@DefaultValue("5") int maxQuelqueChose) { // max-quelque-chose → maxQuelqueChose
}
```

- Un **record** (Java 16+) est une classe immuable déclarée en une ligne : Java génère le constructeur et un accesseur par composant, ici `maxQuelqueChose()`. Voir [dev.java — Records](https://dev.java/learn/records/).
- Spring lit toutes les clés qui commencent par le préfixe et remplit le record ; la conversion `max-quelque-chose` → `maxQuelqueChose` est automatique.
- `@DefaultValue` (package `org.springframework.boot.context.properties.bind`) joue le rôle du `:défaut` de `@Value`.
- Spring doit être prévenu qu'il doit chercher ces classes : ajoutez `@ConfigurationPropertiesScan` sur `TrocApplication`, à côté de `@SpringBootApplication`.

Le record est ensuite un bean : on l'injecte par le constructeur, et on lit la valeur avec son accesseur, vérifié par le compilateur.

> **`@Value` ou `@ConfigurationProperties` ?** `@Value` pour une valeur isolée ; `@ConfigurationProperties` dès qu'un groupe de réglages apparaît pour une fonctionnalité. D'autres réglages `troc.item.*` et `troc.trade.*` viendront : utilisez `@ConfigurationProperties`.

#### Les sources de configuration, de la plus forte à la plus faible

| Priorité | Source | Exemple |
|---|---|---|
| 1 | argument de ligne de commande | `--troc.item.max-per-user=50` |
| 2 | variable d'environnement | `TROC_ITEM_MAXPERUSER=50` |
| 3 | fichier de profil | `application-dev.properties` (voir bonus) |
| 4 | fichier principal | `application.properties` |
| 5 | valeur par défaut dans le code | `@DefaultValue("20")` |

Nom d'une variable d'environnement : tout en majuscules, points remplacés par `_`, tirets supprimés.

Pour définir une variable d'environnement dans IntelliJ : **Run > Edit Configurations…** > `TrocApplication` > **Environment variables** (si le champ n'apparaît pas : **Modify options > Environment variables**). En ligne de commande, l'équivalent d'un argument :

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--troc.item.max-per-user=1
```

### Indices

- Pour compter les objets d'un utilisateur, ajoutez au repository une méthode `long countByOwnerId(Long ownerId)` (le nom servira au TP 5).
- La vérification de la limite ne concerne que `create` : modifier un objet existant ne change pas leur nombre.

### Vérifier

- [ ] Avec `troc.item.max-per-user=2` dans le fichier, le troisième **Créer objet (Alice)** répond `409`, avec un message clair. Aucune ligne de Java n'a changé entre `20` et `2`.
- [ ] La clé retirée du fichier, la valeur par défaut `20` s'applique.
- [ ] Avec la variable d'environnement `TROC_ITEM_MAXPERUSER=1` dans la configuration de lancement, le deuxième objet d'Alice est refusé, alors que le fichier dit `20`. Retirez ensuite la variable.
- [ ] `ItemService` ne contient pas le nombre `20`, ni la chaîne `"troc.item.max-per-user"`.
- [ ] `application.properties` contient `troc.item.max-per-user=20` (et toujours `spring.web.error.include-message=always`).

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| La limite est ignorée | Faute de frappe dans la clé (fichier ou annotation), ou `@ConfigurationPropertiesScan` absent de `TrocApplication`. |
| `required a bean of type 'fr.cours.troc.item.ItemProperties'` | `@ConfigurationPropertiesScan` absent, ou `ItemProperties` placé en dehors de `fr.cours.troc`. |
| La variable d'environnement est ignorée | Nom mal formé (`TROC_ITEM_MAX_PER_USER` au lieu de `TROC_ITEM_MAXPERUSER`), ou application pas relancée. |
| `Failed to convert value of type 'java.lang.String' to required type 'int'` | La valeur du fichier n'est pas un nombre (espace, commentaire sur la même ligne). |
| La valeur ne change pas après modification du fichier | Le fichier est lu au **démarrage** : relancez l'application. |

## Bonus

### Bonus 1 — Un fichier par environnement : les profils

Un **fichier de profil** `application-<profil>.properties` ne contient que ce qui **change** pour ce profil ; toutes les autres clés viennent toujours d'`application.properties`.

1. Créez `src/main/resources/application-demo.properties` avec `troc.item.max-per-user=3`.
2. Activez le profil : **Run > Edit Configurations…** > **Active profiles** : `demo` (ou la variable d'environnement `SPRING_PROFILES_ACTIVE=demo`).
3. Vérifiez que la limite passe à 3, et revient à 20 sans le profil.

Documentation : [Spring Boot — Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html).

### Bonus 2 — Refuser une configuration absurde

Que se passe-t-il avec `troc.item.max-per-user=-5` ? Faites en sorte que l'application **refuse de démarrer** avec un message clair : ajoutez la dépendance `spring-boot-starter-validation`, l'annotation `@Validated` sur le record et `@Positive` sur son composant. Une erreur au démarrage vaut mieux qu'une règle silencieusement fausse en production.

Documentation : [Spring Boot — `@ConfigurationProperties` Validation](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.validation).

## Pour aller plus loin

- [Spring Boot — Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) (toutes les sources et leur ordre de priorité)
- [The Twelve-Factor App — III. Configuration](https://12factor.net/fr/config) (pourquoi la configuration vit dans l'environnement)
- [dev.java — Records](https://dev.java/learn/records/)
