# Session 1 — TP bloc 1 : ressentir le couplage fort

## Objectif

Avant de découvrir l'injection de dépendances, on va volontairement écrire du code **sans** Spring, avec un couplage fort entre les classes. Le but n'est pas d'écrire du "bon" code ici, mais de sentir concrètement pourquoi on va vouloir de l'IoC/DI juste après.

## Étape 1 — Une première entité du domaine

Créez une classe simple (POJO Java classique : champs privés + getters/setters), dans un package `domain` :

- `User` : `id` (Long), `username` (String), `email` (String)

Pas d'annotation Spring, pas d'interface — juste une classe Java normale. Les entités `Objet` et `Troc` arriveront un peu plus tard, une fois qu'on en aura besoin — pas la peine de tout créer d'un coup. On ajoutera la persistance (Spring Data JDBC) en session 3.

## Étape 2 — Un repository "en dur"

Créez une classe `UserRepositoryImpl` (package `repository`) qui simule une base de données avec une simple `Map<Long, User>` en mémoire :

- `User save(User user)` : génère un id auto-incrémenté si absent, stocke l'utilisateur, le retourne.
- `User findById(Long id)` : retourne l'utilisateur correspondant (ou `null` si absent).

Rien de spécial ici — c'est une classe concrète comme une autre, pas encore de notion d'interface.

## Étape 3 — Le service qui instancie lui-même sa dépendance

Créez `TrocService` (package `service`) avec :

```java
private final UserRepositoryImpl userRepository = new UserRepositoryImpl();
```

Puis deux méthodes : `inscrireUtilisateur(String username, String email)` qui crée un `User` et le sauvegarde, et `consulterUtilisateur(Long id)` qui va chercher un utilisateur par son id.

**Lancer l'application.** À ce stade, vérifiez que tout compile et démarre : `mvnw spring-boot:run` (Linux/Mac) ou `mvnw.cmd spring-boot:run` (Windows). Dans les logs, vous devriez voir `Tomcat started on port 8080`. Rien n'est visible depuis un navigateur pour autant — normal, aucune route HTTP n'existe encore à ce stade, `TrocService` n'est utilisé nulle part. C'est l'objet de l'étape suivante.

## Étape 4 — Exposer le service via un contrôleur REST

Ajoutons un contrôleur pour pouvoir tester tout ça avec un navigateur ou Bruno.

Créez `TrocController` (package `controller`) :

```java
@RestController
@RequestMapping("/api/users")
public class TrocController {

    private final TrocService trocService = new TrocService();

    @PostMapping
    public User inscrire(@RequestBody User user) {
        return trocService.inscrireUtilisateur(user.getUsername(), user.getEmail());
    }

    @GetMapping("/{id}")
    public User consulter(@PathVariable Long id) {
        return trocService.consulterUtilisateur(id);
    }
}
```

Remarquez que `TrocController` fait exactement la même chose que `TrocService` un peu plus haut : il instancie lui-même sa dépendance (`new TrocService()`). C'est volontaire — on garde le même couplage fort à tous les étages pour l'instant.

**Le contenu de la requête POST.** Contrairement à la version précédente (paramètres dans l'URL), la création d'un utilisateur se fait maintenant en envoyant les données dans le **corps** de la requête, au format JSON — c'est la convention standard pour une création de ressource en REST :

- En-tête `Content-Type: application/json`, pour indiquer le format du corps envoyé.
- Corps de la requête :
  ```json
  {
    "username": "alice",
    "email": "alice@example.com"
  }
  ```

`@RequestBody User user` demande à Spring de désérialiser ce JSON directement vers un objet `User` (grâce à la bibliothèque Jackson, incluse avec `spring-boot-starter-webmvc`) : chaque clé du JSON est associée au setter correspondant (`setUsername`, `setEmail`). Le champ `id` n'est pas fourni par le client — il reste à `null` et c'est `UserRepositoryImpl` qui le génère. La réponse renvoyée est le `User` complet, `id` inclus, à nouveau sérialisé en JSON.

Lancez l'application, puis testez les deux routes avec [Bruno](https://www.usebruno.com/) (client HTTP gratuit et open-source, dont les collections sont de simples fichiers texte qu'on peut versionner dans le dépôt) :

1. Ouvrez Bruno → **Open Collection** → sélectionnez le dossier `bruno/` du projet.
2. Choisissez l'environnement **Local** (en haut à droite) — il définit `baseUrl = http://localhost:8080`.
3. Exécutez la requête **Inscrire utilisateur** (POST, avec le corps JSON ci-dessus) : elle crée `alice` et renvoie son JSON avec un `id`.
4. Exécutez la requête **Consulter utilisateur** (GET `/api/users/1`) : elle renvoie le même utilisateur.

Sans Bruno, l'équivalent en ligne de commande :

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com"}'

curl http://localhost:8080/api/users/1
```

## Étape 5 — Les problèmes concrets de cette version

Ce code fonctionne, on vient de le tester. Mais la chaîne `TrocController` → `TrocService` → `UserRepositoryImpl`, où chaque classe instancie elle-même la suivante avec `new`, cache plusieurs problèmes qui deviennent critiques dès qu'un projet grandit :

**1. Impossible à tester unitairement**

`TrocService` ne peut fonctionner qu'avec la vraie `UserRepositoryImpl` — aucun moyen de la remplacer par une version simplifiée pour un test. *Dans une entreprise*, si `UserRepositoryImpl` interrogeait une vraie base PostgreSQL, chaque test de `TrocService` deviendrait un test d'intégration : plus lent, plus fragile (il dépend d'un réseau et d'une base disponibles), et des tests "flaky" (en échec de façon aléatoire) qui ralentissent toute l'équipe en intégration continue (CI/CD).

**2. Changer d'implémentation = modifier tout le code métier**

Si demain le repository doit changer (passer d'un stockage SQL à MongoDB, ou brancher une base différente selon l'environnement), il faut aller modifier chaque classe qui fait `new XxxRepositoryImpl()`. *Dans la vraie vie*, une migration de base de données ou de fournisseur cloud impliquerait ainsi des dizaines, voire des centaines de modifications dispersées dans tout le code, avec un risque de régression à chaque endroit touché.

**3. Impossible de partager une seule instance**

Chaque classe qui a besoin d'un repository en crée sa propre instance. *Dans la vraie vie*, un repository qui parle à une base de données gère en général un pool de connexions, coûteux à créer. Si dix services créent chacun leur propre instance, on multiplie inutilement les pools de connexions ouverts vers la base, jusqu'à épuiser les connexions disponibles côté serveur.

**4. Bascule impossible sans changer le code**

On ne peut pas choisir, au démarrage de l'application, quelle implémentation utiliser (une vraie base en production, une base de test en environnement de recette). *Dans la vraie vie*, une équipe veut typiquement une configuration différente entre "dev", "recette" et "prod" sans dupliquer le code — juste en changeant un fichier de configuration ou une variable d'environnement.

Dans les quatre cas, remarquez bien que le problème n'est jamais *"le code ne marche pas"* : il tourne très bien. Le problème, c'est qu'il est rigide et coûteux à faire évoluer. C'est exactement ce que l'injection de dépendances va résoudre.

## Pour aller plus loin (optionnel à ce stade)

- [Wikipedia — Couplage (informatique)](https://fr.wikipedia.org/wiki/Couplage_(informatique))

*(La suite — comment Spring résout ce problème avec l'injection de dépendances — arrive juste après le checkpoint collectif.)*
