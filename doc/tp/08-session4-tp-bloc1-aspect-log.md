# Session 4 — TP bloc 1 : tracer chaque troc avec un aspect

## Objectif

Depuis la séance 3, un troc peut être proposé, accepté ou refusé. Mais si un utilisateur demande « pourquoi mon troc d'hier a-t-il échoué ? », personne ne peut répondre : l'application ne garde **aucune trace** de ce qui s'est passé.

Dans ce bloc, on fait raconter à la console chaque tentative de troc, réussie ou non, **sans modifier une seule ligne de `TradeService`** :

```
→ propose [ProposeTradeRequest[proposerId=1, offeredItemId=1, offeredQuantity=3, recipientId=2, requestedItemId=2, requestedQuantity=4]]
✓ propose : troc 1 PROPOSED
→ accept [1, 2]
✓ accept : troc 1 EXECUTED
→ accept [2, 2]
✗ accept : 409 CONFLICT "Stock insuffisant : l'objet 2 n'a plus que 1 unités"
→ reject [2, 1]
✗ reject : 403 FORBIDDEN "Seul le destinataire (utilisateur 2) peut répondre à ce troc"
```

**Objectif obligatoire de fin de bloc** :

1. Une classe `TradeLoggingAspect` écrit une ligne **avant** chaque appel à une méthode de `TradeService`, une ligne en cas de **succès**, une ligne en cas d'**échec**.
2. `TradeService` est **identique** à celui de la fin de séance 3.
3. Le scénario de l'étape 6 produit dans la console une histoire lisible, ligne par ligne.

**Point de départ** : le projet de fin de séance 3. Si `accept` ne fonctionne pas encore chez vous, ce n'est pas bloquant : l'aspect se branche sur **toutes** les méthodes de `TradeService`, `propose` suffit pour le voir à l'œuvre.

> **Séance dense** : ce bloc est le seul point non négociable de la journée. Le bloc 2 (les tests) vient ensuite. Si le temps manque, c'est le bloc 2 qu'on raccourcit, jamais celui-ci.

> Organisation conseillée pour un groupe de 4 : deux personnes sur l'aspect (étapes 1 à 6) ; les deux autres lisent dès maintenant les étapes 1 et 2 du bloc 2 (préparer la base de test) pour démarrer les tests dès le checkpoint.

## Étape 1 — Le problème : le log mélangé au métier

Avant d'écrire un aspect, regardons ce qu'il faudrait faire **sans**. Pour tracer chaque acceptation, `accept` deviendrait :

```java
@Transactional
public Trade accept(Long tradeId, Long userId) {
    log.info("accept {} by {}", tradeId, userId);
    try {
        Trade trade = findById(tradeId);
        checkAcceptable(trade, userId);
        // … les lignes métier de la séance 3 …
        log.info("trade {} executed", tradeId);
        return trades.save(trade);
    } catch (RuntimeException e) {
        log.warn("trade {} : {}", tradeId, e.getMessage());
        throw e;
    }
}
```

Ne l'écrivez pas : regardez seulement ce que ça coûte.

- **Le métier est noyé** : près de la moitié des lignes ne parlent plus de troc. Pour relire la règle d'acceptation, il faut trier.
- **Il faut le recopier partout** : dans `propose`, `reject`, puis dans `UserService`, `ItemService`… Dans une vraie équipe, c'est le genre de copier-coller où une méthode finit par être oubliée : le jour d'un incident en production, c'est justement elle qu'on ne voit pas dans les logs.
- **Changer le format coûte cher** : l'équipe d'exploitation veut ajouter l'heure de début et la durée de chaque opération ? Il faut modifier chaque méthode de chaque service, et retester tout le métier qu'on a touché au passage.
- **Le service a deux raisons de changer** : les règles du troc, et le format des logs. C'est contraire au **S** de SOLID (séance 1) : une classe, une seule raison de changer.

La journalisation est une **préoccupation transverse** : elle concerne tous les services de la même façon, sans faire partie du métier d'aucun. Les transactions en sont une autre : en séance 3, on les a réglées avec une seule annotation, `@Transactional`, sans écrire de `BEGIN` ni de `COMMIT` dans le service. Aujourd'hui, on fait la même chose pour les logs, en écrivant notre propre **aspect**.

## Étape 2 — La dépendance

Ajoutez dans `pom.xml`, sous les dépendances de la séance 3 :

```xml
<!-- Séance 4 : AOP, la programmation orientée aspect -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aspectj</artifactId>
</dependency>
```

Puis rechargez Maven (**Load Maven Changes**).

> Dans beaucoup de tutoriels, ce starter s'appelle `spring-boot-starter-aop` : c'est son ancien nom, renommé en Spring Boot 4. Avec Spring Boot 4, `spring-boot-starter-aop` est introuvable.

Ce starter apporte les annotations `@Aspect`, `@Before`, etc. Elles viennent d'[AspectJ](https://eclipse.dev/aspectj/), un outil d'AOP plus ancien que Spring, dont Spring réutilise la syntaxe. Le mécanisme, lui, reste celui de Spring : le proxy, vu en séance 3.

## Étape 3 — Un vrai logger plutôt que `System.out.println`

Jusqu'ici, on affichait avec `System.out.println`. Pour des traces destinées à durer, on utilise un **logger** : SLF4J, déjà fourni par Spring Boot (c'est lui qui affiche les lignes de démarrage, et le SQL depuis la séance 3).

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(MaClasse.class);

log.info("→ {} {}", "accept", 7); // affiche « → accept 7 »
```

| | `System.out.println` | `log.info(…)` |
|---|---|---|
| Date, heure, thread, classe d'origine | non | oui, sur chaque ligne |
| Niveau d'importance | non | `debug`, `info`, `warn`, `error` |
| Désactiver ou filtrer sans toucher au code | non | oui, dans `application.properties` |
| Envoyer vers un fichier, un outil de supervision | non | oui, par configuration |

- Les `{}` sont remplacés, dans l'ordre, par les arguments qui suivent. Pas de concaténation avec `+` à écrire.
- Le niveau sert à trier : `info` pour ce qui se passe normalement, `warn` pour une anomalie qui n'empêche pas l'application de tourner (un troc refusé), `error` pour une panne.
- Vous avez déjà réglé un niveau sans le savoir : `logging.level.org.springframework.jdbc.core.JdbcTemplate=DEBUG` (séance 3) affiche les messages `debug` de cette classe-là, et seulement d'elle.

## Étape 4 — Premier aspect : tracer chaque appel

### 4.1 — Quatre mots de vocabulaire

| Terme | Sens | Dans notre cas |
|---|---|---|
| **Join point** | un endroit du programme qu'on peut intercepter ; avec Spring, c'est toujours **l'appel d'une méthode d'un bean** | l'appel de `accept(1, 2)` |
| **Pointcut** | le **filtre** qui choisit les join points : *où* agir | « toutes les méthodes de `TradeService` » |
| **Advice** | le code à exécuter, et *quand* : avant, après, en cas d'erreur… | « écrire une ligne de log » |
| **Aspect** | la classe qui regroupe pointcuts et advices | `TradeLoggingAspect` |

En une phrase : « **avant** (advice) chaque méthode de `TradeService` (pointcut), écrire une ligne de log ».

### 4.2 — La classe

Créez `TradeLoggingAspect` dans le package `fr.cours.troc.trade` :

```java
package fr.cours.troc.trade;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

// La journalisation des trocs, écrite une fois, hors de TradeService.
@Aspect     // cette classe contient des advices
@Component  // et c'est un bean, comme les autres : sinon Spring ne la connaît pas
public class TradeLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(TradeLoggingAspect.class);

    // Toutes les méthodes publiques de TradeService, quels que soient le retour et les arguments.
    @Pointcut("execution(* fr.cours.troc.trade.TradeService.*(..))")
    void tradeOperations() {
    }

    @Before("tradeOperations()")
    public void logAttempt(JoinPoint jp) {
        log.info("→ {} {}", jp.getSignature().getName(), Arrays.toString(jp.getArgs()));
    }
}
```

**Le pointcut**, de gauche à droite :

```
execution(  *                 fr.cours.troc.trade.TradeService  .*         (..)  )
            type de retour :  la classe, avec son package        méthode :  arguments :
            n'importe lequel  complet                            n'importe  n'importe lesquels
```

`@Pointcut` donne un **nom** au filtre (`tradeOperations()`). La méthode qui le porte reste vide : elle ne sert que d'étiquette, réutilisable par tous les advices de la classe.

**L'advice** : `@Before("tradeOperations()")` dit « avant chaque join point choisi par ce filtre ». Spring passe en paramètre un `JoinPoint`, qui décrit l'appel intercepté :

- `jp.getSignature().getName()` : le nom de la méthode appelée (`accept`) ;
- `jp.getArgs()` : les arguments reçus, dans un tableau (`[1, 2]`). `Arrays.toString` l'affiche lisiblement.

### 4.3 — Vérifier

Relancez l'application. Dans Bruno, exécutez **Trocs > Lister trocs d'un utilisateur**, puis **Trocs > Proposer troc**. Dans la console :

```
… INFO … fr.cours.troc.trade.TradeLoggingAspect : → findAll [1]
… INFO … fr.cours.troc.trade.TradeLoggingAspect : → propose [ProposeTradeRequest[proposerId=1, …]]
```

Rien n'apparaît ? Ne cherchez pas plus loin que le tableau des [erreurs fréquentes](#erreurs-fréquentes) : dans neuf cas sur dix, c'est une faute de frappe dans le pointcut.

> `ProposeTradeRequest` s'affiche en entier parce que c'est un `record` : Java lui écrit automatiquement une méthode `toString()` qui liste ses champs. Un objet ordinaire, comme `Trade`, afficherait seulement `fr.cours.troc.trade.Trade@1b2c3d`.

## Étape 5 — Tracer les échecs et les succès

### 5.1 — En cas d'échec : `@AfterThrowing`

Ajoutez à l'aspect :

```java
@AfterThrowing(pointcut = "tradeOperations()", throwing = "ex")
public void logFailure(JoinPoint jp, RuntimeException ex) {
    log.warn("✗ {} : {}", jp.getSignature().getName(), ex.getMessage());
}
```

(import : `org.aspectj.lang.annotation.AfterThrowing`)

- `throwing = "ex"` relie l'exception levée au paramètre nommé `ex`. Le nom dans l'annotation et celui du paramètre doivent être **identiques**.
- Le type du paramètre filtre aussi : `RuntimeException` attrape les `ResponseStatusException` (403, 404, 409…) et les erreurs de la base.
- L'advice **observe** l'exception, il ne l'attrape pas : elle continue sa route jusqu'au client, qui reçoit toujours son `409`. Et la transaction de `accept` voit toujours passer l'exception : le `ROLLBACK` de la séance 3 a bien lieu.

Testez : **Trocs > Accepter troc (pas le destinataire)** doit produire une ligne `WARN` avec `403 FORBIDDEN`.

### 5.2 — En cas de succès : `@AfterReturning`

À vous. Écrivez un advice `logSuccess` qui, après un appel **réussi**, affiche le nom de la méthode et, si elle a renvoyé un troc, son id et son statut :

```
✓ accept : troc 1 EXECUTED
✓ findAll
```

Indices :

- l'annotation `@AfterReturning` fonctionne comme `@AfterThrowing`, avec `returning = "result"` au lieu de `throwing` ;
- le paramètre correspondant peut être de type `Object` : il reçoit ce que la méthode a renvoyé, quel qu'il soit ;
- toutes les méthodes ne renvoient pas un `Trade` : `findAll` renvoie une liste. Testez le type avec `instanceof`.

<details>
<summary>Solution : logSuccess</summary>

```java
@AfterReturning(pointcut = "tradeOperations()", returning = "result")
public void logSuccess(JoinPoint jp, Object result) {
    // findAll renvoie une liste : on ne détaille que les méthodes qui renvoient un troc
    if (result instanceof Trade trade) {
        log.info("✓ {} : troc {} {}", jp.getSignature().getName(), trade.getId(), trade.getStatus());
    } else {
        log.info("✓ {}", jp.getSignature().getName());
    }
}
```

`result instanceof Trade trade` teste le type **et**, si c'est bien un `Trade`, le range dans une variable `trade` déjà convertie : pas besoin d'écrire `(Trade) result`.

</details>

### 5.3 — Quand chaque advice s'exécute-t-il ?

```
TradeController ──accept(1, 2)──▶ ┌────────── proxy généré par Spring ──────────┐
                                  │ @Before          → accept [1, 2]            │
                                  │      TradeService.accept(…)  (inchangé)     │
                                  │ @AfterReturning  → ✓ si la méthode a rendu  │
                                  │ @AfterThrowing   → ✗ si elle a levé         │
                                  └─────────────────────────────────────────────┘
```

C'est le **même proxy** que celui de `@Transactional` en séance 3 (étape 6.2) : le conteneur IoC injecte dans `TradeController` un emballage autour de `TradeService`, et cet emballage a maintenant deux couches, la transaction et nos logs.

Il existe aussi `@After` (sans suffixe), exécuté dans les deux cas, comme un `finally`, et `@Around`, qui englobe tout l'appel : voir le bonus 12.

## Étape 6 — Lire l'histoire

Repartez d'une base propre (arrêtez l'application, supprimez `data/`, relancez). Dans Bruno :

1. **Créer utilisateur** (Alice), **Créer un 2e utilisateur** (Bob) ;
2. **Créer objet (Alice)** (10 tomates), **Créer objet (Bob)** (5 carottes) ;
3. **Proposer troc**, deux fois (trocs 1 et 2) ;
4. **Accepter troc** sur le troc 1 ;
5. **Accepter troc** sur le troc 2 (modifiez l'URL : `/trades/2/accept`) : Bob n'a plus qu'une carotte ;
6. **Refuser troc** sur le troc 2, avec `"userId": 1` ;
7. **Consulter troc** (`GET /trades/1`).

La console doit raconter, au milieu des lignes SQL :

```
→ propose [ProposeTradeRequest[…]]
✓ propose : troc 1 PROPOSED
→ propose [ProposeTradeRequest[…]]
✓ propose : troc 2 PROPOSED
→ accept [1, 2]
✓ accept : troc 1 EXECUTED
→ accept [2, 2]
✗ accept : 409 CONFLICT "Stock insuffisant : l'objet 2 n'a plus que 1 unités"
→ reject [2, 1]
✗ reject : 403 FORBIDDEN "Seul le destinataire (utilisateur 2) peut répondre à ce troc"
→ findById [1]
✓ findById : troc 1 EXECUTED
```

Répondez, par écrit :

1. `TradeService` a-t-il changé depuis la séance 3 ? Vérifiez avec git (`git diff` sur le fichier, ou l'onglet **Commit** d'IntelliJ).
2. `accept` appelle `findById` dès sa première ligne. Pourquoi ne voit-on **pas** de ligne `→ findById` pendant un `accept`, alors qu'on en voit une à l'étape 7 ? Et pourquoi aucune ligne pour `checkAcceptable` ou `checkStock` ?
3. Comment feriez-vous pour journaliser aussi `ItemService` ? Que faudrait-il changer dans `TradeService` ou `ItemService` ?

<details>
<summary>Réponses</summary>

1. Non : c'est tout l'intérêt. Le format des logs peut changer sans qu'on touche au métier.
2. Le proxy n'intercepte que les appels qui **passent par lui**, c'est-à-dire ceux qui viennent d'un autre bean (ici, le contrôleur). Quand `accept` appelle `findById`, c'est un appel interne (`this.findById(…)`) : il reste dans l'objet `TradeService` et ne traverse pas le proxy. C'est exactement le premier piège de `@Transactional` (séance 3, étape 6.3). Quant à `checkAcceptable` et `checkStock`, elles sont `private` : jamais interceptées. En pratique, c'est le bon niveau de détail : on trace les opérations demandées par le client, pas chaque étape interne.
3. Rien à changer dans les services : il suffit d'un pointcut plus large dans l'aspect (voir le bonus 14).

</details>

## Avant le checkpoint : la liste de vérification

- [ ] `pom.xml` contient `spring-boot-starter-aspectj`.
- [ ] `TradeLoggingAspect` porte `@Aspect` **et** `@Component`.
- [ ] Le scénario de l'étape 6 produit les 12 lignes attendues (`→`, `✓`, `✗`).
- [ ] Les échecs sont en `WARN`, les appels et les succès en `INFO`.
- [ ] Le client reçoit toujours les mêmes codes qu'en séance 3 (`409`, `403`…) : l'aspect observe, il ne change rien.
- [ ] `TradeService` n'a pas été modifié.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| Aucune ligne de l'aspect, aucune erreur au démarrage | **Faute dans le pointcut** : package ou nom de classe mal recopié, majuscule oubliée (`tradeService`). Spring ne prévient pas : un filtre qui ne trouve rien n'est pas une erreur pour lui. Recopiez le nom complet de la classe depuis IntelliJ (clic droit sur la classe > **Copy Reference**). |
| Aucune ligne de l'aspect, et le pointcut est juste | `@Component` oublié : l'aspect n'est pas un bean, Spring ne le connaît pas. |
| `Cannot resolve symbol 'Aspect'` | Dépendance `spring-boot-starter-aspectj` absente, ou Maven pas rechargé. |
| `spring-boot-starter-aop` introuvable dans Maven | Ancien nom du starter : en Spring Boot 4, c'est `spring-boot-starter-aspectj`. |
| Le `JoinPoint` vient du mauvais package | Il faut `org.aspectj.lang.JoinPoint`, pas une autre classe du même nom proposée par l'auto-complétion. |
| Les flèches s'affichent `?` ou `â†'` dans la console | Encodage de la console Windows. Dans IntelliJ, **Settings > Editor > General > Console** : encodage `UTF-8`. Ou remplacez les symboles par `>`, `OK`, `KO`. |
| Le client ne reçoit plus d'erreur, mais un `200` | L'advice attrape l'exception au lieu de l'observer : `@AfterThrowing` ne doit contenir ni `try`/`catch`, ni `return`. |
| On ne voit pas de `→ findById` pendant `accept` | Normal : appel interne, il ne passe pas par le proxy (étape 6, question 2). |

Pour vérifier qu'un advice est bien appelé, le plus sûr reste un **point d'arrêt** dans sa méthode, avec l'application lancée en mode debug (icône d'insecte d'IntelliJ).

## En avance ? Features bonus

### Bonus 12 — Chronométrer avec `@Around`

`@Around` est le plus puissant des advices : il **englobe** l'appel, et c'est lui qui décide d'appeler (ou non) la vraie méthode.

```java
@Around("tradeOperations()")
public Object timed(ProceedingJoinPoint jp) throws Throwable {
    long start = System.nanoTime();
    try {
        return jp.proceed();                    // appelle la vraie méthode, et renvoie son résultat
    } finally {
        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("⏱ {} : {} ms", jp.getSignature().getName(), ms);
    }
}
```

(imports : `org.aspectj.lang.ProceedingJoinPoint`, `org.aspectj.lang.annotation.Around`)

1. Ajoutez-le, et relancez le scénario de l'étape 6. Quelle opération est la plus lente ? Pourquoi la première requête est-elle souvent bien plus lente que les suivantes ?
2. Mettez **temporairement** `jp.proceed()` en commentaire (avec `return null;`). Que se passe-t-il quand on propose un troc ? Que reçoit le client, qu'y a-t-il en base ?
3. `@Transactional` est écrit exactement comme ça, par Spring : décrivez en trois lignes ce que contient son `@Around` (indice : `BEGIN`, `proceed()`, puis…).

> Oublier `proceed()`, c'est empêcher la méthode de s'exécuter, **sans aucune erreur**. `@Around` peut aussi modifier les arguments, la valeur renvoyée, ou avaler une exception : à réserver aux cas où les autres advices ne suffisent pas.

### Bonus 13 — Choisir les méthodes chronométrées avec une annotation

Plutôt que de chronométrer tout `TradeService`, on veut choisir méthode par méthode, en posant une annotation maison `@Timed`.

1. Créez l'annotation dans `fr.cours.troc` :

   ```java
   @Target(ElementType.METHOD)          // se pose sur une méthode
   @Retention(RetentionPolicy.RUNTIME)  // reste visible pendant l'exécution, pour que Spring la lise
   public @interface Timed {
   }
   ```

2. Changez le pointcut de `timed` en `@Around("@annotation(fr.cours.troc.Timed)")`, et déplacez cet advice dans un aspect à part, `TimingAspect`, dans le package `fr.cours.troc`.
3. Posez `@Timed` sur `TradeService.accept` et `ItemService.create` : seules ces deux méthodes sont chronométrées, dans deux services différents.

C'est le même principe que `@Transactional` : une annotation qui ne fait rien seule, et un aspect qui la cherche.

### Bonus 14 — Journaliser tous les services

1. Élargissez le pointcut à tous les services : `execution(* fr.cours.troc..*Service.*(..))` (`..` entre deux packages : n'importe quel sous-package). Déplacez l'aspect dans `fr.cours.troc`, renommez-le `ServiceLoggingAspect`.
2. Ajoutez le nom de la classe dans chaque ligne : `jp.getSignature().getDeclaringType().getSimpleName()`.
3. Proposez un troc : combien de lignes apparaissent maintenant ? `propose` appelle `userService.findById` et `itemService.findById` : pourquoi ces appels-là sont-ils interceptés, alors que le `findById` interne de l'étape 6 ne l'était pas ?
4. **Données sensibles** : si un jour `User` a un champ `password`, que se passe-t-il avec `Arrays.toString(jp.getArgs())` sur `create` ? Dans une vraie application, les logs sont lus par beaucoup de monde (exploitation, support, outils externes) : un mot de passe ou un numéro de carte n'y a jamais sa place. Comment l'éviter ?

## Pour aller plus loin

- [Spring Framework — Programmation orientée aspect avec Spring](https://docs.spring.io/spring-framework/reference/core/aop.html), en particulier [les pointcuts](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/pointcuts.html), [les advices](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/advice.html) et [le fonctionnement par proxy](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)
- [Spring Boot — Logging](https://docs.spring.io/spring-boot/reference/features/logging.html) (niveaux, format, écrire dans un fichier)
- [SLF4J — Manuel](https://www.slf4j.org/manual.html)
- [Baeldung — Introduction to Spring AOP](https://www.baeldung.com/spring-aop) et [Implementing a Custom Spring AOP Annotation](https://www.baeldung.com/spring-aop-annotation) (en anglais, pour le bonus 13)
- [Wikipédia — Programmation orientée aspect](https://fr.wikipedia.org/wiki/Programmation_orient%C3%A9e_aspect)
