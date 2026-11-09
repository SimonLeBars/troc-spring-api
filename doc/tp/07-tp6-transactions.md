# TP 6 — Les transactions : accepter un troc, tout ou rien

> Rythme indicatif : séance 3. Point de départ : la mise en place du TP 5 terminée (les trois tables, plus de classe `InMemory…`).

## Le concept en bref

Une opération métier demande souvent **plusieurs écritures** en base. Accepter un troc, c'est retirer des tomates à Alice, en ajouter à Bob, retirer des carottes à Bob, en ajouter à Alice, et changer le statut du troc. Si la troisième écriture échoue, les deux premières sont déjà faites : Alice a donné ses tomates, et n'a rien reçu.

Une **transaction** regroupe plusieurs écritures en un tout : elles sont toutes validées ensemble (`COMMIT`), ou toutes annulées (`ROLLBACK`). C'est l'**atomicité**, le « A » de [ACID](https://fr.wikipedia.org/wiki/Propri%C3%A9t%C3%A9s_ACID). Avec Spring, une annotation suffit : `@Transactional`.

---

## Partie 1 — Expérimentation : une panne au milieu (facultative)

**Vous pouvez passer à la partie 2 si vous savez répondre à ces questions :**

- Sans transaction, que deviennent les écritures déjà faites quand l'écriture suivante échoue ?
- Que fait `@Transactional` quand une exception sort de la méthode ? Et quand elle est attrapée à l'intérieur ?
- Pourquoi `@Transactional` n'a-t-il aucun effet sur un appel interne (`this.methode()`) ?

Le code temporaire va dans le package `fr.cours.troc.sandbox.transaction`.

### Étape 1 — Deux écritures, dont la seconde échoue

Un déplacement de stock simplifié, comme un virement bancaire : on **crédite** un objet, puis on **débite** l'autre. Si le stock débité est insuffisant, la contrainte `CHECK (quantity >= 0)` du TP 5 refuse la seconde écriture.

```java
package fr.cours.troc.sandbox.transaction;

@Service
public class StockMoveDemo {

    private final ItemRepository items;

    public StockMoveDemo(ItemRepository items) {
        this.items = items;
    }

    // @Transactional   ← à décommenter pour le deuxième essai
    public void move(Long fromId, Long toId, int quantity) {
        Item to = items.findById(toId).orElseThrow();
        to.setQuantity(to.getQuantity() + quantity);
        items.save(to);                               // écriture 1 : le crédit

        Item from = items.findById(fromId).orElseThrow();
        from.setQuantity(from.getQuantity() - quantity);
        items.save(from);                             // écriture 2 : le débit, refusé si le stock devient négatif
    }
}
```

```java
@RestController
public class StockMoveDemoController {

    private final StockMoveDemo demo;

    public StockMoveDemoController(StockMoveDemo demo) {
        this.demo = demo;
    }

    @PostMapping("/sandbox/move") // POST /sandbox/move?fromId=2&toId=1&quantity=8
    public void move(@RequestParam Long fromId, @RequestParam Long toId, @RequestParam int quantity) {
        demo.move(fromId, toId, quantity);
    }
}
```

### Étape 2 — Premier essai : sans transaction

Arrêtez l'application, **supprimez `data/`**, relancez. Dans Bruno, créez Alice et Bob, puis **Créer objet (Alice)** (10 tomates, objet 1) et **Créer objet (Bob)** (5 carottes, objet 2). Exécutez **Expérimentation > TP6 - Déplacer du stock** : 8 unités de l'objet 2 vers l'objet 1, alors que Bob n'en a que 5.

Notez la réponse, la dernière ligne `Caused by` de la console, puis regardez la base (console H2) :

```sql
SELECT * FROM items;
```

<details>
<summary>Ce que vous devriez observer</summary>

Un `500` : la contrainte `CHECK` a refusé de passer l'objet 2 à −3. Mais le crédit, lui, a été **enregistré** : l'objet 1 est passé à 18. Huit unités ont été créées à partir de rien.

Chaque `save()` a été validé immédiatement, indépendamment des autres. La base respecte ses contraintes (aucun stock négatif), mais elle est **fausse** du point de vue du métier.

</details>

### Étape 3 — Deuxième essai : avec `@Transactional`

Décommentez `@Transactional` (package `org.springframework.transaction.annotation`). Arrêtez, supprimez `data/`, relancez, recréez les mêmes données, et rejouez exactement la même requête.

<details>
<summary>Ce que vous devriez observer</summary>

Toujours un `500` : la panne est la même. Mais l'objet 1 est resté à 10 : le crédit a été **annulé** (`ROLLBACK`), parce que l'exception est sortie de la méthode transactionnelle.

</details>

### Étape 4 — Deux pièges

1. Dans `StockMoveDemo`, ajoutez une méthode publique `moveFromInside(…)`, **sans** `@Transactional`, qui se contente d'appeler `this.move(…)` avec les mêmes arguments. Faites appeler `moveFromInside` par le contrôleur (`move` garde son `@Transactional`), et rejouez le scénario. La transaction agit-elle ?
2. Revenez à l'appel direct de `move`. Entourez la seconde écriture d'un `try { … } catch (RuntimeException e) { System.out.println(e.getMessage()); }` et rejouez. Que reçoit le client ? Et la base ?

<details>
<summary>Ce que vous devriez observer</summary>

1. Non : le crédit reste enregistré. `@Transactional` n'est pas magique, c'est un **proxy** : au démarrage, Spring injecte dans le contrôleur un objet qui se fait passer pour `StockMoveDemo`, et qui entoure chaque appel d'un `BEGIN` et d'un `COMMIT` ou `ROLLBACK`. L'appel `this.move(…)` reste **à l'intérieur** de l'objet, il ne passe pas par le proxy : pas de transaction.
2. Le message s'affiche, puis le client reçoit quand même un `500`, avec « Transaction rolled back because it has been marked as rollback-only ». L'exception est née dans `save()`, lui-même transactionnel : en le traversant, elle a marqué la transaction « à annuler » avant que votre `catch` ne l'attrape. On s'en sort par chance ; si l'exception venait de **votre** code, le proxy n'aurait rien vu et aurait validé le crédit. Règle simple : ne rattrapez pas une exception dans une méthode `@Transactional`, sauf pour la relancer.

</details>

### Ce qu'il faut retenir

```
Contrôleur ──move(…)──▶ [ proxy : BEGIN ] ──▶ StockMoveDemo.move(…) : écriture 1, écriture 2
                        [ proxy : COMMIT si fin normale, ROLLBACK si une RuntimeException sort ]
```

- Sans transaction, chaque écriture est validée seule : une panne au milieu laisse la base dans un état métier faux.
- `@Transactional` est un proxy créé par le conteneur IoC (TP 1) : il n'agit que sur les appels qui le **traversent**, et que si l'exception **sort** de la méthode.
- La contrainte `CHECK` n'est qu'un déclencheur possible. Une panne réseau, un crash du serveur, un bug produiraient le même état sans transaction : elle couvre aussi les cas qu'on n'a pas prévus.

**Supprimez le package `fr.cours.troc.sandbox`.**

---

## Partie 2 — Mise en place : accepter ou refuser un troc (obligatoire)

Au TP 4, un troc s'arrêtait au statut `PROPOSED`. Le destinataire donne maintenant **le second accord**, ou refuse :

```
                       POST /trades/{id}/accept        ┌──────────┐
                     ┌───────────────────────────────▶ │ EXECUTED │  stocks échangés
POST /trades  ┌──────┴───┐  (le destinataire accepte)  └──────────┘
────────────▶ │ PROPOSED │
   (TP 4)     └──────┬───┘  (le destinataire refuse)   ┌──────────┐
                     └───────────────────────────────▶ │ REJECTED │  stocks intacts
                       POST /trades/{id}/reject        └──────────┘
```

L'échange a lieu **au moment de l'acceptation** : pas d'état « accepté mais pas encore échangé ».

### Objectifs

| Verbe | Chemin | Corps | Rôle | Code si tout va bien | Codes d'erreur |
|---|---|---|---|---|---|
| `POST` | `/trades/{id}/reject` | `{"userId": 2}` | refuser un troc | `200` | `404` troc inconnu, `403` pas le destinataire, `409` déjà traité |
| `POST` | `/trades/{id}/accept` | `{"userId": 2}` | accepter et exécuter un troc | `200` | les mêmes, et `409` stock insuffisant |

Accepter le troc d'Alice (3 tomates contre 4 carottes de Bob), ce sont **cinq écritures**, atomiques :

1. retirer 3 tomates à Alice ;
2. ajouter 3 tomates à Bob : à sa ligne « tomates » s'il en a une, sinon dans une **nouvelle** ligne « tomates » à son nom ;
3. retirer 4 carottes à Bob ;
4. ajouter 4 carottes à Alice (même principe) ;
5. passer le troc au statut `EXECUTED`.

Règles :

- seul le **destinataire** peut accepter ou refuser (`403` sinon) ;
- un troc qui n'est plus `PROPOSED` ne peut plus recevoir de réponse (`409`) ;
- entre la proposition et l'acceptation, les stocks ont pu changer : ils sont **revérifiés au moment d'accepter, avant toute écriture** (`409`) ;
- un stock qui tombe à 0 est un cas normal (Alice offre toutes ses tomates) ;
- si l'une des cinq écritures échoue, **aucune** ne reste.

> **Question à trancher** : la création d'une ligne chez le destinataire doit-elle respecter `troc.item.max-per-user` ? Si oui, un troc accepté peut échouer parce que Bob a déjà 20 objets. Si non, la règle peut être contournée par des trocs. Choisissez, et justifiez dans un commentaire.

### Syntaxes utiles

#### Qui répond ? Le corps de la requête

L'API n'a pas d'authentification : le client **se déclare** dans le corps, avec un objet de requête (TP 4) :

```java
public record TradeDecisionRequest(Long userId) {
}
```

> Dans une vraie application, l'identité viendrait de l'authentification (un jeton, une session), jamais du corps : n'importe qui peut écrire `"userId": 2`. C'est le sujet de Spring Security, en annexe du cours.

`403 Forbidden` dit : « je sais ce que vous demandez, mais **vous** n'avez pas le droit ». C'est différent de `401 Unauthorized` : « je ne sais pas qui vous êtes ».

#### Une action métier sur une ressource

```java
@PostMapping("/{id}/accept") // POST /trades/7/accept
```

On modélise accepter et refuser comme des sous-ressources, en `POST` (une action qui modifie l'état, qu'on ne rejoue pas sans conséquence). Pourquoi pas `PUT /trades/7` avec `{"status": "REJECTED"}` ? Ce serait laisser le client choisir le statut, et donc écrire `"EXECUTED"` sans passer par les règles (TP 4).

#### Une transaction

```java
@Transactional // org.springframework.transaction.annotation : BEGIN … COMMIT, ou ROLLBACK si une RuntimeException sort
public UnType operationMetier(…) { … }
```

Où la placer : sur la **méthode publique du service** qui porte l'opération métier complète.

- Pas sur le contrôleur : trop haut, il ne connaît pas le métier.
- Pas sur chaque écriture : chaque `save()` serait sa propre transaction, c'est déjà le comportement par défaut de Spring Data.
- Une méthode d'un autre service appelée **depuis** la méthode transactionnelle s'exécute dans la transaction déjà ouverte : pas besoin de l'annoter.
- Une opération qui ne fait qu'**une** écriture est déjà atomique.

Les pièges, qui ont tous la même cause (le proxy n'est pas traversé, ou il ne voit pas l'exception) :

| Piège | Pourquoi la transaction n'agit pas |
|---|---|
| Appel interne : `this.accept(…)` depuis une autre méthode du même service | L'appel ne passe pas par le proxy. |
| Exception **vérifiée** (pas une `RuntimeException`) | Par défaut, seule une `RuntimeException` déclenche le `ROLLBACK`. Sinon : `@Transactional(rollbackFor = Exception.class)`. |
| `try { … } catch (…) { … }` dans la méthode, sans relancer | Le proxy ne voit jamais l'exception : il valide. |
| Méthode `private`, ou classe qui n'est pas un bean | Le proxy n'intercepte que les méthodes publiques d'un bean. |

`ResponseStatusException` et les erreurs de la base sont des `RuntimeException` : elles déclenchent bien le `ROLLBACK`.

> **Garder la transaction courte** : pendant qu'elle est ouverte, les lignes modifiées sont verrouillées. Pas d'appel à un service externe, pas d'envoi d'e-mail, pas d'attente à l'intérieur.

#### `Optional` : la valeur, ou une valeur construite

```java
Item credited = repository.findBy…(…).orElseGet(() -> { /* construire un nouvel objet */ });
```

`orElseGet` renvoie le contenu de l'`Optional` s'il y en a un, sinon le résultat de la fonction fournie. Un objet construit avec un `id` à `null` sera inséré par `save()`.

### Indices

- Commencez par **refuser** : une seule écriture, sans transaction. Les vérifications « destinataire » et « statut » serviront aussi à `accept` : une méthode privée commune évite de les écrire deux fois.
- Le **transfert** d'une quantité d'un objet vers un utilisateur (débit, puis crédit d'une ligne existante ou nouvelle) manipule des objets : il a sa place dans `ItemService`. La contrainte `UNIQUE (owner_id, name)` du TP 5 garantit qu'il y a au plus une ligne à créditer.
- N'utilisez pas la méthode `update` d'`ItemService` pour le transfert : sa vérification « quantité > 0 » refuserait un stock qui tombe à 0.
- `accept` lit les deux objets, revérifie les deux stocks, **puis** fait les deux transferts et change le statut.
- La règle `max-pending` du TP 4 compte les trocs `PROPOSED` : un troc accepté ou refusé libère une place pour son proposeur, sans rien changer.

### Vérifier

#### Les scénarios

Repartez d'une base propre (arrêtez, supprimez `data/`, relancez), créez Alice, Bob, leurs objets (10 tomates, 5 carottes) et un troc (3 tomates contre 4 carottes), puis :

| Scénario | Requête | Résultat attendu |
|---|---|---|
| Accepter un troc valide | **Trocs > Accepter troc** | `200`, `EXECUTED`, cinq écritures dans les traces SQL |
| Accepter un troc inconnu | `POST /trades/99/accept` | `404` |
| Accepter à la place du destinataire | **Accepter troc (pas le destinataire)** | `403` |
| Accepter deux fois | **Accepter troc**, deux fois | `409` la seconde fois |
| Accepter alors que le stock a baissé | nouveau troc, **Objets > Réduire le stock de Bob**, puis **Accepter troc** | `409`, stocks intacts |
| Refuser | **Refuser troc** sur un troc `PROPOSED` | `200`, `REJECTED`, stocks intacts |
| Refuser un troc déjà exécuté | **Refuser troc** sur le troc 1 | `409` |
| Les données survivent | redémarrer, **Lister trocs** | statuts conservés |

Après le premier scénario, dans la console H2 :

```sql
SELECT * FROM items ORDER BY owner_id, name;
```

| OWNER_ID | NAME | QUANTITY |
|---|---|---|
| 1 | carottes | 4 |
| 1 | tomates | 7 |
| 2 | carottes | 1 |
| 2 | tomates | 3 |

#### La preuve du rôle de la transaction

C'est la démonstration attendue dans le rapport du projet : la même panne, sur **votre** `accept`, sans puis avec transaction.

1. Mettez **temporairement** en commentaire la revérification du stock **demandé** dans `accept`, et retirez `@Transactional`. C'est le bug qu'on simule : un développeur a oublié une vérification.
2. Base propre, données, troc, **Réduire le stock de Bob** (1 carotte), **Accepter troc**. Notez le code reçu et l'état de `items` et `trades`.
3. Remettez `@Transactional`, et rejouez exactement le même scénario. Notez à nouveau.
4. Remettez la vérification, et rejouez : `409`, sans aucune écriture tentée.

Gardez vos observations (état de la base avant et après chaque essai) : elles constituent une démonstration reproductible des trois niveaux de protection.

| Niveau | Rôle | Dans la preuve |
|---|---|---|
| Le **service** vérifie | répondre un code clair au client, avant d'écrire | `409` au lieu de `500` |
| La **transaction** protège | annuler les écritures déjà faites si la suite échoue | stock d'Alice intact |
| Les **contraintes** garantissent | refuser toute écriture incohérente, même si le code se trompe | `CHECK` a stoppé le débit négatif |

#### Liste de vérification

- [ ] Toutes les lignes du tableau des scénarios répondent comme attendu.
- [ ] La preuve est faite, et les observations notées.
- [ ] `@Transactional` est sur `accept`, et sur aucune méthode qui n'en a pas besoin.
- [ ] Le choix pour `max-per-user` lors d'un crédit est justifié dans un commentaire.
- [ ] Les requêtes des TP précédents répondent toujours comme avant.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| Avec ou sans `@Transactional`, même résultat | Import incorrect, annotation sur une méthode `private`, appel interne, ou application pas **relancée** après l'ajout. |
| Les résultats de la preuve ne correspondent pas | La base n'a pas été remise à zéro entre deux essais : supprimez `data/` **avant** chaque essai. |
| `403` alors que `userId` est bien le destinataire | Clé mal nommée dans le corps (`"user"` au lieu de `"userId"`) : `userId` arrive à `null`. Un corps absent, lui, donne `400`. |
| `400 La quantité doit être positive` pendant un troc | Le transfert passe par `update`, qui refuse un stock à 0 : utilisez directement le repository. |
| `500` « Unique index or primary key violation » pendant un troc | Le crédit crée une nouvelle ligne au lieu de compléter celle qui existe : vérifiez la recherche par propriétaire et nom (bons paramètres, dans le bon ordre). |
| `500` « Referential integrity constraint violation » au crédit | La nouvelle ligne est créée avec un mauvais propriétaire. |
| Le stock d'Alice baisse deux fois | Le débit est fait à la fois dans `accept` et dans le transfert. |
| `405 Method Not Allowed` sur `/trades/1/accept` | Requête envoyée en `PUT` ou `GET`, ou chemin du `@PostMapping` mal recopié. |

## Bonus

### Bonus 1 — Le proposeur annule

Alice change d'avis avant que Bob ait répondu. Ajoutez `POST /trades/{id}/cancel`, réservé au **proposeur**, qui passe un troc `PROPOSED` au statut `CANCELLED`. Quels codes d'erreur ? La vérification commune à `accept` et `reject` est-elle réutilisable telle quelle ?

### Bonus 2 — Des index justifiés

Un **index** permet à la base de trouver des lignes sans lire toute la table. Il accélère les lectures mais ralentit chaque écriture : on indexe **ce qu'on interroge souvent**, et on le justifie.

1. Listez les requêtes qui filtrent les trocs (indice : `GET /trades?userId=` et la règle `max-pending`).
2. Regardez les index existants : `SELECT INDEX_NAME, TABLE_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'TRADES';`. H2 en crée un pour chaque clé étrangère ; ce n'est pas le cas de toutes les bases (PostgreSQL ne le fait pas).
3. Ajoutez à `schema.sql` un index adapté à la requête de `countByProposerIdAndStatus` (syntaxe : `CREATE INDEX IF NOT EXISTS nom ON table (colonne1, colonne2);`), et vérifiez qu'il est utilisé avec `EXPLAIN` devant la requête dans la console H2.

Notez dans un commentaire de `schema.sql` **quelle requête** chaque index sert.

### Bonus 3 — Deux acceptations en même temps

Bob clique deux fois sur « accepter », et les deux requêtes arrivent en même temps : chacune lit le troc `PROPOSED` et vérifie les stocks avant que l'autre ait écrit. Pour le provoquer, ajoutez temporairement une pause dans `accept`, juste après les vérifications :

```java
try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
```

Envoyez **Accepter troc** depuis deux onglets de Bruno, à moins de 5 secondes d'intervalle. Le résultat est-il celui attendu ? Pistes (à choisir et justifier) :

- **verrouillage optimiste** : un champ `@Version Long version` dans `Trade` (et une colonne `version BIGINT`) ; Spring Data JDBC lève `OptimisticLockingFailureException` si la ligne a changé depuis la lecture, à traduire en `409` ;
- **mise à jour conditionnelle** : faire vérifier la condition par la base dans la requête d'écriture, avec une requête écrite à la main (`@Modifying` et `@Query` du package `org.springframework.data.jdbc.repository.query`) :

  ```java
  @Modifying
  @Query("UPDATE trades SET status = 'EXECUTED' WHERE id = :id AND status = 'PROPOSED'")
  int markExecuted(Long id); // nombre de lignes modifiées : 0 si un autre a été plus rapide
  ```

Documentation : [Optimistic Locking](https://docs.spring.io/spring-data/relational/reference/jdbc/entity-persistence.html#jdbc.entity-persistence.optimistic-locking) et [`@Query`](https://docs.spring.io/spring-data/relational/reference/jdbc/query-methods.html#jdbc.query-methods.at-query).

### Bonus 4 — Dater la réponse

Si vous avez fait le bonus « Dater les trocs » du TP 5, ajoutez une colonne `decided_at TIMESTAMP` (sans `NOT NULL` : elle reste vide tant que le troc est `PROPOSED`), renseignée par `accept` et `reject`.

## Pour aller plus loin

- [Spring Framework — Gestion déclarative des transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html), en particulier [le rôle du proxy](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-decl-explained.html) et [les règles de rollback](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)
- [Spring Guide — Managing Transactions](https://spring.io/guides/gs/managing-transactions/)
- [Wikipédia — Propriétés ACID](https://fr.wikipedia.org/wiki/Propri%C3%A9t%C3%A9s_ACID)
- [MDN — 403 Forbidden](https://developer.mozilla.org/fr/docs/Web/HTTP/Status/403) et [MDN — 401 Unauthorized](https://developer.mozilla.org/fr/docs/Web/HTTP/Status/401)
- [H2 — EXPLAIN et les index](https://h2database.com/html/performance.html#explain_plan)
