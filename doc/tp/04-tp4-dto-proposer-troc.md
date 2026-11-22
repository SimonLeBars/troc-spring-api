# TP 4 — Objets de requête (DTO) et règles métier : proposer un troc

> Rythme indicatif : séance 2. Point de départ : la mise en place du TP 3 terminée (CRUD `/users` et `/items`, `ItemProperties`).

## Le concept en bref

Jusqu'ici, les contrôleurs recevaient directement les entités (`@RequestBody User user`) : tout ce que le client envoyait arrivait tel quel dans l'objet enregistré. Pour un utilisateur, ce n'est pas grave. Pour un troc, qui a un **statut** que seules les règles métier doivent faire évoluer, ce serait laisser le client sauter ces règles.

On sépare donc :

- **l'objet de requête** (souvent appelé *DTO*, *Data Transfer Object*) : ce que le client a le **droit** de choisir, et rien d'autre ;
- **l'entité** : ce que le serveur enregistre, construite par le **service**, après ses vérifications.

Ce TP applique cette séparation à la première étape d'un troc : la **proposition**.

---

## Partie 1 — Expérimentation : le client décide du statut (facultative)

**Vous pouvez passer à la partie 2 si vous savez répondre à ces questions :**

- Que risque-t-on à recevoir directement une entité dans un `@RequestBody` ?
- Que fait Jackson d'une clé JSON qui n'existe pas dans la classe cible ?
- Pourquoi une `enum` plutôt qu'une `String` pour un statut ?

Le code temporaire va dans le package `fr.cours.troc.sandbox.dto`.

### Étape 1 — Une entité reçue telle quelle

```java
package fr.cours.troc.sandbox.dto;

// Une commande de démonstration : son statut ne devrait changer qu'après paiement.
public class NaiveOrder {
    private Long id;
    private String product;
    private String status; // "PENDING", puis "PAID" après paiement
    // getters et setters (Alt+Insert)
}
```

```java
@RestController
public class NaiveOrderController {

    private final List<NaiveOrder> orders = new ArrayList<>();

    @PostMapping("/sandbox/orders")
    public NaiveOrder create(@RequestBody NaiveOrder order) {
        orders.add(order); // le client a tout choisi
        return order;
    }
}
```

Exécutez **Expérimentation > TP4 - Commande naïve** : `POST /sandbox/orders` avec

```json
{ "id": 999, "product": "vélo", "status": "PAID" }
```

La commande est enregistrée **payée**, avec l'`id` choisi par le client. Essayez aussi `"status": "PAYED"` (faute de frappe) : acceptée sans broncher.

### Étape 2 — Un objet de requête

Créez un record qui ne contient **que** ce que le client a le droit de choisir :

```java
public record CreateOrderRequest(String product) {
}
```

Modifiez la route pour recevoir un `CreateOrderRequest`, et construire elle-même la commande : `id` attribué par le serveur, statut `PENDING`. Renvoyez le même corps JSON.

<details>
<summary>Ce que vous devriez observer</summary>

Les clés `id` et `status` sont **ignorées** : elles n'existent pas dans `CreateOrderRequest`, Jackson ne sait pas où les mettre. La commande est `PENDING`, quoi que le client envoie. Le client ne peut plus que demander ; c'est le serveur qui décide.

</details>

### Ce qu'il faut retenir

- Recevoir une entité dans un `@RequestBody`, c'est laisser le client écrire **tous** ses champs, y compris ceux qui portent des règles (statut, identifiant, propriétaire…). C'est une faille classique, appelée *mass assignment*.
- L'objet de requête documente aussi le contrat de la route : ses champs sont exactement ce que le client doit envoyer.
- Une `enum` limite un statut aux valeurs prévues : `"PAYED"` serait refusé (`400`) au lieu d'être enregistré.

**Supprimez le package `fr.cours.troc.sandbox`.**

---

## Partie 2 — Mise en place : proposer un troc (obligatoire)

Un **troc** (`Trade`) se déroule en plusieurs étapes : un utilisateur le propose, l'autre l'accepte ou le refuse, puis les objets changent de main. Ce TP ne code que **la proposition** :

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

L'acceptation et l'échange effectif des quantités doivent réussir ou échouer **ensemble** : c'est le TP 6 (transactions). Ici, une proposition ne modifie **aucun** stock.

Tout le code de ce TP va dans un nouveau package `fr.cours.troc.trade`.

### Objectifs

| Verbe | Chemin | Rôle | Code si tout va bien | Codes d'erreur |
|---|---|---|---|---|
| `POST` | `/trades` | proposer un troc | `201` + `Location` | voir les règles |
| `GET` | `/trades/{id}` | consulter un troc | `200` | `404` |
| `GET` | `/trades` (option `?userId=…`) | lister les trocs, tous ou ceux où l'utilisateur est proposeur **ou** destinataire | `200` | |

Les classes attendues :

| Classe | Rôle |
|---|---|
| `TradeStatus` | une `enum`, avec pour l'instant une seule valeur : `PROPOSED` |
| `Trade` | l'entité : `id`, `status`, `proposerId`, `offeredItemId`, `offeredQuantity`, `recipientId`, `requestedItemId`, `requestedQuantity` |
| `ProposeTradeRequest` | l'objet de requête : ce que le client a le droit de choisir (ni `id`, ni `status`) |
| `TradeRepository`, `InMemoryTradeRepository` | le stockage, sur le modèle des objets |
| `TradeProperties` | les réglages `troc.trade.*` |
| `TradeService`, `TradeController` | |

Les règles de `propose`, vérifiées **dans cet ordre** :

| # | Règle | Code si elle n'est pas respectée |
|---|---|---|
| 1 | le proposeur et le destinataire existent | `404` |
| 2 | l'objet offert et l'objet demandé existent | `404` |
| 3 | le proposeur et le destinataire sont deux utilisateurs différents | `400` |
| 4 | l'objet offert appartient au proposeur, l'objet demandé au destinataire | `400` |
| 5 | les deux quantités sont strictement positives | `400` |
| 6 | les stocks sont suffisants (on n'offre pas 3 tomates si on n'en a que 2) | **à vous de choisir** |
| 7 | le proposeur a moins de `troc.trade.max-pending` trocs au statut `PROPOSED` (valeur : `3`) | `409` |

**Règle 6** : `400`, `409`, ou `422 Unprocessable Content` ? Il n'y a pas une seule bonne réponse : choisissez, et notez **pourquoi** dans un commentaire. Rappel : `400` si la requête est invalide en elle-même, `409` si elle est valide mais que l'état actuel des données l'empêche.

### Syntaxes utiles

#### Une `enum`

```java
public enum TradeStatus {
    PROPOSED // la suite (acceptation…) au TP 6
}
```

Une [`enum`](https://dev.java/learn/classes-objects/enums/) liste les valeurs possibles ; le compilateur refuse toute autre valeur. Jackson l'écrit en JSON sous forme de texte (`"status": "PROPOSED"`). Deux valeurs d'`enum` se comparent avec `==` : chaque valeur n'existe qu'en un seul exemplaire.

#### Un objet de requête : un `record`

```java
public record UneRequete(Long unId, int uneQuantite) {
}
```

Un record est idéal pour une requête : court, immuable (personne ne la modifie une fois reçue), et Jackson sait le remplir à partir du JSON. On lit ses valeurs avec les accesseurs générés : `request.unId()`, `request.uneQuantite()`.

Dans un record, un composant `int` absent du JSON fait échouer la lecture (`400`) ; un `Long` absent vaut `null`.

#### Un service qui utilise les autres fonctionnalités

`TradeService` a besoin de retrouver des utilisateurs et des objets. Il les reçoit par son constructeur, comme toujours. Faites-le dépendre des **services** (`UserService`, `ItemService`) plutôt que de leurs repositories : il profite ainsi de leurs règles, en particulier du `404` de leurs méthodes `findById`. Les règles 1 et 2 ne demandent alors presque rien.

#### Des méthodes de recherche bien nommées

Le repository a besoin de deux recherches, nommées pour que Spring Data sache les écrire seul au TP 5 :

```java
List<Trade> findByProposerIdOrRecipientId(Long proposerId, Long recipientId);
long countByProposerIdAndStatus(Long proposerId, TradeStatus status);
```

En mémoire, un stream avec un ou plusieurs `filter` suffit (voir `findByOwnerId` au TP 2), terminé par `.toList()` ou `.count()`.

### Indices

- Remarquez que « proposer un troc » est une action métier, et pourtant l'URL ne contient pas de verbe (`/proposeTrade`) : proposer un troc, c'est **créer** une ressource troc, donc `POST /trades`.
- `TradeProperties` se construit exactement comme `ItemProperties` (TP 3), avec la clé `troc.trade.max-pending=3`.
- C'est le service qui construit l'entité `Trade`, à partir de la requête, **après** les vérifications, avec le statut `PROPOSED`.

### Vérifier

Les données sont en mémoire : après chaque redémarrage, recréez-les. Dans Bruno :

1. **Utilisateurs > Créer utilisateur** et **Créer un 2e utilisateur** (Alice 1, Bob 2) ;
2. **Objets > Créer objet (Alice)** et **Créer objet (Bob)** (10 tomates, 5 carottes) ;
3. **Trocs > Proposer troc** : `201`, `"status": "PROPOSED"` ;
4. **Trocs > Consulter troc** et **Lister trocs d'un utilisateur** (`?userId=2`) : le troc apparaît aussi pour Bob, le destinataire.

Puis modifiez le corps de **Proposer troc** pour provoquer chaque règle :

| Modification du corps | Code attendu |
|---|---|
| `"recipientId": 42` | `404` |
| `"recipientId": 1` | `400` (soi-même) |
| `"offeredItemId": 2` | `400` (l'objet 2 est à Bob) |
| `"offeredQuantity": 0` | `400` |
| `"requestedQuantity": 50` | votre choix pour le stock |
| `"status": "EXECUTED"`, `"id": 99` | `201`, troc `PROPOSED` avec un `id` choisi par le serveur |

- [ ] Toutes les lignes du tableau ci-dessus répondent comme attendu.
- [ ] Après une proposition, **Objets > Lister objets** montre des quantités inchangées.
- [ ] Avec la variable d'environnement `TROC_TRADE_MAXPENDING=1` (TP 3), la deuxième proposition d'Alice répond `409` ; retirez ensuite la variable.
- [ ] Le choix de code de la règle 6 est justifié dans un commentaire.
- [ ] `TradeController` reçoit un `ProposeTradeRequest`, jamais un `Trade`.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `The dependencies of some of the beans in the application context form a cycle` | Deux services dépendent l'un de l'autre (par exemple `ItemService` qui reçoit `TradeService`, et l'inverse) : Spring ne sait pas lequel créer en premier. Seul `TradeService` doit dépendre des autres. |
| `400` avec `Cannot map null into type int` | Une quantité manque dans le JSON, ou sa clé est mal nommée (`"quantity"` au lieu de `"offeredQuantity"`). |
| `500 Internal Server Error` sur `POST /trades` | Une exception non prévue, souvent une `NullPointerException` (un champ absent du JSON, `null`, utilisé sans vérification) ou deux `Long` comparés avec `==`. Lisez la trace dans la console : un `500` est toujours un bug. |
| `"status"` absent de la réponse | Getter `getStatus()` manquant dans `Trade`. |
| La limite `max-pending` est ignorée | Variable mal nommée (`TROC_TRADE_MAX_PENDING`), ou le compteur ne filtre pas sur le statut `PROPOSED`. |

## Bonus

### Bonus 1 — Refuser les doublons

Alice peut proposer dix fois exactement le même troc à Bob. Refusez une proposition si un troc `PROPOSED` identique (même proposeur, même destinataire, mêmes objets) existe déjà. Quel code renvoyer ? Justifiez.

### Bonus 2 — Ne pas promettre deux fois le même stock

Alice a 10 tomates. Elle propose 8 tomates à Bob, puis 8 tomates à Charlie : chaque proposition, prise seule, est valide, mais elle ne pourra pas honorer les deux. Faites en sorte que la règle 6 tienne compte des quantités déjà offertes dans les trocs `PROPOSED` d'Alice sur le même objet.

Cette question (deux opérations qui se disputent le même stock) reviendra sous une forme plus difficile au TP 6, quand deux trocs seront **exécutés** en même temps.

### Bonus 3 — Valider l'objet de requête

Si vous avez fait le bonus `@Valid` du TP 2, appliquez-le à `ProposeTradeRequest` : les annotations de validation se placent directement sur les composants du record (`@NotNull Long proposerId`, `@Positive int offeredQuantity`…). Quelles règles du tableau peuvent passer dans le record, et lesquelles doivent rester dans le service ?

## Pour aller plus loin

- [Martin Fowler — Data Transfer Object](https://martinfowler.com/eaaCatalog/dataTransferObject.html)
- [OWASP — Mass Assignment](https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html) (en anglais : la faille de l'expérimentation, et les parades)
- [dev.java — Records](https://dev.java/learn/records/) et [dev.java — Enums](https://dev.java/learn/classes-objects/enums/)
- [MDN — 409 Conflict](https://developer.mozilla.org/fr/docs/Web/HTTP/Status/409) et [MDN — 422 Unprocessable Content](https://developer.mozilla.org/fr/docs/Web/HTTP/Status/422)
