# 🌐 Fiche 2 — HTTP et REST

> `TP N` = notion vue à partir du TP N · ✅ à faire · ❌ à éviter · ⚠️ piège · 💡 astuce

## 🔤 Verbes · `TP 2`

| Verbe | Rôle | 🛡️ Sûr | 🔁 Idempotent | 📦 Corps | troc-api |
|---|---|:-:|:-:|:-:|---|
| 📖 `GET` | lire | ✅ | ✅ | ❌ | `GET /users/1` |
| ➕ `POST` | créer / action | ❌ | ❌ | ✅ | `POST /items` |
| ♻️ `PUT` | remplacer **tout** | ❌ | ✅ | ✅ | `PUT /items/3` |
| ✏️ `PATCH` | modifier **une partie** | ❌ | ⚠️ | ✅ | *non utilisé* |
| 🗑️ `DELETE` | supprimer | ❌ | ✅ | ❌ | `DELETE /users/2` |

🛡️ Sûr = ne modifie rien

🔁 Idempotent = Effectuer N fois l'action donne le même état que l'effectuer 1 fois (par opposition 2 `POST` = 2 objets)

⚠️ `PUT` : champ absent = effacé / `PATCH` : seulement les champs envoyés

## 🛣️ Chemins · `TP 2`

💡 **Principe :** le chemin désigne **quoi** (une ressource, avec un nom), le verbe dit **quoi faire**. L'action n'est jamais dans le chemin.

**Anatomie d'une requête :**

```
 PUT    /items     /3     ?ownerId=1      {"name": "Vélo", "quantity": 2, "ownerId": 1}
 ───    ──────     ──     ──────────      ──────────────────────────────────────────────
 🔤       📚       🎯         🔎                            📦
verbe  collection  id   filtre (option)              corps JSON (données)
```

| Élément | Rôle | troc-api |
|---|---|---|
| 📚 collection | **toutes** les ressources d'un type, nom au **pluriel** | `GET /items` liste · `POST /items` ajoute |
| 🎯 `/{id}` | **une** ressource précise | `GET`, `PUT`, `DELETE /items/3` |
| 🔎 `?clé=valeur` | **filtre** facultatif sur une liste | `GET /items?ownerId=1` · `GET /trades?userId=2` |
| 📦 corps | les **données** à enregistrer (`POST`, `PUT`) | `{"name": "Vélo", …}` |
| ⚡ `/{id}/action` | action métier qui n'est ni créer, ni remplacer, ni supprimer · `TP 6` | `POST /trades/5/accept` · `/reject` |

**Un chemin, plusieurs verbes :**

| | 📚 `/items` | 🎯 `/items/3` |
|---|---|---|
| 📖 `GET` | liste des objets | l'objet 3 |
| ➕ `POST` | crée un objet | ❌ |
| ♻️ `PUT` | ❌ | remplace l'objet 3 |
| 🗑️ `DELETE` | ❌ | supprime l'objet 3 |

| ✅ A faire | ❌ A éviter | Pourquoi |
|---|---|---|
| `GET /users` | `GET /getUsers` | le verbe dit déjà « lire » |
| `GET /items/7` | `GET /items?id=7` | un id désigne **une** ressource, `?` sert à filtrer |
| `GET /items?ownerId=1` | `GET /items/owner/1` | critère facultatif → paramètre |
| `PUT /users/1` | id seulement dans le corps | ⚠️ l'id du **chemin** fait foi |
| `POST /trades/5/accept` | `POST /acceptTrade?id=5` | l'action porte sur **le troc 5** |

## 🚦 Codes de statut

| Code | Sens | troc-api | Spring | TP |
|---|---|---|---|:-:|
| ✅ `200` OK | succès | `GET`, `PUT`, accepter | par défaut | 2 |
| ✅ `201` Created | créé | `POST /users`, `/items`, `/trades` | `ResponseEntity.created(uri).body(x)` | 2 |
| ✅ `204` No Content | succès sans corps | `DELETE /items/{id}` | `@ResponseStatus(NO_CONTENT)` | 2 |
| ⚠️ `400` Bad Request | requête fausse **en soi** | quantité ≤ 0, troc avec soi-même | `ResponseStatusException(BAD_REQUEST, "…")` | 2 |
| 🔑 `401` Unauthorized | 👤❓ qui êtes-vous ? | sans jeton / jeton invalide | Spring Security | 9 |
| 🚫 `403` Forbidden | 👤✅ authentifié mais ⛔ pas le droit | accepter le troc d'un autre | `FORBIDDEN` | 6 |
| 🔍 `404` Not Found | inconnu | `GET /users/42` | `NOT_FOUND` | 2 |
| 💢 `409` Conflict | valide, mais **l'état** bloque | stock insuffisant, troc déjà traité | `CONFLICT` | 2 |
| 💥 `500` Server Error | bug | exception non traduite | ❌ à éviter | 2 |

💡 **400 ou 409 ?** Avec d'autres données en base, la requête aurait réussi ? → `409`. Sinon → `400`. (`422` : discuté au `TP 4`)

`1xx` ℹ️ info · `2xx` ✅ succès · `3xx` ↪️ redirection · `4xx` 🙋 faute du client · `5xx` 💥 faute du serveur

### 🤖 Codes renvoyés par Spring tout seul

| Code | 🔎 Cause |
|---|---|
| `400` | JSON mal formé · `/items/abc` · `@RequestParam` obligatoire absent |
| `404` | chemin mal recopié · préfixe en double (`/users/users`) |
| `405` | bon chemin, mauvais verbe |
| `415` | `Content-Type: application/json` manquant |

## 🎛️ Contrôleur · `TP 2`

```java
@RestController
@RequestMapping("/items")                                   // préfixe commun à toutes les routes
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping                                             // GET /items?ownerId=1
    public List<Item> findAll(@RequestParam(required = false) Long ownerId) {
        return itemService.findAll(ownerId);
    }

    @GetMapping("/{id}")                                    // GET /items/7
    public Item findById(@PathVariable Long id) {
        return itemService.findById(id);
    }

    @PostMapping                                            // POST /items + JSON → 201
    public ResponseEntity<Item> create(@RequestBody Item item) {
        Item saved = itemService.create(item);
        return ResponseEntity.created(URI.create("/items/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")                                    // PUT /items/7 + JSON → 200
    public Item update(@PathVariable Long id, @RequestBody Item item) {
        return itemService.update(id, item);
    }

    @DeleteMapping("/{id}")                                 // DELETE /items/7 → 204
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        itemService.delete(id);
    }
}
```

| Annotation | Lit | ⚠️ |
|---|---|---|
| `@PathVariable` | `/{id}` | lié par le nom |
| `@RequestParam` | `?ownerId=` | obligatoire par défaut |
| `@RequestBody` | corps JSON | Jackson → setters |

- ✅ décisions métier (`400`, `404`, `409`) dans le **service** · ❌ pas de `if` sur les données dans le contrôleur
- 📘 `Annexe 1` : `@Operation(summary = "…")`, `@ApiResponse` → Swagger UI (fiche 9)

### 💬 Afficher le message d'erreur · `TP 2`

Le texte passé à `new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur 42 introuvable")` doit arriver dans le champ `"message"` du JSON d'erreur. ⚠️ Par défaut, Spring Boot le **masque** : le champ est absent de la réponse.

| ❌ Sans réglage | ✅ Avec le réglage |
|---|---|
| `{"status": 404, "error": "Not Found", "path": "/users/42", …}` | `{"status": 404, "error": "Not Found", "message": "Utilisateur 42 introuvable", "path": "/users/42", …}` |

✅ Ajouter dans `src/main/resources/application.properties` :

```properties
spring.web.error.include-message=always
```

## ⌨️ IntelliJ · `TP 2`

⭐ = aide Spring, IntelliJ **Ultimate** (licence étudiante gratuite) · raccourcis Windows / Linux ; macOS et liste générale : [fiche 10](10-raccourcis-intellij.md)

| Raccourci | Effet |
|---|---|
| ⭐ **View > Tool Windows > Endpoints** | toutes les routes de l'API, double-clic → la méthode du contrôleur |
| `Ctrl+Shift+F` `"/items"` | retrouver la méthode d'un chemin (sans ⭐) |
| `HttpStatus.` + `Ctrl+Espace` | tous les codes par leur nom (`CONFLICT`, `NOT_FOUND`) |
| `Ctrl+P` dans `new ResponseStatusException(` | les paramètres attendus |
| `Ctrl+Q` sur une annotation (`@PathVariable`) | sa documentation, sans quitter le code |

## 🔗 Pour aller plus loin

[MDN — Méthodes](https://developer.mozilla.org/fr/docs/Web/HTTP/Methods) · [MDN — Codes](https://developer.mozilla.org/fr/docs/Web/HTTP/Status) · [Spring — Contrôleurs](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html)
