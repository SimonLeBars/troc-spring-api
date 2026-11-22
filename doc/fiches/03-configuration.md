# 🔧 Fiche 3 — Configuration

> `TP N` = notion vue à partir du TP N · ✅ à faire · ❌ à éviter · ⚠️ piège · 💡 astuce

## 📄 `application.properties` · `TP 2`

📍 `src/main/resources/application.properties`, lu **au démarrage** (⚠️ modifié → relancer).

```properties
# Une clé par ligne, commentaire sur sa propre ligne
spring.web.error.include-message=always
troc.item.max-per-user=20
```

| Préfixe | Lu par | Exemples |
|---|---|---|
| `server.*` | Spring Boot (serveur web) | `server.port=9090` |
| `spring.*` | Spring Boot | `spring.datasource.url` (TP 5), `spring.profiles.active` |
| `logging.level.*` | Spring Boot (logs) | `logging.level.org.springframework.jdbc.core.JdbcTemplate=DEBUG` (TP 5) |
| `springdoc.*` | springdoc | `springdoc.swagger-ui.operations-sorter=method` (Annexe 1) |
| 🍅 `troc.*` | **nous** | `troc.item.max-per-user`, `troc.trade.max-pending` (TP 4), `troc.jwt.secret` (TP 9) |

- ✅ mots séparés par des tirets : `max-per-user`
- ❌ commentaire en fin de ligne : `troc.item.max-per-user=20 # max` → la valeur devient `20 # max`
- 💡 `application.yml` fait la même chose en indenté : un seul des deux suffit, on garde `.properties`

## 🎚️ Les sources, de la plus forte à la plus faible · `TP 3`

| | Source | Exemple |
|:-:|---|---|
| 🥇 | argument de ligne de commande | `--troc.item.max-per-user=50` |
| 🥈 | variable d'environnement | `TROC_ITEM_MAXPERUSER=50` |
| 🥉 | fichier de profil | `application-demo.properties` |
| 4 | fichier principal | `application.properties` |
| 5 | défaut dans le code | `@DefaultValue("20")` |

💡 La source la plus « proche » du lancement gagne : celui qui lance l'application a le dernier mot, sans toucher au code.

**🔤 Nom d'une variable d'environnement :** MAJUSCULES, `.` → `_`, tirets **supprimés**.

| Clé | ✅ Variable | ❌ |
|---|---|---|
| `troc.item.max-per-user` | `TROC_ITEM_MAXPERUSER` | `TROC_ITEM_MAX_PER_USER` |
| `troc.trade.max-pending` | `TROC_TRADE_MAXPENDING` | `TROC_TRADE_MAX_PENDING` |
| `server.port` | `SERVER_PORT` | |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | |

**⚙️ Où les définir :**

| Où | Argument | Variable d'environnement |
|---|---|---|
| IntelliJ | **Run > Edit Configurations…** > **Program arguments** | **Environment variables** (sinon **Modify options**) |
| Maven | `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` | |
| `.jar` | `java -jar target/troc-api-0.0.1-SNAPSHOT.jar --server.port=9090` | |
| PowerShell | | `$env:SERVER_PORT=9091` (retirer : `$env:SERVER_PORT=$null`) |
| Linux / macOS | | `SERVER_PORT=9091 java -jar …` |

## 📌 Lire une valeur isolée : `@Value` · `TP 3`

```java
public UneClasse(@Value("${troc.une.cle:5}") int valeur) { … }   // ${clé:défaut}
```

- `org.springframework.beans.factory.annotation.Value`, sur un paramètre du **constructeur**
- Spring convertit le texte dans le type du paramètre (`int`, `boolean`, `Duration`…)
- ⚠️ faute de frappe dans la clé → **aucune erreur**, le défaut s'applique en silence

## 📦 Regrouper des réglages : `@ConfigurationProperties` · `TP 3`

```java
@ConfigurationProperties("troc.item")                       // préfixe des clés lues
public record ItemProperties(@DefaultValue("20") int maxPerUser) {   // max-per-user → maxPerUser
}
```

```java
@SpringBootApplication
@ConfigurationPropertiesScan     // ✅ sans lui, ItemProperties n'est pas un bean
public class TrocApplication { … }
```

```java
@Service
public class ItemService {
    private final ItemProperties props;                       // 💉 injecté comme un autre bean
    …
    if (count >= props.maxPerUser()) { … }                    // ✅ accesseur vérifié par le compilateur
}
```

| | `@Value` | `@ConfigurationProperties` |
|---|---|---|
| Pour | une valeur isolée | un **groupe** de réglages d'une fonctionnalité |
| Clé | chaîne libre, non vérifiée | préfixe + noms des composants |
| Dans troc-api | — | `ItemProperties` (TP 3), `TradeProperties` (TP 4), un record `troc.jwt` (TP 9) |

- `@DefaultValue` : `org.springframework.boot.context.properties.bind`
- 💡 `Duration` comprend `1h`, `30m`, `10s` (`troc.jwt.ttl=1h`, TP 9)
- ✅ `ItemService` ne contient ni `20`, ni `"troc.item.max-per-user"`

## 🎭 Profils · `TP 3` (bonus)

```
application.properties             ← toujours lu
application-demo.properties        ← en plus, si le profil "demo" est actif (ne contient que ce qui change)
```

| Activer | |
|---|---|
| IntelliJ | **Run > Edit Configurations…** > **Active profiles** : `demo` |
| Variable | `SPRING_PROFILES_ACTIVE=demo` |
| Test | `@ActiveProfiles("test")` sur la classe (`TP 8`) |

| Profil | Fichier | Contenu | TP |
|---|---|---|:-:|
| `demo` | `src/main/resources/application-demo.properties` | `troc.item.max-per-user=3`, ou une base H2 en mémoire | 3, 5 |
| `test` | `src/test/resources/application-test.properties` | base H2 en mémoire, secret JWT de test | 8, 9 |

⚠️ `src/test/resources/application.properties` **remplace entièrement** celui de `main` : nommez-le `application-test.properties`.

## 🔐 Secrets · `TP 9`

```properties
# lu dans l'environnement : jamais la valeur dans git
troc.jwt.secret=${TROC_JWT_SECRET}
```

- ❌ jamais un mot de passe, une clé, un jeton dans un fichier versionné (sauf une valeur de **test**)
- ⚠️ variable absente → le texte `${TROC_JWT_SECRET}` reste tel quel

## ⌨️ IntelliJ · `TP 3`

⭐ = aide Spring, IntelliJ **Ultimate** (licence étudiante gratuite) · raccourcis Windows / Linux ; macOS et liste générale : [fiche 10](10-raccourcis-intellij.md)

| Raccourci | Effet |
|---|---|
| ⭐ `Ctrl+Espace` dans `application.properties` | les clés de Spring Boot, avec leur documentation et leur valeur par défaut |
| ⭐ clé surlignée | clé inconnue : faute de frappe probable |
| ⭐ `Ctrl+clic` sur une clé | la classe qui la lit |
| `Alt+Shift+F10` | choisir la configuration à lancer (avec ou sans profil `demo`) |
| **Run > Edit Configurations…** > icône **Copy** | dupliquer une configuration : une par profil / port |

## 🚨 Erreurs fréquentes

| Symptôme | 🔎 Cause | TP |
|---|---|:-:|
| la valeur du fichier est ignorée | faute de frappe dans la clé · application pas relancée | 3 |
| `required a bean of type '…ItemProperties'` | `@ConfigurationPropertiesScan` absent · record hors de `fr.cours.troc` | 3 |
| variable d'environnement ignorée | `TROC_ITEM_MAX_PER_USER` au lieu de `TROC_ITEM_MAXPERUSER` | 3 |
| `Failed to convert value of type 'java.lang.String' to required type 'int'` | commentaire en fin de ligne (`20 # max`) | 3 |
| `Web server failed to start. Port 8080 was already in use` | une autre instance tourne déjà : l'arrêter, ou `--server.port=8081` | 3 |
| toute la configuration semble perdue pendant les tests | `src/test/resources/application.properties` au lieu de `application-test.properties` | 8 |

## 🔗 Pour aller plus loin

[Spring Boot — Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) · [Spring Boot — Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html) · [Twelve-Factor — Configuration](https://12factor.net/fr/config)
