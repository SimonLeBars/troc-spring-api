# Session 1 — TP bloc 1 : codez-le mal, exprès

## Objectif

Avant de découvrir ce que Spring apporte, on va écrire du code **sans** Spring, en Java pur, avec un couplage fort entre les classes : chaque classe fabrique elle-même les objets dont elle a besoin, avec `new`.

Le but n'est pas d'écrire du « bon » code. Le but est de **ressentir** les problèmes de cette façon de faire : le client va vous demander trois évolutions, et pour chacune vous noterez combien de fichiers vous avez dû modifier. Ces chiffres seront mis en commun au checkpoint #1.

Rappel de l'architecture en couches vue en théorie : un **service** porte les règles métier, un **repository** stocke et retrouve les données. Le repository n'a pas besoin d'une vraie base de données : ici, une simple `Map` en mémoire suffit (la base H2 arrive en séance 3).

Tout le code de ce bloc va dans des packages **sous** `fr.cours.troc` (voir le support d'initialisation). Aucune annotation Spring dans ce bloc.

Le code est écrit en anglais (noms de classes, de méthodes, de champs), comme dans la plupart des projets professionnels.

## Étape 1 — L'entité `User`

Dans un nouveau package `fr.cours.troc.user`, créez une classe `User` : une classe Java classique (on parle de *POJO*, *Plain Old Java Object*), avec des champs privés et leurs getters/setters.

- `id` (`Long`)
- `name` (`String`)

Astuce IntelliJ : déclarez les deux champs, puis `Alt+Insert` (Windows/Linux) ou `Cmd+N` (macOS) > **Getter and Setter** pour générer les accesseurs.

## Étape 2 — Le repository en mémoire

Toujours dans `fr.cours.troc.user`, créez une classe `InMemoryUserRepository` qui simule un stockage avec une `Map<Long, User>` :

- `User save(User user)` : si l'utilisateur n'a pas encore d'`id`, lui en attribue un (1, 2, 3…), le stocke dans la map et le renvoie.
- `List<User> findAll()` : renvoie tous les utilisateurs stockés.
- `User findById(Long id)` : renvoie l'utilisateur correspondant, ou `null` s'il n'existe pas.

Pour générer les identifiants, un simple compteur suffit. Par exemple :

```java
private final Map<Long, User> users = new HashMap<>();
private final AtomicLong nextId = new AtomicLong(1);

public User save(User user) {
    if (user.getId() == null) {
        user.setId(nextId.getAndIncrement());
    }
    users.put(user.getId(), user);
    return user;
}
```

`AtomicLong` est un compteur fourni par Java (`java.util.concurrent.atomic`) : `getAndIncrement()` renvoie la valeur courante puis l'augmente de 1. Pour `findAll()`, `new ArrayList<>(users.values())` renvoie une liste des valeurs de la map.

## Étape 3 — Le service, couplé en dur

Créez `UserService` dans `fr.cours.troc.user`. C'est lui qui fabrique son repository :

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

## Étape 4 — Lancer le code sans Spring

Il n'y a encore ni Spring ni HTTP dans ce bloc : on lance notre code avec une simple méthode `main`. Créez une classe `ManualDemo` dans le package racine `fr.cours.troc` :

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

Lancez-la avec la flèche verte à gauche de `main` dans IntelliJ (**Run 'ManualDemo.main()'**). La console doit afficher :

```
1 Alice
2 Bob
```

Tout fonctionne. Passons aux demandes du client.

## Les évolutions demandées par le client

Pour chaque évolution, remplissez ce tableau (sur papier ou dans un fichier texte) : il servira au checkpoint #1.

| Évolution | Fichiers créés | Fichiers existants modifiés | Problème rencontré |
|---|---|---|---|
| 1 — Les objets | | | |
| 2 — Les traces | | | |
| 3 — Le préfixe | | | |

### Évolution 1 — Les utilisateurs déclarent leurs objets

> *Le client* : « Chaque utilisateur doit pouvoir déclarer les objets qu'il possède, avec leur quantité. Un objet appartient forcément à un utilisateur qui existe. »

Le type d'objets échangés est au choix de votre groupe : légumes, livres, jeux de société, outils… Les exemples de ce support utilisent des légumes ; adaptez-les à votre choix.

1. Dans un nouveau package `fr.cours.troc.item`, créez l'entité `Item` : `id` (`Long`), `name` (`String`), `quantity` (`int`), `ownerId` (`Long`, l'`id` de l'utilisateur propriétaire).
2. Créez `InMemoryItemRepository`, sur le même modèle que `InMemoryUserRepository`, avec `save` et `findAll`.
3. Créez `ItemService`. Pour vérifier que le propriétaire existe, il a besoin des utilisateurs : comme `UserService`, il fabrique ce dont il a besoin.

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

4. À la fin de `ManualDemo.main`, Alice déclare un objet (ici, des tomates) :

   ```java
   ItemService itemService = new ItemService();

   Item tomatoes = new Item();
   tomatoes.setName("tomates");
   tomatoes.setQuantity(10);
   tomatoes.setOwnerId(alice.getId());
   itemService.create(tomatoes);

   System.out.println(itemService.findAll().size() + " objet(s) déclaré(s)");
   ```

Relancez `ManualDemo`. Le programme s'arrête sur :

```
Exception in thread "main" java.lang.IllegalArgumentException: Utilisateur introuvable : 1
```

Pourtant Alice vient d'être créée, avec l'`id` 1. **Cherchez pourquoi** avant de lire la suite : combien d'objets `InMemoryUserRepository` existent dans ce programme ?

<details>
<summary>Explication</summary>

`UserService` et `ItemService` font chacun leur propre `new InMemoryUserRepository()` : il y a **deux** maps d'utilisateurs, qui s'ignorent. Alice est enregistrée dans celle de `UserService`, mais `ItemService` cherche dans la sienne, qui est vide.

</details>

Ne cherchez pas à corriger ce bug pour l'instant : notez-le dans le tableau, on le résoudra au bloc 2. Pour pouvoir continuer, mettez en commentaire l'appel `itemService.create(tomatoes);`.

### Évolution 2 — Tracer les opérations

> *Le client* : « Pendant la phase de mise au point, je veux voir dans la console chaque opération faite sur les utilisateurs. »

1. Dans `fr.cours.troc.user`, créez `LoggingUserRepository` : une copie d'`InMemoryUserRepository` (mêmes méthodes, même map), dont chaque méthode affiche aussi une ligne dans la console. Par exemple dans `save` :

   ```java
   System.out.println("[USERS] save " + user.getId() + " " + user.getName());
   ```

   Faites de même dans `findAll` et `findById`. Pour l'instant, la classe n'a pas de constructeur particulier.

2. Faites en sorte que **tous** les services utilisent ce nouveau stockage à la place d'`InMemoryUserRepository`.

Relancez `ManualDemo` : les lignes `[USERS]` doivent apparaître. Combien de fichiers avez-vous dû ouvrir pour le point 2 ? Imaginez la même demande sur un projet de 30 services.

### Évolution 3 — Choisir le préfixe des traces

> *Le client* : « Dans les traces, je veux savoir qui a fait l'appel : `[USERS]` quand c'est la gestion des utilisateurs, `[ITEMS]` quand c'est la vérification faite par la gestion des objets. »

Le préfixe ne peut plus être écrit en dur dans `LoggingUserRepository` : c'est à celui qui crée le repository de le choisir.

1. Ajoutez à `LoggingUserRepository` un champ `private final String prefix;` et un constructeur qui le reçoit :

   ```java
   public LoggingUserRepository(String prefix) {
       this.prefix = prefix;
   }
   ```

   Remplacez `"[USERS]"` par `prefix` dans les `System.out.println`.

2. Lancez la compilation (**Build > Build Project**, ou relancez `ManualDemo`). Que se passe-t-il ? Corrigez **chaque** erreur.

Le simple ajout d'un paramètre de constructeur a cassé tous les endroits qui faisaient `new LoggingUserRepository()`. C'est l'**effet cascade**.

## Bilan : pourquoi ça fait mal

Votre code fonctionne (à part le bug de l'évolution 1). Le problème n'est jamais « le code ne marche pas » : il est rigide et coûteux à faire évoluer. Les évolutions ont mis en évidence quatre conséquences du couplage fort.

**1. Données incohérentes**

Deux services, deux `new`, deux stockages qui s'ignorent : c'est le bug de l'évolution 1.

Attention, avoir plusieurs instances n'est pas un problème en soi. Une grosse application tourne souvent sur plusieurs serveurs, chacun avec ses propres objets, et tous accèdent aux mêmes données sans incohérence, parce que ces données vivent dans une base partagée. Ici, le vrai problème est ailleurs : avec `new`, c'est chaque service qui décide qu'il aura **son propre** repository, et personne ne peut lui imposer d'en partager un. Si le repository garde des données à lui (notre `Map` en mémoire), les copies divergent. S'il passe par une base partagée, les données restent cohérentes, mais les ressources sont dupliquées.

*Dans la vraie vie* :
- un repository qui parle à une base gère en général un pool de connexions, coûteux à créer. Si chaque service fabrique le sien, une même application ouvre autant de pools que de services, et peut épuiser les connexions que la base accepte, alors qu'un seul pool partagé aurait suffi ;
- un composant qui garde un état en mémoire, comme un limiteur de requêtes (« 100 appels par minute vers un fournisseur externe »), ne joue plus son rôle si chaque service en crée le sien : chacun compte de son côté, et la limite réelle est multipliée par le nombre de services.

**2. Changer d'implémentation = modifier chaque service**

Remplacer le stockage (évolution 2) oblige à modifier toutes les classes qui font `new InMemoryUserRepository()`. *Dans la vraie vie*, une migration de base de données (par exemple d'Oracle vers PostgreSQL) ou de fournisseur cloud impliquerait des dizaines, voire des centaines de modifications dispersées dans le code, avec un risque de régression à chaque endroit touché.

**3. Effet cascade**

Le constructeur du repository change (évolution 3) : plus rien ne compile, et il faut repasser partout. *Dans la vraie vie*, la mise à jour d'une bibliothèque dont un constructeur a changé peut bloquer toute une équipe tant que chaque appel n'a pas été corrigé, souvent dans des parties du code que personne n'a écrites récemment.

**4. Deux métiers dans une classe**

`UserService` gère les utilisateurs **et** fabrique ses outils. C'est contraire au **S** de SOLID (*Single Responsibility* : une classe, une seule raison de changer). Ici, `UserService` a dû changer pour des raisons qui n'avaient rien à voir avec les utilisateurs (évolutions 2 et 3).

Gardez votre tableau sous la main : au checkpoint #1, on compare les chiffres des groupes, puis on voit comment Spring règle ces quatre problèmes.

## En avance ?

- Ajoutez à `ItemService` une méthode `List<Item> findByOwner(Long ownerId)`, et affichez dans `ManualDemo` les objets d'Alice.
- Ajoutez une règle métier dans `ItemService.create` : la quantité doit être strictement positive, sinon `IllegalArgumentException`.
- Réfléchissez (sans le coder) : comment feriez-vous pour que `UserService` et `ItemService` partagent **le même** `InMemoryUserRepository` ? Notez votre idée, elle sera discutée au checkpoint.

## Pour aller plus loin

- [Wikipedia — Couplage (informatique)](https://fr.wikipedia.org/wiki/Couplage_(informatique))
- [Wikipedia — SOLID (informatique)](https://fr.wikipedia.org/wiki/SOLID_(informatique))
