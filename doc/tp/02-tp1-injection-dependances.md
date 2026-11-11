# TP 1 — Inversion de contrôle et injection de dépendances

> Rythme indicatif : séance 1. Point de départ : le projet créé avec `01-initialisation.md`.

## Le concept en bref

Une classe a besoin d'autres objets pour travailler : un service a besoin d'un repository, un contrôleur a besoin d'un service. La question est : **qui fabrique ces objets ?**

- Sans précaution, chaque classe les fabrique elle-même avec `new`. C'est le **couplage fort** : chaque classe choisit et construit ses dépendances, et tout changement se propage dans tout le code.
- Avec l'**injection de dépendances**, une classe **reçoit** ses dépendances (par son constructeur) au lieu de les fabriquer. Quelqu'un d'autre décide quoi lui passer.
- Avec l'**inversion de contrôle** (IoC), ce « quelqu'un d'autre » est un **conteneur**, ici Spring : il crée les objets (les *beans*) et les branche entre eux au démarrage.

---

## Partie 1 — Expérimentation : codez-le mal, exprès (facultative)

**Vous pouvez passer à la partie 2 si vous savez répondre à ces questions :**

- Quand chaque classe fabrique elle-même ses dépendances avec `new`, qui décide combien d'instances d'une même dépendance existent dans l'application ? Citez des ressources qui doivent être partagées (une seule instance pour toute l'application) et ce qui se passe si chaque classe crée la sienne (pensez à un pool de connexions, un cache, un compteur).
- Que faut-il modifier dans le code pour remplacer une implémentation de repository par une autre, quand chaque service fait son propre `new` ? Et quand les services reçoivent le repository par leur constructeur ?
- Qu'est-ce que l'« effet cascade » d'un changement de constructeur ?

Dans cette partie, on écrit du code **sans** Spring, en Java pur, avec un couplage fort : chaque classe fabrique ce dont elle a besoin avec `new`. Le but n'est pas d'écrire du « bon » code, mais de **ressentir** les problèmes : le client va demander trois évolutions, et pour chacune vous noterez combien de fichiers vous avez dû modifier. Ces chiffres seront mis en commun au checkpoint.

Tout le code de cette partie va dans le package **`fr.cours.troc.sandbox.couplage`** et sera supprimé à la fin. Aucune annotation Spring.

Rappel de l'architecture en couches : un **service** porte les règles métier, un **repository** stocke et retrouve les données. Ici, le repository garde ses données dans une simple `Map` en mémoire.

### Étape 1 — Un utilisateur, son stockage, son service

1. Une classe `User` : un *POJO* (*Plain Old Java Object*) avec deux champs privés, `id` (`Long`) et `name` (`String`), et leurs getters/setters (IntelliJ : `Alt+Insert` sous Windows/Linux, `Cmd+N` sous macOS > **Getter and Setter**).

2. Une classe `InMemoryUserRepository` qui stocke les utilisateurs dans une map :

   ```java
   public class InMemoryUserRepository {

       private final Map<Long, User> users = new HashMap<>();
       private final AtomicLong nextId = new AtomicLong(1); // compteur : 1, 2, 3…

       public User save(User user) {
           if (user.getId() == null) {
               user.setId(nextId.getAndIncrement()); // renvoie la valeur, puis l'augmente de 1
           }
           users.put(user.getId(), user);
           return user;
       }

       public List<User> findAll() {
           return new ArrayList<>(users.values());
       }

       public User findById(Long id) {
           return users.get(id); // null si absent
       }
   }
   ```

3. Une classe `UserService`, qui fabrique **elle-même** son repository :

   ```java
   public class UserService {

       // le service choisit ET fabrique sa dépendance
       private final InMemoryUserRepository users = new InMemoryUserRepository();

       public List<User> findAll() {
           return users.findAll();
       }

       public User create(User user) {
           return users.save(user);
       }
   }
   ```

   C'est le code « naïf » que tout le monde écrit spontanément. Gardez-le tel quel.

4. Une classe `ManualDemo`, lancée avec la flèche verte à gauche de `main` :

   ```java
   public class ManualDemo {

       public static void main(String[] args) {
           UserService userService = new UserService();

           User alice = new User();
           alice.setName("Alice");
           userService.create(alice);

           User bob = new User();
           bob.setName("Bob");
           userService.create(bob);

           for (User user : userService.findAll()) {
               System.out.println(user.getId() + " " + user.getName());
           }
       }
   }
   ```

   La console affiche `1 Alice` puis `2 Bob`. Tout fonctionne. Passons aux demandes du client.

### Étape 2 — Les évolutions demandées par le client

Pour chaque évolution, remplissez ce tableau (sur papier ou dans un fichier texte) :

| Évolution | Fichiers créés | Fichiers existants modifiés | Problème rencontré |
|---|---|---|---|
| 1 — Les objets | | | |
| 2 — Les traces | | | |
| 3 — Le préfixe | | | |

#### Évolution 1 — Les utilisateurs déclarent leurs objets

> *Le client* : « Chaque utilisateur doit pouvoir déclarer les objets qu'il possède, avec leur quantité. Un objet appartient forcément à un utilisateur qui existe. »

1. Une classe `Item` : `id` (`Long`), `name` (`String`), `quantity` (`int`), `ownerId` (`Long`), avec getters/setters.
2. `InMemoryItemRepository`, sur le modèle d'`InMemoryUserRepository`, avec `save` et `findAll`.
3. `ItemService`, qui a besoin des utilisateurs pour vérifier le propriétaire. Comme `UserService`, il fabrique ce dont il a besoin :

   ```java
   public class ItemService {

       private final InMemoryItemRepository items = new InMemoryItemRepository();
       private final InMemoryUserRepository users = new InMemoryUserRepository();

       public List<Item> findAll() {
           return items.findAll();
       }

       public Item create(Item item) {
           if (users.findById(item.getOwnerId()) == null) {
               throw new IllegalArgumentException("Utilisateur introuvable : " + item.getOwnerId());
           }
           return items.save(item);
       }
   }
   ```

4. À la fin de `ManualDemo.main`, Alice déclare des tomates :

   ```java
   ItemService itemService = new ItemService();

   Item tomatoes = new Item();
   tomatoes.setName("tomates");
   tomatoes.setQuantity(10);
   tomatoes.setOwnerId(alice.getId());
   itemService.create(tomatoes);

   System.out.println(itemService.findAll().size() + " objet(s) déclaré(s)");
   ```

Relancez. Le programme s'arrête sur `IllegalArgumentException: Utilisateur introuvable : 1`. Pourtant Alice vient d'être créée, avec l'`id` 1. **Cherchez pourquoi** avant d'ouvrir l'explication : combien d'objets `InMemoryUserRepository` existent dans ce programme ?

<details>
<summary>Explication</summary>

`UserService` et `ItemService` font chacun leur propre `new InMemoryUserRepository()` : il y a **deux** maps d'utilisateurs, qui s'ignorent. Alice est enregistrée dans celle de `UserService`, `ItemService` cherche dans la sienne, qui est vide.

</details>

Ne corrigez pas : notez le bug dans le tableau, et mettez en commentaire la ligne `itemService.create(tomatoes);` pour continuer.

#### Évolution 2 — Tracer les opérations

> *Le client* : « Pendant la mise au point, je veux voir dans la console chaque opération faite sur les utilisateurs. »

1. Créez `LoggingUserRepository` : une copie d'`InMemoryUserRepository` dont chaque méthode affiche aussi une ligne, par exemple `System.out.println("[USERS] save " + user.getId() + " " + user.getName());`.
2. Faites en sorte que **tous** les services utilisent ce nouveau stockage.

Combien de fichiers avez-vous dû ouvrir pour le point 2 ? Imaginez la même demande sur un projet de 30 services.

#### Évolution 3 — Choisir le préfixe des traces

> *Le client* : « Je veux savoir qui a fait l'appel : `[USERS]` pour la gestion des utilisateurs, `[ITEMS]` pour la vérification faite par la gestion des objets. »

1. Ajoutez à `LoggingUserRepository` un champ `private final String prefix;`, un constructeur `LoggingUserRepository(String prefix)`, et utilisez `prefix` dans les `System.out.println`.
2. Compilez (**Build > Build Project**). Corrigez **chaque** erreur.

L'ajout d'un paramètre de constructeur a cassé tous les endroits qui faisaient `new LoggingUserRepository()` : c'est l'**effet cascade**.

### Étape 3 — Le remède : recevoir au lieu de fabriquer

Toujours dans le sandbox, corrigez le code en trois gestes :

1. **Un contrat** : une interface `UserRepository` (`save`, `findAll`, `findById`), implémentée par `InMemoryUserRepository` et `LoggingUserRepository` (`implements UserRepository`, et `@Override` sur chaque méthode).
2. **Les services reçoivent leurs dépendances** par leur constructeur, et ne connaissent plus que l'interface :

   ```java
   public class UserService {

       private final UserRepository users;

       public UserService(UserRepository users) { // reçu, plus fabriqué
           this.users = users;
       }
       // findAll() et create() ne changent pas
   }
   ```

   Faites de même pour `ItemService`, qui reçoit un `UserRepository` (et son `InMemoryItemRepository`).
3. **Quelqu'un doit faire les `new`** : c'est maintenant `ManualDemo`, et lui seul.

   ```java
   UserRepository users = new InMemoryUserRepository();
   UserService userService = new UserService(users);
   ItemService itemService = new ItemService(new InMemoryItemRepository(), users);
   ```

Décommentez `itemService.create(tomatoes);` et relancez : `1 objet(s) déclaré(s)`. Les deux services reçoivent **le même** objet `users` : le bug de l'évolution 1 a disparu.

Refaites maintenant l'évolution 2 (repasser au stockage avec traces). Combien de fichiers modifiez-vous ?

<details>
<summary>Réponse</summary>

Un seul : la première ligne de `ManualDemo`, qui devient `new LoggingUserRepository("[USERS]")`. Les services n'ont pas bougé : ils dépendent d'un contrat, pas d'une implémentation. C'est le **D** de SOLID (inversion des dépendances). Même chose pour l'évolution 3 : un changement de constructeur ne touche que l'endroit qui fait le `new`.

</details>

### Ce qu'il faut retenir

Le code couplé fonctionnait : le problème n'est jamais « ça ne marche pas », mais « c'est rigide et coûteux à faire évoluer ».

| Problème du couplage fort | Dans l'expérience | Dans la vraie vie |
|---|---|---|
| **Données incohérentes** : chaque classe décide qu'elle aura **son propre** objet | deux maps d'utilisateurs, Alice « introuvable » | un pool de connexions à la base créé par chaque service, qui finit par épuiser les connexions acceptées ; un limiteur « 100 appels par minute » dupliqué, donc une limite réelle multipliée |
| **Changer d'implémentation = modifier chaque service** | évolution 2 | migrer d'Oracle vers PostgreSQL, ou changer de fournisseur cloud : des centaines de modifications dispersées, autant de risques de régression |
| **Effet cascade** | évolution 3 | une bibliothèque dont un constructeur change bloque toute l'équipe tant que chaque appel n'est pas corrigé |
| **Deux métiers dans une classe** | `UserService` a changé pour des raisons sans rapport avec les utilisateurs | contraire au **S** de SOLID : une classe, une seule raison de changer |

Un cinquième problème, la **testabilité**, sera vu au TP 8.

`ManualDemo` résout tout, mais sur un projet de 50 classes, quelqu'un doit écrire tous ces `new`, dans le bon ordre. C'est ce travail qu'on confie à Spring dans la partie 2.

**Supprimez le package `fr.cours.troc.sandbox`** (si vous voulez réutiliser `User` ou `Item`, déplacez-les d'abord dans leur package définitif avec **Refactor > Move**).

---

## Partie 2 — Mise en place : le conteneur Spring (obligatoire)

### Objectifs

À la fin de ce TP, le projet contient :

| Package | Classe | Rôle |
|---|---|---|
| `fr.cours.troc.user` | `User` | l'entité : `id` (`Long`), `name` (`String`) |
| | `UserRepository` | interface : `save`, `findAll`, `findById` |
| | `InMemoryUserRepository` | implémentation en mémoire, **bean** Spring |
| | `UserService` | règles métier, **bean**, reçoit un `UserRepository` |
| | `UserController` | **bean** web : `GET /users` et `POST /users` |
| `fr.cours.troc.item` | `Item` | l'entité : `id`, `name`, `quantity` (`int`), `ownerId` (`Long`) |
| | `ItemRepository`, `InMemoryItemRepository` | comme pour les utilisateurs : `save`, `findAll` |
| | `ItemService` | **bean** ; `create` refuse un objet dont le propriétaire n'existe pas (`IllegalArgumentException`) |

Contraintes :

- aucun `new` d'un repository ou d'un service dans le code : c'est Spring qui les crée ;
- chaque service ne connaît que les **interfaces** des repositories ;
- `UserService` et `ItemService` partagent le même stockage d'utilisateurs.

> Le modèle `Item` (un nom en texte libre, une quantité, un propriétaire) est volontairement simple et **pas idéal**. Il suffit pour les TP ; libre à votre groupe de l'améliorer quand vous le souhaitez.

Le code est écrit en anglais (noms de classes, de méthodes, de champs), comme dans la plupart des projets professionnels. Tout va dans des packages **sous** `fr.cours.troc`.

### Syntaxes utiles

#### Déclarer un bean

Un **bean** est un objet dont Spring s'occupe : il le crée et le passe aux classes qui en ont besoin. On désigne les classes concernées par une annotation, qui indique aussi la couche :

| Annotation | Package | Couche |
|---|---|---|
| `@Repository` | `org.springframework.stereotype` | accès aux données |
| `@Service` | `org.springframework.stereotype` | métier |
| `@RestController` | `org.springframework.web.bind.annotation` | web, répond en JSON |
| `@Component` | `org.springframework.stereotype` | bean sans couche particulière |

On annote les **classes**, jamais les interfaces.

#### Recevoir un bean : l'injection par constructeur

```java
@Service
public class UneClasse {

    private final UneDependance dependance; // final : fixée une fois pour toutes

    public UneClasse(UneDependance dependance) { // Spring passe le bean correspondant
        this.dependance = dependance;
    }
}
```

Quand une classe n'a **qu'un seul** constructeur, Spring l'utilise automatiquement. Vous croiserez souvent `@Autowired` dans les tutoriels : il n'est pas nécessaire ici. Le type du paramètre doit être l'**interface** : Spring cherche le bean qui l'implémente.

#### Exposer des routes HTTP

```java
@RestController
public class UnController {

    @GetMapping("/chemin")                       // GET /chemin
    public List<Truc> lister() { … }             // l'objet renvoyé est converti en JSON

    @PostMapping("/chemin")                      // POST /chemin
    public Truc creer(@RequestBody Truc truc) { … } // le JSON reçu est converti en objet
}
```

La conversion objet ↔ JSON est faite par la bibliothèque Jackson, incluse avec Spring Web : elle utilise les getters pour écrire le JSON, et les setters pour le lire (`"name"` ↔ `getName()` / `setName()`).

#### Ce que fait Spring au démarrage

Lancez `TrocApplication` (flèche verte, ou `./mvnw spring-boot:run`, `mvnw.cmd spring-boot:run` sous Windows). Spring :

1. **scanne** le package de `TrocApplication` et ses sous-packages, à la recherche des classes annotées ;
2. **crée** une instance de chaque bean, en commençant par ceux dont les autres ont besoin ;
3. **injecte** chaque bean dans les constructeurs qui le demandent.

C'est pour l'étape 1 que toutes vos classes doivent être **sous** `fr.cours.troc` : une classe placée ailleurs n'est jamais trouvée.

### Étapes conseillées

1. Les entités, les interfaces de repository et leurs implémentations en mémoire (inspirez-vous d'`InMemoryUserRepository` de la partie 1 si besoin : une `Map` et un compteur `AtomicLong`).
2. Les services, avec injection par constructeur.
3. Lancez l'application : elle doit démarrer (`Started TrocApplication`), même sans contrôleur.
4. `UserController`, avec `GET /users` et `POST /users`.

### Vérifier

#### Avec Bruno

1. Ouvrez Bruno > **Open Collection** > le dossier `bruno/` du projet, puis choisissez l'environnement **Local** en haut à droite.
2. **Utilisateurs > Créer utilisateur** : `POST /users` avec le corps `{"name": "Alice"}` et l'en-tête `Content-Type: application/json`. Réponse : `{"id": 1, "name": "Alice"}`, l'`id` attribué par le repository.
3. **Utilisateurs > Lister utilisateurs** : `GET /users` renvoie Alice.

Sans Bruno :

```bash
curl -X POST http://localhost:8080/users -H "Content-Type: application/json" -d '{"name":"Alice"}'
curl http://localhost:8080/users
```

(sous Windows PowerShell, tapez `curl.exe`). Pour un `GET`, un navigateur suffit aussi : <http://localhost:8080/users>.

Les données sont en mémoire : elles disparaissent à chaque redémarrage. La base de données arrive au TP 5.

#### Provoquer une erreur, pour lire Spring

Retirez temporairement `@Repository` d'`InMemoryUserRepository` et relancez. L'application refuse de démarrer :

```
Parameter 1 of constructor in fr.cours.troc.item.ItemService required a bean of type
'fr.cours.troc.user.UserRepository' that could not be found.
```

Spring dit précisément ce qui manque : `ItemService` demande un `UserRepository` (son deuxième paramètre, numéroté 1), et aucun bean ne correspond. Selon l'ordre de création, le message peut désigner `UserService` : c'est le même problème. Remettez l'annotation.

#### Liste de vérification

- [ ] L'application démarre, et `GET` / `POST /users` répondent.
- [ ] Aucun `new` d'un repository ou d'un service dans `fr.cours.troc` (cherchez avec `Ctrl+Shift+F`).
- [ ] Les services ne mentionnent que les interfaces `UserRepository` et `ItemRepository`.
- [ ] Le package `sandbox` a été supprimé.
- [ ] Question pour le checkpoint : si le client redemandait l'évolution 2 (un stockage avec traces), quels fichiers faudrait-il modifier ?

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `required a bean of type ... that could not be found` | Annotation oubliée sur la classe, ou classe placée **en dehors** de `fr.cours.troc`. |
| `required a single bean, but 2 were found` | Deux classes annotées implémentent la même interface : Spring ne sait pas laquelle injecter. |
| `415 Unsupported Media Type` sur le `POST` | En-tête `Content-Type: application/json` manquant. |
| `Port 8080 was already in use` | Une instance précédente tourne encore : arrêtez-la (bouton stop rouge d'IntelliJ). |
| `UnsupportedClassVersionError` au lancement | JDK trop ancien : **File > Project Structure > Project > SDK** doit être un JDK 25. |
| Le `POST` renvoie `"name": null` | Clé JSON mal orthographiée (`"Name"`, `"nom"`…), ou setter manquant. |
| Un champ manque dans la réponse JSON | Getter manquant. |

## Bonus

### Bonus 1 — Choisir l'implémentation selon l'environnement (profils Spring)

Créez une deuxième implémentation, `LoggingUserRepository`, qui affiche une trace à chaque opération. Plutôt que de déplacer `@Repository` d'une classe à l'autre, laissez Spring choisir au démarrage selon un **profil** actif :

- `@Profile("dev")` sur `LoggingUserRepository`, `@Profile("!dev")` sur `InMemoryUserRepository` (`!dev` : tout profil sauf `dev`), et `@Repository` sur les deux ;
- activez le profil dans **Run > Edit Configurations…** > `TrocApplication` > **Active profiles** : `dev`.

Les traces apparaissent sans qu'aucune classe n'ait été modifiée. Documentation : [Spring Boot — Profiles](https://docs.spring.io/spring-boot/reference/features/profiles.html).

### Bonus 2 — Les règles des objets

- Ajoutez une méthode `findByOwner(Long ownerId)` à `ItemService` et au repository.
- Dans `ItemService.create`, refusez une quantité nulle ou négative (`IllegalArgumentException`).

Ces règles seront exposées en HTTP au TP 2.

## Pour aller plus loin

- [Spring Framework — The IoC Container](https://docs.spring.io/spring-framework/reference/core/beans.html) (référence complète, à parcourir)
- [Spring — Guide « Building a RESTful Web Service »](https://spring.io/guides/gs/rest-service)
- [Spring Boot — Structuring Your Code](https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html) (pourquoi la classe principale doit être dans le package racine)
- [Wikipédia — Couplage (informatique)](https://fr.wikipedia.org/wiki/Couplage_(informatique)), [SOLID](https://fr.wikipedia.org/wiki/SOLID_(informatique)) et [Inversion de contrôle](https://fr.wikipedia.org/wiki/Inversion_de_contr%C3%B4le)
