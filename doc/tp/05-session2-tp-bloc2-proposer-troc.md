# Session 2 — TP bloc 2 : proposer un troc

## Objectif

Les utilisateurs et leurs objets existent ; il est temps de les échanger. Un **troc** (`Trade`) se déroule en plusieurs étapes : un utilisateur le propose, l'autre l'accepte ou le refuse, puis les objets changent de main. Dans ce bloc, on ne code que **la première étape : la proposition**.

```
POST /trades
{
  "proposerId": 1,          ← Alice propose…
  "offeredItemId": 1,       ← …3 de ses tomates (objet 1)…
  "offeredQuantity": 3,
  "recipientId": 2,         ← …à Bob…
  "requestedItemId": 2,     ← …contre 4 de ses carottes (objet 2)
  "requestedQuantity": 4
}
```

**Objectif obligatoire de fin de bloc** :

| Verbe | Chemin | Rôle | Code si tout va bien |
|---|---|---|---|
| `POST` | `/trades` | proposer un troc | `201` |
| `GET` | `/trades/{id}` | consulter un troc | `200` (`404` si inconnu) |
| `GET` | `/trades` (option `?userId=…`) | lister les trocs, tous ou ceux d'un utilisateur | `200` |

…avec les règles métier vérifiées par un `TradeService`, et le nombre maximum de trocs en attente lu depuis la configuration.

**Ce qui n'est pas au programme** : l'acceptation par le destinataire et l'échange effectif des quantités. Ces deux opérations doivent réussir ou échouer **ensemble**, ce qui demande une base de données et des transactions : c'est la séance 3. Ici, une proposition ne modifie aucun stock.

**Point de départ** : le bloc 1 terminé (CRUD `/users` et `/items`, `ItemProperties`).

Tout le code de ce bloc va dans un nouveau package `fr.cours.troc.trade`.

## Étape 1 — L'entité `Trade` et son statut

### 1.1 — Le statut : une `enum`

Un troc passe par plusieurs états. Une [`enum`](https://dev.java/learn/classes-objects/enums/) liste les valeurs possibles, et le compilateur refuse toute autre valeur (contrairement à une `String`, où `"PROPSED"` passerait sans erreur) :

```java
public enum TradeStatus {
    PROPOSED // la suite (acceptation, exécution…) en séance 3
}
```

Jackson convertit une `enum` en JSON sous forme de texte : `"status": "PROPOSED"`.

### 1.2 — L'entité

Créez la classe `Trade`, un POJO comme `User` et `Item`, avec ses getters/setters (`Alt+Insert` > **Getter and Setter**) :

| Champ | Type | Rôle |
|---|---|---|
| `id` | `Long` | attribué par le repository |
| `status` | `TradeStatus` | attribué par le service : `PROPOSED` |
| `proposerId` | `Long` | l'utilisateur qui propose |
| `offeredItemId` | `Long` | l'objet qu'il offre |
| `offeredQuantity` | `int` | combien il en offre |
| `recipientId` | `Long` | l'utilisateur à qui le troc est proposé |
| `requestedItemId` | `Long` | l'objet demandé en échange |
| `requestedQuantity` | `int` | combien il en demande |

## Étape 2 — Ce que le client envoie ≠ ce que le serveur garde

Pour `POST /users`, le contrôleur recevait directement un `User`. Pour un troc, ce serait dangereux : un client pourrait envoyer `"status": "EXECUTED"` (quand ce statut existera) et sauter toutes les règles métier, ou choisir lui-même l'`id`.

On sépare donc :

- **la requête** : ce que le client a le droit de choisir ;
- **l'entité** : ce que le serveur enregistre, construite par le service **après** ses vérifications.

Créez la requête sous forme de **record** (vu au bloc 1 avec `ItemProperties`) :

```java
// Ce que le client envoie pour proposer un troc : ni id ni statut.
public record ProposeTradeRequest(
        Long proposerId,
        Long offeredItemId,
        int offeredQuantity,
        Long recipientId,
        Long requestedItemId,
        int requestedQuantity
) {
}
```

Un record est idéal pour une requête : court, immuable (personne ne peut modifier la requête une fois reçue), et Jackson sait le remplir à partir du JSON en appelant son constructeur. On lit ses valeurs avec les accesseurs générés : `request.proposerId()`, `request.offeredQuantity()`…

## Étape 3 — Le repository

Sur le modèle d'`InMemoryItemRepository`, créez l'interface `TradeRepository` et son implémentation `InMemoryTradeRepository` (annotée `@Repository`) :

| Méthode | Rôle |
|---|---|
| `Trade save(Trade trade)` | attribue un `id` si besoin, enregistre |
| `List<Trade> findAll()` | tous les trocs |
| `Optional<Trade> findById(Long id)` | un troc, ou vide |
| `List<Trade> findByProposerIdOrRecipientId(Long proposerId, Long recipientId)` | les trocs où l'utilisateur est proposeur **ou** destinataire |
| `long countByProposerIdAndStatus(Long proposerId, TradeStatus status)` | le nombre de trocs d'un proposeur dans un statut donné |

Les noms sont longs, mais ils décrivent exactement la requête : en séance 3, Spring Data saura les traduire en SQL tout seul.

Deux `enum` se comparent avec `==` (chaque valeur n'existe qu'en un seul exemplaire) : `trade.getStatus() == status`.

<details>
<summary>Indice : les deux méthodes de recherche</summary>

```java
@Override
public List<Trade> findByProposerIdOrRecipientId(Long proposerId, Long recipientId) {
    return trades.values().stream()
            .filter(trade -> Objects.equals(trade.getProposerId(), proposerId)
                    || Objects.equals(trade.getRecipientId(), recipientId))
            .toList();
}

@Override
public long countByProposerIdAndStatus(Long proposerId, TradeStatus status) {
    return trades.values().stream()
            .filter(trade -> Objects.equals(trade.getProposerId(), proposerId))
            .filter(trade -> trade.getStatus() == status)
            .count();
}
```

</details>

## Étape 4 — La règle « trocs en attente » dans la configuration

Le client fixe une deuxième règle : **un utilisateur ne peut pas avoir plus de 3 trocs proposés en attente**. Même démarche qu'au bloc 1, étape 6.4 :

1. Dans `application.properties`, sous `troc.item.max-per-user` :

   ```properties
   troc.trade.max-pending=3
   ```

2. Dans `fr.cours.troc.trade`, un record `TradeProperties` annoté `@ConfigurationProperties("troc.trade")`, avec un composant `maxPending` et une valeur par défaut de `3`.

`@ConfigurationPropertiesScan` est déjà sur `TrocApplication` : rien d'autre à faire, `TradeProperties` sera détecté.

## Étape 5 — Le service : les règles d'une proposition

`TradeService` est le premier service qui a besoin des **autres fonctionnalités** : il doit retrouver des utilisateurs et des objets. Il les reçoit par son constructeur, comme toujours :

```java
@Service
public class TradeService {

    private final TradeRepository trades;
    private final UserService userService;
    private final ItemService itemService;
    private final TradeProperties props;

    public TradeService(TradeRepository trades, UserService userService, ItemService itemService,
                        TradeProperties props) {
        this.trades = trades;
        this.userService = userService;
        this.itemService = itemService;
        this.props = props;
    }
}
```

Il dépend des **services** et non des repositories des autres fonctionnalités : il profite ainsi de leurs règles, en particulier du `404` déjà codé dans `UserService.findById` et `ItemService.findById`.

### 5.1 — Consulter et lister

Ajoutez `findById(Long id)` (`404` si le troc n'existe pas) et `findAll(Long userId)` (tous les trocs si `userId` est `null`, sinon `findByProposerIdOrRecipientId(userId, userId)`), sur le modèle d'`ItemService`.

### 5.2 — Proposer : `propose(ProposeTradeRequest request)`

La méthode vérifie les règles suivantes, **dans cet ordre**, puis construit et enregistre le troc :

| # | Règle | Code si elle n'est pas respectée |
|---|---|---|
| 1 | le proposeur et le destinataire existent | `404` |
| 2 | l'objet offert et l'objet demandé existent | `404` |
| 3 | le proposeur et le destinataire sont deux utilisateurs différents | `400` |
| 4 | l'objet offert appartient au proposeur, l'objet demandé au destinataire | `400` |
| 5 | les deux quantités sont positives | `400` |
| 6 | les stocks sont suffisants (on n'offre pas 3 tomates si on n'en a que 2) | **à vous de choisir** |
| 7 | le proposeur a moins de `max-pending` trocs au statut `PROPOSED` | `409` |

Les règles 1 et 2 ne demandent presque rien : appeler `userService.findById(...)` et `itemService.findById(...)` suffit, l'exception `404` est levée pour vous.

**Règle 6** : quel code renvoyer ? `400`, `409`, ou `422 Unprocessable Content` ? Il n'y a pas une seule bonne réponse : choisissez, et notez **pourquoi** dans un commentaire. Ce genre de choix technique justifié fait partie de l'évaluation du projet. Rappel de la règle du bloc 1 : `400` si la requête est invalide en elle-même, `409` si elle est valide mais que l'état actuel des données l'empêche.

Si toutes les règles passent, construisez le troc :

```java
Trade trade = new Trade();
trade.setStatus(TradeStatus.PROPOSED); // c'est le serveur qui décide du statut
trade.setProposerId(request.proposerId());
// ... recopier les autres champs de la requête
return trades.save(trade);
```

<details>
<summary>Solution : propose</summary>

```java
public Trade propose(ProposeTradeRequest request) {
    // 404 : utilisateurs et objets existants (findById lève l'erreur)
    userService.findById(request.proposerId());
    userService.findById(request.recipientId());
    Item offered = itemService.findById(request.offeredItemId());
    Item requested = itemService.findById(request.requestedItemId());

    // 400 : la requête est incohérente en elle-même
    if (Objects.equals(request.proposerId(), request.recipientId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de troquer avec soi-même");
    }
    if (!Objects.equals(offered.getOwnerId(), request.proposerId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "L'objet " + offered.getId() + " n'appartient pas à l'utilisateur " + request.proposerId());
    }
    if (!Objects.equals(requested.getOwnerId(), request.recipientId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "L'objet " + requested.getId() + " n'appartient pas à l'utilisateur " + request.recipientId());
    }
    if (request.offeredQuantity() <= 0 || request.requestedQuantity() <= 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les quantités doivent être positives");
    }

    // 409 : la requête est valide, mais l'état actuel des données l'empêche
    if (request.offeredQuantity() > offered.getQuantity()
            || request.requestedQuantity() > requested.getQuantity()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuffisant");
    }
    if (trades.countByProposerIdAndStatus(request.proposerId(), TradeStatus.PROPOSED) >= props.maxPending()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "L'utilisateur " + request.proposerId() + " a déjà " + props.maxPending() + " trocs en attente");
    }

    Trade trade = new Trade();
    trade.setStatus(TradeStatus.PROPOSED);
    trade.setProposerId(request.proposerId());
    trade.setOfferedItemId(request.offeredItemId());
    trade.setOfferedQuantity(request.offeredQuantity());
    trade.setRecipientId(request.recipientId());
    trade.setRequestedItemId(request.requestedItemId());
    trade.setRequestedQuantity(request.requestedQuantity());
    return trades.save(trade);
}
```

Choix fait ici pour le stock : `409`. La même requête serait acceptée si Alice avait plus de tomates : c'est l'état des données qui bloque, pas la requête. `400` se défend aussi (« on ne peut pas offrir ce qu'on n'a pas »), tout comme `422`. L'important est que le choix soit cohérent et expliqué.

</details>

## Étape 6 — Le contrôleur

Créez `TradeController`, avec `@RequestMapping("/trades")` et trois méthodes :

- `GET /trades`, avec un `@RequestParam(required = false) Long userId` ;
- `GET /trades/{id}` ;
- `POST /trades`, qui reçoit un `@RequestBody ProposeTradeRequest` et renvoie `201 Created` avec l'en-tête `Location: /trades/{id}`.

Tout ce qu'il faut a été vu au bloc 1 : n'hésitez pas à repartir d'`ItemController`.

Remarquez que « proposer un troc » est une **action métier**, et pourtant l'URL ne contient pas de verbe (`/proposeTrade`) : proposer un troc, c'est **créer** une ressource troc, donc `POST /trades`.

## Étape 7 — Tester

### 7.1 — Le cas qui marche

Les données sont en mémoire : après chaque redémarrage, recréez-les. Dans Bruno, exécutez dans l'ordre :

1. **Utilisateurs > Créer utilisateur** (Alice, id 1) et **Créer un 2e utilisateur** (Bob, id 2) ;
2. **Objets > Créer objet (Alice)** (10 tomates, id 1) et **Créer objet (Bob)** (5 carottes, id 2) ;
3. **Trocs > Proposer troc** : `201 Created`, avec `"status": "PROPOSED"` et un `id` attribué par le serveur ;
4. **Trocs > Consulter troc** (`GET /trades/1`) et **Lister trocs d'un utilisateur** (`GET /trades?userId=2`) : le troc apparaît aussi pour Bob, qui en est le destinataire.

Vérifiez avec **Objets > Lister objets** que les quantités n'ont **pas** bougé : une proposition n'échange rien.

### 7.2 — Chaque règle, chaque code

Modifiez le corps de **Proposer troc** pour provoquer chaque erreur du tableau de l'étape 5.2, et vérifiez le code et le message renvoyés. Par exemple :

| Modification du corps | Code attendu |
|---|---|
| `"recipientId": 42` | `404` |
| `"recipientId": 1` | `400` (soi-même) |
| `"offeredItemId": 2` | `400` (l'objet 2 est à Bob) |
| `"offeredQuantity": 0` | `400` |
| `"requestedQuantity": 50` | votre choix pour le stock |

Essayez aussi d'envoyer `"status": "EXECUTED"` ou `"id": 99` dans le corps : ces clés n'existent pas dans `ProposeTradeRequest`, elles sont simplement ignorées. Le troc créé est bien `PROPOSED`, avec un `id` choisi par le serveur.

### 7.3 — Trop de trocs en attente : changer la règle sans toucher au code

Avec `max-pending=3`, il faut proposer quatre trocs pour voir l'erreur. Plus rapide : abaissez la limite **pour ce lancement seulement**, avec une variable d'environnement (voir le bloc 1, étape 6.5).

1. **Run > Edit Configurations…** > `TrocApplication` > **Environment variables** : `TROC_TRADE_MAXPENDING=1`.
2. Relancez, recréez les utilisateurs et les objets.
3. Exécutez **Proposer troc** une première fois : `201`. Une deuxième fois : `409`, « L'utilisateur 1 a déjà 1 trocs en attente ».

Le code et `application.properties` n'ont pas changé : seule la source de configuration la plus prioritaire a parlé. Retirez la variable avant de continuer.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `The dependencies of some of the beans in the application context form a cycle` | Deux services dépendent l'un de l'autre (par exemple `ItemService` qui reçoit `TradeService`, et l'inverse) : Spring ne sait pas lequel créer en premier. Seul `TradeService` doit dépendre des autres. |
| `400` avec `Cannot map null into type int` | Une quantité manque dans le JSON, ou sa clé est mal nommée (`"quantity"` au lieu de `"offeredQuantity"`). Dans un record, un `int` ne peut pas rester vide : Jackson refuse la requête. Un `Long` absent, lui, vaut `null`. |
| `500 Internal Server Error` sur `POST /trades` | Une exception non prévue, souvent une `NullPointerException` : un champ absent du JSON (`null`) utilisé sans vérification, ou deux `Long` comparés avec `==`. Lisez la trace dans la console. Un `500` n'est jamais un choix volontaire : c'est un bug à corriger. |
| `"status"` absent de la réponse | Getter `getStatus()` manquant dans `Trade`. |
| La limite `max-pending` est ignorée | La variable d'environnement est mal nommée (`TROC_TRADE_MAX_PENDING` au lieu de `TROC_TRADE_MAXPENDING`), ou le compteur ne filtre pas sur le statut `PROPOSED`. |
| `404` sur `/trades` | `TradeController` est placé en dehors de `fr.cours.troc`, ou il manque `@RestController`. |

## En avance ? Features bonus

Si ce n'est pas déjà fait, commencez par les bonus du bloc 1 (`@RestControllerAdvice`, puis `@Valid`, qui s'applique aussi à `ProposeTradeRequest` : les annotations de validation se placent directement sur les composants du record).

### Bonus 3 — Refuser les doublons

Alice peut proposer dix fois exactement le même troc à Bob. Refusez une proposition si un troc `PROPOSED` identique (même proposeur, même destinataire, mêmes objets) existe déjà. Quel code renvoyer ? Justifiez.

### Bonus 4 — Ne pas promettre deux fois le même stock

Alice a 10 tomates. Elle propose 8 tomates à Bob, puis 8 tomates à Charlie : chaque proposition, prise seule, est valide, mais elle ne pourra pas honorer les deux. Faites en sorte que la règle 6 tienne compte des quantités déjà offertes dans les trocs `PROPOSED` d'Alice sur le même objet.

Cette question (deux opérations qui se disputent le même stock) reviendra sous une forme plus difficile en séance 3, quand deux trocs seront **exécutés** en même temps.

## Pour aller plus loin

- [dev.java — Records](https://dev.java/learn/records/) et [dev.java — Enums](https://dev.java/learn/classes-objects/enums/)
- [Martin Fowler — Data Transfer Object](https://martinfowler.com/eaaCatalog/dataTransferObject.html) (le nom habituel de ce qu'on a fait avec `ProposeTradeRequest` : un objet dédié à l'échange avec l'extérieur)
- [MDN — 409 Conflict](https://developer.mozilla.org/fr/docs/Web/HTTP/Status/409) et [MDN — 422 Unprocessable Content](https://developer.mozilla.org/fr/docs/Web/HTTP/Status/422)
- [Microsoft — Conception d'API web RESTful](https://learn.microsoft.com/fr-fr/azure/architecture/best-practices/api-design) (bonnes pratiques de nommage des ressources et des codes de statut)
