# TP 2 — Une API REST : verbes, chemins, codes de statut

> Rythme indicatif : séance 2. Point de départ : la mise en place du TP 1 terminée (`GET` et `POST /users` répondent).

## Le concept en bref

Vous avez déjà écrit des API REST : ce TP ne réexplique ni les verbes, ni les codes de statut, ni la conception des URL. Il montre **comment on l'écrit en Spring**, et il est volontairement moins guidé que le TP 1 (pas de code complet, seulement des syntaxes).

Trois rappels qui serviront tout au long du cours :

- le client d'une API est un programme, pas un humain : c'est le **code de statut** qu'il teste pour décider quoi faire ;
- **`400` ou `409` ?** `400` si la requête est invalide en elle-même (quantité négative), `409` si elle est valide mais que **l'état actuel des données** s'y oppose (limite atteinte). Ce choix fait partie des choix techniques à justifier à l'évaluation ;
- **un `500` n'est jamais un choix** : c'est une exception non prévue, donc un bug à corriger.

Besoin de rafraîchir la mémoire ? [MDN — Codes de statut HTTP](https://developer.mozilla.org/fr/docs/Web/HTTP/Status), [MDN — Méthodes HTTP](https://developer.mozilla.org/fr/docs/Web/HTTP/Methods), [Microsoft — Conception d'API web RESTful](https://learn.microsoft.com/fr-fr/azure/architecture/best-practices/api-design).

---

## Partie 1 — Expérimentation : jouez le client (facultative)

**Vous pouvez passer à la partie 2 si vous savez répondre à ces questions :**

- Quelle différence entre `400`, `404` et `409` ? Entre `200`, `201` et `204` ?
- Quand une information doit-elle aller dans le chemin (`/items/7`), dans un paramètre (`/items?ownerId=7`), dans le corps de la requête ?
- Pourquoi un serveur ne doit-il pas laisser le client choisir l'`id` d'une ressource qu'il crée ?

Dans cette partie, vous jouez le rôle du développeur d'une application mobile qui consomme votre API, et vous regardez ce qu'elle lui répond **vraiment**. Le code temporaire va dans le package `fr.cours.troc.sandbox.rest`.

### Étape 1 — Voir le code de statut

Dans Bruno, le code s'affiche en haut de la réponse. En ligne de commande, `curl` ne l'affiche qu'avec `-i` :

```bash
curl -i -X POST http://localhost:8080/users -H "Content-Type: application/json" -d '{"name": "Alice"}'
```

```
HTTP/1.1 200
Content-Type: application/json

{"id":1,"name":"Alice"}
```

(sous Windows PowerShell, tapez `curl.exe`). Notez le code d'une création réussie : `200`.

### Étape 2 — Ce que Spring fait déjà pour vous

Sans écrire de code, envoyez :

| Requête | Code reçu | Ce que ça signifie |
|---|---|---|
| `POST /users` **sans** l'en-tête `Content-Type` | | |
| `POST /users` avec un JSON mal formé (virgule en trop) | | |
| `DELETE /users` | | |
| `GET /usrs` | | |

Spring choisit déjà le bon code quand la faute vient du client : `415` (format non supporté), `400` (requête mal formée), `405` (verbe non autorisé sur ce chemin), `404` (chemin inconnu). Le premier chiffre suffit à savoir qui est en faute : `2xx` tout va bien, `4xx` c'est le client, `5xx` c'est le serveur.

### Étape 3 — Un utilisateur qui n'existe pas

Créez une route naïve, qui lit directement dans le repository :

```java
package fr.cours.troc.sandbox.rest;

@RestController
public class NaiveUserController {

    private final UserRepository users;

    public NaiveUserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/sandbox/users/{id}")  // {id} : une variable dans le chemin
    public User findById(@PathVariable Long id) {
        return users.findById(id);      // null si l'utilisateur n'existe pas
    }
}
```

Relancez, créez Alice, puis exécutez **Expérimentation > TP2 - Consulter un utilisateur (naïf)** (`GET /sandbox/users/42`), puis la même requête avec l'id `1` (modifiez l'URL).

- Quel code pour l'utilisateur 42 ? Quel corps ?
- Côté application mobile, comment distinguer « cet utilisateur n'existe pas » de « le serveur a un bug et a renvoyé une réponse vide » ?

### Étape 4 — Le client choisit l'`id`

Créez Alice et Bob, puis exécutez **Expérimentation > TP2 - Créer un utilisateur avec un id** : `POST /users` avec `{"id": 1, "name": "Pirate"}`. Listez les utilisateurs.

Alice a disparu, remplacée par Pirate : le repository a vu un `id` déjà rempli et a écrasé l'utilisateur existant. Un client malveillant (ou simplement maladroit) peut écraser n'importe quelle donnée.

### Ce qu'il faut retenir

- Le **code de statut** est la première chose que lit le client. Une API qui renvoie `200` et un corps vide pour une ressource inconnue l'oblige à deviner.
- C'est le **serveur** qui décide des identifiants, jamais le client.

**Supprimez le package `fr.cours.troc.sandbox`.**

---

## Partie 2 — Mise en place : les utilisateurs et les objets, de bout en bout (obligatoire)

### Objectifs

Tous les endpoints de ce tableau répondent, avec le bon code :

| Verbe | Chemin | Rôle | Code si tout va bien | Codes d'erreur |
|---|---|---|---|---|
| `GET` | `/users` | lister les utilisateurs | `200` | |
| `GET` | `/users/{id}` | consulter un utilisateur | `200` | `404` inconnu |
| `POST` | `/users` | créer un utilisateur | `201` + en-tête `Location` | |
| `PUT` | `/users/{id}` | modifier un utilisateur | `200` | `404` inconnu |
| `DELETE` | `/users/{id}` | supprimer un utilisateur | `204` | `404` inconnu |
| `GET` | `/items` (option `?ownerId=…`) | lister les objets, tous ou ceux d'un utilisateur | `200` | |
| `GET` | `/items/{id}` | consulter un objet | `200` | `404` inconnu |
| `POST` | `/items` | déclarer un objet | `201` + `Location` | `400` quantité ≤ 0, `404` propriétaire inconnu |
| `PUT` | `/items/{id}` | modifier un objet | `200` | `400`, `404` |
| `DELETE` | `/items/{id}` | supprimer un objet | `204` | `404` inconnu |

Règles :

- l'`id` envoyé par le client à la création est **ignoré** : c'est le repository qui l'attribue ;
- un `PUT` prend l'`id` dans le **chemin**, les données dans le **corps** ;
- le message d'erreur (« Utilisateur 42 introuvable ») apparaît dans le corps de la réponse ;
- les décisions métier (`404`, `400`) sont prises dans les **services**, pas dans les contrôleurs.

Les données restent **en mémoire** : la base arrive au TP 5.

> Organisation conseillée pour un groupe de 4 : deux personnes sur `user/`, deux sur `item/` (en commençant par le repository). Le découpage du projet par fonctionnalité s'y prête bien. Mettez-vous d'accord d'abord sur le changement de `findById` (ci-dessous), qui touche les deux.

### Syntaxes utiles

#### Vos réflexes REST, en Spring

| Vous connaissez… | En Spring |
|---|---|
| Associer un verbe et un chemin à un traitement | `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` sur une méthode |
| Un préfixe commun à un groupe de routes | `@RequestMapping("/users")` sur la classe du contrôleur |
| Renvoyer du JSON | renvoyer l'objet Java : Spring le convertit tout seul |
| Les données d'une requête (chemin, paramètre, corps) | `@PathVariable`, `@RequestParam`, `@RequestBody` (détaillés plus bas) |
| Choisir le code de la réponse | `ResponseEntity`, `@ResponseStatus`, `ResponseStatusException` (détaillés plus bas) |

#### Un préfixe commun aux routes d'un contrôleur

```java
@RestController
@RequestMapping("/users")   // préfixe de toutes les méthodes de la classe
public class UserController {

    @GetMapping             // GET /users
    …
    @GetMapping("/{id}")    // GET /users/{id} : chemin de la classe + chemin de la méthode
    …
}
```

#### Récupérer les informations de la requête

| L'information… | Elle va dans… | Syntaxe | Exemple |
|---|---|---|---|
| désigne **une** ressource précise | le chemin | `@PathVariable Long id` (lié à `{id}` par le nom) | `GET /items/7` |
| filtre ou affine une liste | un paramètre | `@RequestParam(required = false) Long ownerId` | `GET /items?ownerId=42` |
| contient les données à enregistrer | le corps | `@RequestBody Item item` | `POST /items` + JSON |

Par défaut, un `@RequestParam` est **obligatoire** : sans `required = false`, `GET /items` sans paramètre répondrait `400`. Absent, il vaut `null`.

#### « Peut-être absent » : `Optional`

`User findById(Long id)` qui renvoie `null` est un piège : rien dans la signature ne prévient l'appelant. Java fournit [`Optional<T>`](https://dev.java/learn/api/streams/optionals/), une boîte qui contient une valeur, ou rien :

```java
Optional<User> findById(Long id);                 // dans l'interface du repository

return Optional.ofNullable(users.get(id));        // vide si la map ne contient pas l'id

repository.findById(id).isEmpty()                 // true si absent
repository.findById(id).orElseThrow(() -> new UneException(…)) // la valeur, ou lève l'exception
```

Changez la signature dans l'interface : le compilateur vous montrera tous les endroits qui doivent traiter le cas « absent ».

#### Choisir le code de la réponse

| Besoin | Outil | Syntaxe |
|---|---|---|
| Interrompre le traitement avec une erreur (`404`, `400`, `409`) | une exception que Spring traduit en réponse | `throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur " + id + " introuvable");` |
| Le code est toujours le même quand tout va bien (`204`) | une annotation sur la méthode | `@ResponseStatus(HttpStatus.NO_CONTENT)` |
| Le code **et** les en-têtes dépendent du résultat (`201` + `Location`) | une réponse construite à la main | `return ResponseEntity.created(URI.create("/users/" + id)).body(saved);` |

`ResponseStatusException` vient de `org.springframework.web.server`, `HttpStatus` et `ResponseEntity` de `org.springframework.http`. Sans rien de tout ça, la réponse est `200`.

Par défaut, Spring Boot **masque** le message de l'exception dans la réponse. Pour l'afficher, ajoutez dans `src/main/resources/application.properties` :

```properties
spring.web.error.include-message=always
```

C'est un réglage de Spring Boot, fait sans toucher au code : la configuration est le sujet du TP 3.

#### Filtrer une collection : les streams

Pour `findByOwnerId` en mémoire, un [stream](https://dev.java/learn/api/streams/) :

```java
return items.values().stream()
        .filter(item -> Objects.equals(item.getOwnerId(), ownerId))
        .toList();
```

`Objects.equals` compare deux `Long` sans risque de `NullPointerException`. Ne comparez **jamais** deux `Long` avec `==` : cela compare les objets, pas les valeurs, et donne des résultats faux au-delà de 127.

Nommez les méthodes de recherche `findByOwnerId`, `countByOwnerId`… : au TP 5, Spring Data saura écrire ces requêtes tout seul à partir de leur nom.

### Indices

- Une méthode `findById` dans chaque service, qui lève le `404`, peut être réutilisée par `update` et `delete` (« l'utilisateur existe-t-il ? »).
- Les vérifications communes à `create` et `update` d'un objet peuvent aller dans une méthode privée.
- `ItemService` vérifiait déjà le propriétaire avec une `IllegalArgumentException` : quel code HTTP Spring renvoie-t-il pour cette exception ? Remplacez-la.
- Pour supprimer, ajoutez `deleteById` à l'interface du repository **et** à toutes ses implémentations.

### Vérifier

Dans Bruno, les dossiers **Utilisateurs** et **Objets** contiennent une requête pour chaque endpoint. Rejouez-les dans l'ordre (créez d'abord les utilisateurs 1 et 2), et comparez **le code** de chaque réponse avec le tableau des objectifs, pas seulement le JSON. Provoquez aussi chaque erreur, en modifiant le corps de **Créer objet (Alice)** : `"quantity": -1` → `400`, `"ownerId": 99` → `404`.

- [ ] Chaque ligne du tableau des objectifs répond avec le bon code, y compris les cas d'erreur.
- [ ] `POST /users` renvoie un en-tête `Location: /users/{id}` (onglet **Headers** de la réponse dans Bruno).
- [ ] `{"id": 1, "name": "Pirate"}` envoyé en `POST /users` crée un **nouvel** utilisateur, sans écraser Alice.
- [ ] `GET /users/42` répond `404`, avec le message dans le corps.
- [ ] `GET /items` (sans paramètre) et `GET /items?ownerId=1` répondent tous les deux.
- [ ] Aucun contrôleur ne contient de `if` sur les données : les règles sont dans les services.

> **Question pour le checkpoint** : on supprime un utilisateur qui possède des objets. Que deviennent ses objets ? Ici, rien : ils restent, avec un `ownerId` qui ne désigne plus personne. Au TP 5, la base de données refusera cette suppression grâce à une **clé étrangère**.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| `415 Unsupported Media Type` | En-tête `Content-Type: application/json` oublié. |
| `400 Bad Request` | JSON mal formé, ou `/items/abc` au lieu d'un nombre. La console de l'application indique la raison précise. |
| `405 Method Not Allowed` | Bon chemin, mauvais verbe : la méthode `@PutMapping` (par exemple) n'existe pas encore. |
| `404` sur une route qui devrait exister | Chemin mal recopié, ou `/users` à la fois dans `@RequestMapping` et dans `@GetMapping("/users")` (ce qui donne `/users/users`). |
| `400` sur `GET /items` sans paramètre | `required = false` oublié sur le `@RequestParam`. |
| `500` au lieu de `404` ou `400` | Une exception Java ordinaire (`IllegalArgumentException`, `NullPointerException`) sort du service : Spring ne sait pas la traduire. Utilisez `ResponseStatusException`. |
| Un champ arrive à `null` (ou `0`) | Clé JSON différente du nom Java (`"owner"` au lieu de `"ownerId"`), ou setter manquant. |
| Le message d'erreur n'apparaît pas dans la réponse | `spring.web.error.include-message=always` absent d'`application.properties`. |
| `does not override abstract method deleteById` | Une méthode ajoutée à l'interface n'est pas implémentée dans **toutes** ses implémentations. |

## Bonus

Le CRUD étant un terrain connu, la plupart des groupes devraient atteindre au moins le bonus 1 pendant la séance.

### Bonus 1 — Centraliser la gestion des erreurs : `@RestControllerAdvice`

`ResponseStatusException` dans un service mélange deux couches : le service, qui porte le métier, connaît les codes HTTP. Plus propre : le service lève une exception **métier** (`NotFoundException extends RuntimeException`, dans un package `fr.cours.troc.common`), et un composant de la couche web la traduit en réponse :

```java
@RestControllerAdvice // s'applique à tous les contrôleurs
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }
}
```

`ProblemDetail` produit un corps d'erreur au format standard [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457). Faites de même pour `400` et `409`. Les services ne dépendent plus de rien de « web ». Documentation : [Spring Framework — Exceptions](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html).

### Bonus 2 — Valider les entrées : `@Valid`

Plutôt que de tester chaque champ à la main, déclarez les contraintes sur la classe :

1. Ajoutez la dépendance `org.springframework.boot:spring-boot-starter-validation` dans `pom.xml`, et rechargez Maven.
2. Annotez les champs d'`Item` (package `jakarta.validation.constraints`) : `@NotBlank` sur `name`, `@Positive` sur `quantity`, `@NotNull` sur `ownerId`.
3. Demandez la validation dans le contrôleur : `create(@Valid @RequestBody Item item)`.

Un objet sans nom ou avec une quantité négative est refusé en `400` avant même d'entrer dans le service. Documentation : [Spring Guide — Validating Form Input](https://spring.io/guides/gs/validating-form-input) et [la liste des contraintes](https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/constraints/package-summary).

## Pour aller plus loin

- [Spring Framework — Annotated Controllers](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html) (`@PathVariable`, `@RequestParam`, `@RequestBody`, `ResponseEntity`)
- [MDN — Codes de statut HTTP](https://developer.mozilla.org/fr/docs/Web/HTTP/Status) et [Méthodes de requête HTTP](https://developer.mozilla.org/fr/docs/Web/HTTP/Methods)
- [Microsoft — Conception d'API web RESTful](https://learn.microsoft.com/fr-fr/azure/architecture/best-practices/api-design) (nommage des ressources, codes de statut)
