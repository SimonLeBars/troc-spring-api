# Session 3 — TP bloc 2 : accepter un troc, tout ou rien

## Objectif

En séance 2, un troc s'arrêtait au statut `PROPOSED` : le proposeur avait donné son accord, rien n'était échangé. Dans ce bloc, le destinataire donne **le second accord**, ou refuse :

```
                       POST /trades/{id}/accept        ┌──────────┐
                     ┌───────────────────────────────▶ │ EXECUTED │  stocks échangés
POST /trades  ┌──────┴───┐  (le destinataire accepte)  └──────────┘
────────────▶ │ PROPOSED │
 (séance 2)   └──────┬───┘  (le destinataire refuse)   ┌──────────┐
                     └───────────────────────────────▶ │ REJECTED │  stocks intacts
                       POST /trades/{id}/reject        └──────────┘
```

L'échange a lieu **au moment de l'acceptation** : pas d'état intermédiaire « accepté mais pas encore échangé ». Accepter le troc d'Alice (3 tomates contre 4 carottes de Bob), c'est **cinq écritures** en base :

1. retirer 3 tomates à Alice ;
2. ajouter 3 tomates à Bob ;
3. retirer 4 carottes à Bob ;
4. ajouter 4 carottes à Alice ;
5. passer le troc au statut `EXECUTED`.

Si l'une d'elles échoue, les autres ne doivent pas rester à moitié faites : c'est le rôle d'une **transaction**.

**Objectif obligatoire de fin de bloc** :

| Verbe | Chemin | Corps | Rôle | Code si tout va bien | Codes d'erreur |
|---|---|---|---|---|---|
| `POST` | `/trades/{id}/reject` | `{"userId": 2}` | refuser un troc | `200` | `404` troc inconnu, `403` pas le destinataire, `409` déjà traité |
| `POST` | `/trades/{id}/accept` | `{"userId": 2}` | accepter et exécuter un troc | `200` | les mêmes, et `409` stock insuffisant |

…et l'**expérience** de l'étape 5 réalisée, avec ses observations notées : c'est la preuve concrète du rôle de la transaction, à reprendre dans le rapport du projet.

**Point de départ** : le bloc 1 terminé (les trois tables, plus de classe `InMemory…`).

## Étape 1 — Les nouveaux statuts

Complétez l'`enum` :

```java
public enum TradeStatus {
    PROPOSED, // accord du proposeur
    EXECUTED, // accord du destinataire : les stocks ont été échangés
    REJECTED  // refus du destinataire : les stocks sont intacts
}
```

Rien à changer dans la base : la colonne `status` est un `VARCHAR(20)`, elle accepte n'importe quel texte de 20 caractères au plus.

Remarquez un effet de bord bienvenu : la règle `max-pending` de la séance 2 compte les trocs au statut `PROPOSED`. Un troc accepté ou refusé ne compte plus : il libère une place pour son proposeur.

## Étape 2 — Refuser un troc

On commence par le plus simple : refuser, c'est **une seule** écriture (le statut).

### 2.1 — Qui répond ? Le corps de la requête

Seul le destinataire a le droit de répondre à un troc. Mais l'API n'a pas d'authentification : elle ne sait pas qui envoie la requête. Comme pour `proposerId` en séance 2, le client **se déclare** dans le corps :

```java
// Corps de POST /trades/{id}/accept et /reject : l'utilisateur qui répond au troc.
public record TradeDecisionRequest(Long userId) {
}
```

> Dans une vraie application, l'identité viendrait de l'authentification (un jeton, une session), jamais du corps : n'importe qui peut écrire `"userId": 2`. C'est le sujet de Spring Security, hors programme de ce cours.

### 2.2 — Le service

Dans `TradeService` :

```java
public Trade reject(Long tradeId, Long userId) {
    Trade trade = findById(tradeId);   // 404
    checkAcceptable(trade, userId);    // 403 ou 409
    trade.setStatus(TradeStatus.REJECTED);
    return trades.save(trade);         // id renseigné → UPDATE
}
```

Écrivez `checkAcceptable(Trade trade, Long userId)`, une méthode privée qui lève :

- `403 Forbidden` si `userId` n'est pas le destinataire du troc. `403` dit : « je sais ce que vous demandez, mais **vous** n'avez pas le droit de le faire ». C'est différent de `401 Unauthorized`, qui veut dire « je ne sais pas qui vous êtes » ;
- `409 Conflict` si le troc n'est plus `PROPOSED` (on ne refuse pas un troc déjà exécuté ou déjà refusé).

<details>
<summary>Solution : checkAcceptable</summary>

```java
private void checkAcceptable(Trade trade, Long userId) {
    // 403 : la requête est comprise, mais cet utilisateur n'a pas le droit de répondre
    if (!Objects.equals(trade.getRecipientId(), userId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Seul le destinataire (utilisateur " + trade.getRecipientId() + ") peut répondre à ce troc");
    }
    // 409 : le troc a déjà reçu une réponse
    if (trade.getStatus() != TradeStatus.PROPOSED) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Le troc " + trade.getId() + " est déjà " + trade.getStatus());
    }
}
```

</details>

### 2.3 — La route

Accepter et refuser sont des **actions métier** sur un troc existant. On les modélise comme des sous-ressources, en `POST` (une action qui modifie l'état, et qu'on ne rejoue pas sans conséquence) :

```java
@PostMapping("/{id}/reject") // POST /trades/7/reject
public Trade reject(@PathVariable Long id, @RequestBody TradeDecisionRequest request) {
    return tradeService.reject(id, request.userId());
}
```

> Pourquoi pas `PUT /trades/7` avec `{"status": "REJECTED"}` ? Ce serait laisser le client choisir le statut, et donc pouvoir écrire `"EXECUTED"` sans passer par les règles. Une route par action garde les règles dans le service (même raisonnement que `ProposeTradeRequest` en séance 2).

### 2.4 — Tester

Dans Bruno (avec Alice, Bob, leurs objets et le troc 1 de la séance 2) :

1. **Trocs > Proposer troc** une deuxième fois : troc 2 ;
2. **Trocs > Refuser troc** (`POST /trades/2/reject`, `userId` 2) : `200`, `"status": "REJECTED"` ;
3. la même requête une deuxième fois : `409` ;
4. modifiez le corps avec `"userId": 1` : `403` (Alice ne peut pas répondre à sa propre proposition).

## Étape 3 — Déplacer une quantité d'un utilisateur à l'autre

Avant d'accepter, il faut savoir **transférer** une quantité d'objet. Pour 3 tomates d'Alice vers Bob :

- **débit** : la ligne « tomates » d'Alice perd 3 ;
- **crédit** : si Bob a déjà une ligne « tomates », elle gagne 3 ; sinon, on **crée** une ligne « tomates » de quantité 3 pour Bob.

C'est pour ce crédit que la contrainte `UNIQUE (owner_id, name)` du bloc 1 est précieuse : il y a au plus **une** ligne à retrouver, avec `findByOwnerIdAndName`.

Le transfert manipule des objets : il a sa place dans `ItemService`. Écrivez :

```java
public void transfer(Item item, int quantity, Long recipientId) {
    // débit : retirer quantity à item, et l'enregistrer
    // crédit : chercher la ligne (recipientId, item.getName()) ; l'augmenter, ou la créer
}
```

N'utilisez pas `update` : sa vérification « quantité > 0 » refuserait un stock qui tombe à 0, alors que c'est un cas normal (Alice offre toutes ses tomates). Appelez directement `items.save(...)`.

> **Question à trancher** : la création d'une ligne chez le destinataire doit-elle respecter `troc.item.max-per-user` ? Si oui, un troc accepté peut échouer parce que Bob a déjà 20 objets. Si non, la règle peut être contournée par des trocs. Choisissez, et justifiez dans un commentaire.

<details>
<summary>Solution : transfer</summary>

```java
// Pas de @Transactional ici : la méthode rejoint la transaction de l'appelant (TradeService.accept).
public void transfer(Item item, int quantity, Long recipientId) {
    item.setQuantity(item.getQuantity() - quantity); // débit
    items.save(item);

    // crédit : la ligne (destinataire, nom) existe déjà, ou on la crée.
    // Choix : max-per-user n'est pas appliqué ici, un troc accepté ne doit pas échouer pour ça.
    Item credited = items.findByOwnerIdAndName(recipientId, item.getName()).orElseGet(() -> {
        Item created = new Item();
        created.setOwnerId(recipientId);
        created.setName(item.getName());
        return created; // id null → save() fera un INSERT
    });
    credited.setQuantity(credited.getQuantity() + quantity);
    items.save(credited);
}
```

`orElseGet` renvoie le contenu de l'`Optional` s'il y en a un, sinon le résultat de la fonction fournie.

</details>

## Étape 4 — Accepter un troc, première version

### 4.1 — Revérifier avant d'écrire

Entre la proposition et l'acceptation, du temps a passé : Bob a peut-être donné ses carottes dans un autre troc, ou modifié son stock. Les vérifications de `propose` ne suffisent plus, il faut **revérifier les stocks au moment d'exécuter**, et **avant toute écriture**.

Écrivez dans `TradeService` :

```java
public Trade accept(Long tradeId, Long userId) {
    Trade trade = findById(tradeId);                                        // 404
    checkAcceptable(trade, userId);                                         // 403 ou 409

    Item offered = itemService.findById(trade.getOfferedItemId());
    Item requested = itemService.findById(trade.getRequestedItemId());
    // les stocks ont pu changer depuis la proposition : on revérifie, AVANT toute écriture
    checkStock(offered, trade.getOfferedQuantity());                        // 409
    checkStock(requested, trade.getRequestedQuantity());

    itemService.transfer(offered, trade.getOfferedQuantity(), trade.getRecipientId());
    itemService.transfer(requested, trade.getRequestedQuantity(), trade.getProposerId());
    trade.setStatus(TradeStatus.EXECUTED);
    return trades.save(trade);
}
```

…avec `checkStock(Item item, int quantity)`, qui lève un `409` si `item.getQuantity()` est inférieur à `quantity`, et la route `POST /{id}/accept` dans `TradeController`, sur le modèle de `reject`.

C'est aussi la réponse au bonus 4 de la séance 2 : si Alice a promis 8 tomates à Bob et 8 à Charlie, le premier troc accepté passe, le second répond `409`.

### 4.2 — Tester le cas qui marche

Repartez d'une base propre : arrêtez l'application, **supprimez `data/`**, relancez. Puis, dans Bruno :

1. **Créer utilisateur** (Alice, 1), **Créer un 2e utilisateur** (Bob, 2) ;
2. **Créer objet (Alice)** (10 tomates, objet 1), **Créer objet (Bob)** (5 carottes, objet 2) ;
3. **Proposer troc** (3 tomates contre 4 carottes, troc 1) ;
4. **Accepter troc** (`POST /trades/1/accept`, `userId` 2) : `200`, `"status": "EXECUTED"`.

Dans la console de l'application, comptez les requêtes `UPDATE` et `INSERT` qui ont suivi l'appel : il y a bien cinq écritures. Dans la console H2 :

```sql
SELECT * FROM items ORDER BY owner_id, name;
```

| ID | OWNER_ID | NAME | QUANTITY |
|---|---|---|---|
| 4 | 1 | carottes | 4 |
| 1 | 1 | tomates | 7 |
| 2 | 2 | carottes | 1 |
| 3 | 2 | tomates | 3 |

Exécutez encore **Accepter troc** : `409`, le troc est déjà `EXECUTED`. Et **Accepter troc (pas le destinataire)** : `403`.

## Étape 5 — L'expérience : sans transaction, puis avec

Pour voir ce qu'apporte une transaction, il faut une écriture qui **échoue au milieu**. On va en provoquer une, volontairement.

### 5.1 — Préparer la panne

Dans `accept`, mettez **temporairement** en commentaire la vérification du stock demandé :

```java
checkStock(offered, trade.getOfferedQuantity());
// checkStock(requested, trade.getRequestedQuantity());   ← EXPÉRIENCE, à remettre
```

C'est le bug qu'on simule : un développeur a oublié une vérification. Le code va maintenant tenter de retirer à Bob plus de carottes qu'il n'en a, et c'est la contrainte `CHECK (quantity >= 0)` du bloc 1 qui va refuser l'écriture.

### 5.2 — Premier essai : sans transaction

Arrêtez l'application, supprimez `data/`, relancez. Dans Bruno :

1. Créez Alice, Bob, les 10 tomates d'Alice et les 5 carottes de Bob ;
2. **Proposer troc** (3 tomates contre 4 carottes) : accepté, Bob a bien 5 carottes ;
3. **Objets > Réduire le stock de Bob** (`PUT /items/2`, `quantity` 1) : Bob a donné ses carottes ailleurs entre-temps ;
4. **Accepter troc**.

Notez la réponse, puis regardez la base :

```sql
SELECT * FROM items ORDER BY owner_id, name;
SELECT id, status FROM trades;
```

Répondez, par écrit :

- Quel code a reçu le client ? Que dit la dernière ligne `Caused by` de la console ?
- Combien de tomates reste-t-il à Alice ? Qu'a reçu Bob ? Qu'a reçu Alice en échange ?
- Quel est le statut du troc ? Que se passerait-il si Bob acceptait à nouveau, après avoir racheté des carottes ?

<details>
<summary>Ce que vous devriez observer</summary>

Le client reçoit un `500` : la contrainte `CHECK` a refusé de passer les carottes de Bob à −3. Mais les deux premières écritures, elles, ont été **enregistrées** : Alice n'a plus que 7 tomates, Bob en a reçu 3, et Alice n'a rien reçu. Le troc est toujours `PROPOSED` : s'il est accepté à nouveau, Alice perdra encore 3 tomates.

Chaque appel à `save()` a été validé immédiatement, indépendamment des autres. La base est **cohérente** du point de vue des contraintes (aucun stock négatif), mais **fausse** du point de vue du métier : des tomates ont été données en échange de rien.

</details>

### 5.3 — Deuxième essai : avec `@Transactional`

Ajoutez une annotation sur `accept` (package `org.springframework.transaction.annotation`) :

```java
@Transactional // BEGIN … puis COMMIT si tout va bien, ou ROLLBACK si une exception sort de la méthode
public Trade accept(Long tradeId, Long userId) {
```

Rejouez **exactement** le même scénario (arrêter, supprimer `data/`, relancer, mêmes requêtes Bruno), et répondez aux mêmes questions.

<details>
<summary>Ce que vous devriez observer</summary>

Toujours un `500` : la panne est la même. Mais cette fois, la base est **exactement dans l'état d'avant l'appel** : Alice a ses 10 tomates, Bob n'a pas de ligne « tomates », le troc est `PROPOSED`. Les deux écritures déjà faites ont été annulées (`ROLLBACK`).

Les cinq écritures forment maintenant un tout : elles sont toutes validées, ou aucune. C'est l'**atomicité**, le « A » de [ACID](https://fr.wikipedia.org/wiki/Propri%C3%A9t%C3%A9s_ACID).

</details>

### 5.4 — Troisième essai : remettre la vérification

Retirez le commentaire de `checkStock(requested, …)`. Rejouez le scénario : `409 Conflict`, « Stock insuffisant », sans aucune écriture tentée.

Les trois essais résument les trois niveaux de protection :

| Niveau | Rôle | Dans l'expérience |
|---|---|---|
| Le **service** vérifie | répondre un code clair au client, avant d'écrire | `409` au lieu de `500` |
| La **transaction** protège | annuler les écritures déjà faites si la suite échoue | stock d'Alice intact |
| Les **contraintes** garantissent | refuser toute écriture incohérente, même si le code se trompe | `CHECK` a stoppé le débit négatif |

> La contrainte `CHECK` n'est qu'**un** déclencheur possible. Une panne réseau entre l'application et la base, un crash du serveur, un bug dans le crédit… produiraient le même état incohérent sans transaction. La transaction couvre tous ces cas, y compris ceux qu'on n'a pas prévus.

**Gardez vos réponses** : avec l'état de la base avant et après chaque essai, elles constituent une démonstration reproductible du rôle de la transaction. C'est exactement ce qu'on attend dans le rapport du projet.

## Étape 6 — Comprendre `@Transactional`

### 6.1 — Où placer l'annotation ?

Sur la **méthode publique du service** qui porte l'opération métier complète :

- pas sur le contrôleur : trop haut, il ne connaît pas le métier, et la transaction engloberait la conversion JSON ;
- pas sur chaque méthode du repository : trop bas, chaque écriture aurait sa propre transaction, comme dans l'essai 5.2 (c'est d'ailleurs ce que fait Spring Data par défaut : chaque `save()` est une petite transaction à lui seul) ;
- `ItemService.transfer`, appelé **depuis** `accept`, n'a pas besoin de l'annotation : il s'exécute dans la transaction déjà ouverte, et ses écritures sont annulées avec les autres.

`reject` n'a pas d'annotation : une seule écriture, déjà atomique.

### 6.2 — Comment ça marche : le proxy

`TradeController` croit appeler `TradeService`. En réalité, au démarrage, Spring a vu `@Transactional` et a injecté dans le contrôleur un **proxy** : un objet qui se fait passer pour `TradeService` et entoure chaque appel de `accept` :

```
TradeController ──accept(1, 2)──▶ [ proxy : BEGIN ] ──▶ TradeService.accept(…) : vos 5 écritures
                                  [ proxy : COMMIT si fin normale, ROLLBACK si RuntimeException ]
```

C'est le conteneur IoC de la séance 1 qui rend ça possible : comme c'est lui qui crée les beans et les injecte, il peut injecter un emballage autour de `TradeService` plutôt que l'objet lui-même. Vous pouvez le constater : dans `TradeController`, affichez `tradeService.getClass()` dans la console. Le nom affiché n'est pas `TradeService`, mais quelque chose comme `TradeService$$SpringCGLIB$$0`.

> Ce mécanisme d'interception porte un nom : la **programmation orientée aspect** (AOP). `@Transactional` est un aspect fourni par Spring. En séance 4, on écrira le nôtre.

### 6.3 — Les pièges : quand `@Transactional` ne fait rien

Ils ont tous la même cause : le proxy n'est pas traversé, ou il ne voit pas l'exception.

| Piège | Pourquoi la transaction n'agit pas |
|---|---|
| Appel interne : une autre méthode de `TradeService` appelle `this.accept(…)` | L'appel ne sort pas de l'objet, il ne passe pas par le proxy : pas de `BEGIN`. |
| Exception **vérifiée** (une sous-classe d'`Exception` qui n'est pas une `RuntimeException`) | Par défaut, seule une `RuntimeException` déclenche le `ROLLBACK`. Pour une exception vérifiée, le proxy valide (`COMMIT`). Pour changer ce comportement : `@Transactional(rollbackFor = Exception.class)`. |
| `try { … } catch (Exception e) { … }` dans la méthode, sans relancer | Le proxy ne voit jamais l'exception : pour lui, la méthode s'est terminée normalement, il valide. |
| Méthode `private`, ou classe qui n'est pas un bean | Le proxy ne peut intercepter que les méthodes publiques d'un bean créé par Spring. |

Dans notre projet, `ResponseStatusException` et les erreurs de la base sont des `RuntimeException` : elles déclenchent bien le `ROLLBACK`.

**Pour vous en convaincre** (facultatif) : refaites l'essai 5.3 en entourant le second `itemService.transfer(…)` d'un `try { … } catch (RuntimeException e) { System.out.println(e.getMessage()); }`. Que se passe-t-il ? Remettez le code d'origine ensuite.

<details>
<summary>Réponse</summary>

La console affiche bien le message du `catch`, la méthode continue et passe le troc à `EXECUTED`… puis le client reçoit quand même un `500`, avec un message déroutant : « Transaction rolled back because it has been marked as rollback-only » (`UnexpectedRollbackException`). Les stocks sont intacts.

Explication : l'exception est née **dans** `save()`, qui est lui-même une méthode `@Transactional` de Spring Data. En la traversant, l'exception a marqué la transaction en cours « à annuler », avant que votre `catch` ne l'attrape. À la sortie de `accept`, le proxy veut valider, découvre la marque, annule tout et le signale par cette exception.

Ici, on s'en sort par chance. Si l'exception avalée venait de **votre** code (une `ResponseStatusException` levée entre deux écritures, par exemple), aucun proxy ne l'aurait vue : le proxy de `accept` aurait validé les écritures déjà faites, et on retrouverait l'état incohérent de l'essai 5.2. Règle simple : ne rattrapez pas une exception dans une méthode `@Transactional`, sauf pour la relancer.

</details>

> **Garder la transaction courte** : pendant qu'elle est ouverte, les lignes modifiées sont verrouillées, et les autres requêtes qui veulent les modifier attendent. Pas d'appel à un service externe, pas d'envoi d'e-mail, pas d'attente à l'intérieur.

## Étape 7 — Tout vérifier

Repartez d'une base propre et vérifiez chaque ligne :

| Scénario | Requête | Code attendu |
|---|---|---|
| Accepter un troc valide | **Accepter troc** | `200`, stocks échangés |
| Accepter un troc inconnu | `POST /trades/99/accept` | `404` |
| Accepter à la place du destinataire | **Accepter troc (pas le destinataire)** | `403` |
| Accepter deux fois | **Accepter troc**, deux fois | `409` la seconde fois |
| Accepter alors que le stock a baissé | **Réduire le stock de Bob**, puis **Accepter troc** | `409`, stocks intacts |
| Refuser | **Refuser troc** (sur un troc `PROPOSED`) | `200`, stocks intacts |
| Refuser un troc déjà exécuté | **Refuser troc** sur le troc 1 | `409` |
| Les données survivent | redémarrer, **Lister trocs** | les statuts sont conservés |

Et les requêtes des séances précédentes répondent toujours comme avant.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| L'essai 5.3 donne le même résultat que le 5.2 | Import de `@Transactional` incorrect, annotation sur une méthode `private`, ou `accept` appelée depuis une autre méthode de `TradeService` (appel interne). Vérifiez aussi que l'application a bien été **relancée** après l'ajout. |
| Les résultats de l'expérience ne correspondent pas | La base n'a pas été remise à zéro entre deux essais : arrêtez l'application et supprimez `data/` **avant** chaque essai, puis recréez les données dans l'ordre. |
| `403` alors que `userId` est bien le destinataire | Clé mal nommée dans le corps (`"user"` au lieu de `"userId"`) : `userId` arrive à `null`, qui ne correspond à aucun destinataire. Un corps absent, lui, donne `400`. |
| `400 La quantité doit être positive` pendant un troc | `transfer` appelle `itemService.update`, qui refuse un stock à 0 : utilisez `items.save` directement. |
| `500` « Unique index or primary key violation » pendant un troc | Le crédit crée une nouvelle ligne au lieu de compléter celle qui existe : vérifiez `findByOwnerIdAndName` (bons paramètres, dans le bon ordre). |
| `500` « Referential integrity constraint violation » au crédit | La nouvelle ligne est créée avec un mauvais `ownerId` (le proposeur au lieu du destinataire, ou l'inverse). |
| Le stock d'Alice baisse deux fois | Le débit est fait à la fois dans `accept` et dans `transfer`. |
| `405 Method Not Allowed` sur `/trades/1/accept` | Requête envoyée en `PUT` ou `GET`, ou `@PostMapping("/{id}/accept")` mal recopié. |

## En avance ? Features bonus

### Bonus 8 — Le proposeur annule

Alice change d'avis avant que Bob ait répondu. Ajoutez `POST /trades/{id}/cancel`, réservé au **proposeur**, qui passe un troc `PROPOSED` au statut `CANCELLED`. Quels codes d'erreur ? `checkAcceptable` est-il réutilisable tel quel ?

### Bonus 9 — Des index justifiés

Un **index** permet à la base de trouver des lignes sans lire toute la table, comme l'index d'un livre. Il accélère les lectures, mais ralentit un peu chaque écriture (l'index doit être mis à jour) : on indexe **ce qu'on interroge souvent**, et on le justifie.

1. Listez les requêtes de votre application qui filtrent les trocs (indice : `GET /trades?userId=` et la règle `max-pending`).
2. Dans la console H2, regardez les index qui existent déjà :

   ```sql
   SELECT INDEX_NAME, TABLE_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'TRADES';
   ```

   H2 crée automatiquement un index pour chaque clé étrangère. Ce n'est pas le cas de toutes les bases : PostgreSQL, par exemple, ne le fait pas.
3. La requête de `countByProposerIdAndStatus` filtre sur **deux** colonnes. Ajoutez à `schema.sql` un index adapté :

   ```sql
   CREATE INDEX IF NOT EXISTS idx_trades_proposer_status ON trades (proposer_id, status);
   ```

4. Vérifiez qu'il est utilisé, avec `EXPLAIN` devant la requête dans la console H2 :

   ```sql
   EXPLAIN SELECT COUNT(*) FROM trades WHERE proposer_id = 1 AND status = 'PROPOSED';
   ```

Notez dans un commentaire de `schema.sql` **quelle requête** chaque index sert. Avec quelques lignes, la différence de vitesse est invisible ; avec des millions de trocs, elle se compte en secondes.

### Bonus 10 — Deux acceptations en même temps

Que se passe-t-il si Bob clique deux fois sur « accepter », et que les deux requêtes arrivent en même temps ? Chacune lit le troc `PROPOSED`, chacune vérifie les stocks… avant que l'autre ait écrit.

**Provoquer le problème** : dans `accept`, juste après les `checkStock`, ajoutez temporairement une pause :

```java
try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
```

Ouvrez **Accepter troc** dans deux onglets de Bruno, et envoyez-les à moins de 5 secondes d'intervalle. Observez les deux réponses et l'état de la base. Le résultat est-il celui attendu ?

**Pistes de solution** (à choisir et justifier) :

- **Verrouillage optimiste** : ajoutez un champ `@Version private Long version;` à `Trade` (et une colonne `version BIGINT` à la table). À chaque `save()`, Spring Data JDBC vérifie que la version n'a pas changé depuis la lecture ; sinon, il lève `OptimisticLockingFailureException`, et la transaction est annulée. À traduire en `409`.
- **Mise à jour conditionnelle** : faire vérifier la condition par la base, dans la même requête que l'écriture. Avec une requête écrite à la main dans le repository :

  ```java
  @Modifying
  @Query("UPDATE trades SET status = 'EXECUTED' WHERE id = :id AND status = 'PROPOSED'")
  int markExecuted(Long id); // nombre de lignes modifiées : 0 si un autre a été plus rapide
  ```

  (`@Modifying` et `@Query` du package `org.springframework.data.jdbc.repository.query`). Le même principe s'applique au débit d'un stock : `UPDATE items SET quantity = quantity - :qty WHERE id = :id AND quantity >= :qty`.

Documentation : [Spring Data JDBC — Optimistic Locking](https://docs.spring.io/spring-data/relational/reference/jdbc/entity-persistence.html#jdbc.entity-persistence.optimistic-locking) et [Spring Data JDBC — `@Query`](https://docs.spring.io/spring-data/relational/reference/jdbc/query-methods.html#jdbc.query-methods.at-query).

### Bonus 11 — Dater la réponse

Si vous avez fait le bonus 7 du bloc 1 (`proposed_at`), ajoutez une colonne `decided_at TIMESTAMP` (sans `NOT NULL` : elle reste vide tant que le troc est `PROPOSED`), renseignée par `accept` et `reject`.

## Pour aller plus loin

- [Spring Framework — Gestion déclarative des transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html), et en particulier [le rôle du proxy](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-decl-explained.html) et [les règles de rollback](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)
- [Spring Guide — Managing Transactions](https://spring.io/guides/gs/managing-transactions/) (un exemple complet d'écritures annulées par `@Transactional`)
- [Wikipédia — Propriétés ACID](https://fr.wikipedia.org/wiki/Propri%C3%A9t%C3%A9s_ACID) (atomicité, cohérence, isolation, durabilité)
- [MDN — 403 Forbidden](https://developer.mozilla.org/fr/docs/Web/HTTP/Status/403) et [MDN — 401 Unauthorized](https://developer.mozilla.org/fr/docs/Web/HTTP/Status/401)
- [H2 — EXPLAIN et les index](https://h2database.com/html/performance.html#explain_plan)
