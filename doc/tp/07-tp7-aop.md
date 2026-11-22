# TP 7 — La programmation orientée aspect (AOP) : tracer chaque troc

> Rythme indicatif : séance 4. Point de départ : la mise en place du TP 6 terminée. Si `accept` ne fonctionne pas encore chez vous, ce n'est pas bloquant : l'aspect se branche sur toutes les méthodes de `TradeService`, `propose` suffit pour le voir à l'œuvre.

## Le concept en bref

Certaines préoccupations concernent **toutes** les classes de la même façon, sans faire partie du métier d'aucune : journaliser, mesurer les temps de réponse, ouvrir une transaction, vérifier des droits. On les appelle des **préoccupations transverses**. Écrites à la main, elles se recopient dans chaque méthode et noient le code métier.

La **programmation orientée aspect** (AOP) les écrit **une seule fois**, dans une classe à part (un *aspect*), et les applique automatiquement aux méthodes choisies. Vous en utilisez déjà une sans le savoir : `@Transactional`.

---

## Partie 1 — Expérimentation : un aspect que vous utilisez déjà (facultative)

**Vous pouvez passer à la partie 2 si vous savez répondre à ces questions :**

- Qu'est-ce qu'une préoccupation transverse ? Donnez deux exemples.
- Comment Spring parvient-il à exécuter du code avant et après une méthode, sans modifier sa classe ?
- Pourquoi un appel interne (`this.methode()`) n'est-il jamais intercepté ?

Cette partie modifie **temporairement** `TradeController` et `TradeService` : remettez-les dans leur état d'origine à la fin (`git restore`, ou **Rollback** dans l'onglet **Commit** d'IntelliJ).

### Étape 1 — Le proxy de `@Transactional`

Dans le constructeur de `TradeController`, ajoutez temporairement :

```java
System.out.println("TradeService injecté : " + tradeService.getClass().getName());
```

Faites de même dans `ItemController` pour `itemService`. Relancez, et comparez les deux lignes dans la console. Pourquoi l'un des deux services n'est-il pas l'objet que vous avez écrit ?

<details>
<summary>Explication</summary>

`TradeService` a une méthode `@Transactional` : Spring n'injecte pas l'objet lui-même, mais un **proxy**, une sous-classe générée au démarrage (`TradeService$$SpringCGLIB$$0`), qui entoure chaque appel d'un `BEGIN` et d'un `COMMIT` ou `ROLLBACK`. `ItemService` n'en a pas besoin : il est injecté tel quel. C'est possible parce que c'est le conteneur IoC (TP 1) qui crée et injecte les beans : il peut injecter un emballage à la place de l'objet.

</details>

### Étape 2 — Voir l'interception

Posez un **point d'arrêt** sur la première ligne de `TradeService.accept` (clic dans la marge), lancez l'application en mode debug (icône d'insecte d'IntelliJ), et exécutez **Trocs > Accepter troc** dans Bruno.

Quand l'exécution s'arrête, regardez la pile d'appels (onglet **Debugger > Frames**) entre `TradeController.accept` et `TradeService.accept`. Quelles classes y trouvez-vous, qui ne viennent pas de votre code ?

Vous devriez y voir, entre autres, `CglibAopProxy` et `TransactionInterceptor` : le proxy, et le code de Spring qui ouvre et ferme la transaction autour de votre méthode.

### Étape 3 — Journaliser à la main

> *Le client* : « Si un utilisateur demande pourquoi son troc d'hier a échoué, personne ne peut lui répondre. Je veux une trace de chaque tentative de troc, réussie ou non. »

Écrivez-le à la main dans `accept` et `propose` : une ligne à l'entrée de la méthode, une en cas de succès, une en cas d'échec. Par exemple :

```java
public Trade accept(Long tradeId, Long userId) {
    System.out.println("→ accept " + tradeId + " " + userId);
    try {
        // … le code métier existant …
        System.out.println("✓ accept : troc " + tradeId);
        return …;
    } catch (RuntimeException e) {
        System.out.println("✗ accept : " + e.getMessage());
        throw e;
    }
}
```

Puis répondez :

1. Quelle proportion des lignes de `accept` parle encore de troc ?
2. Il faudrait faire de même dans `reject`, `findById`, puis dans `UserService` et `ItemService`. Combien de méthodes en tout ? Que se passe-t-il le jour où quelqu'un en oublie une ?
3. L'équipe d'exploitation veut maintenant la **durée** de chaque opération dans la trace : combien de méthodes faut-il modifier, et retester ?
4. Pour combien de raisons différentes `TradeService` peut-il maintenant devoir changer ?

**Remettez `TradeController`, `ItemController` et `TradeService` dans leur état d'origine.**

### Ce qu'il faut retenir

- La journalisation est une **préoccupation transverse**, comme les transactions. Écrite à la main, elle noie le métier, se recopie partout (et s'oublie quelque part), et donne au service une seconde raison de changer, contraire au **S** de SOLID.
- Spring sait déjà exécuter du code autour d'une méthode sans toucher à sa classe : il injecte un **proxy** à sa place. Un appel interne ne traverse pas le proxy.
- Écrire un **aspect**, c'est ajouter notre propre couche à ce proxy.

---

## Partie 2 — Mise en place : un aspect de journalisation (obligatoire)

### Objectifs

Une classe `TradeLoggingAspect`, dans `fr.cours.troc.trade`, écrit dans la console, pour **chaque** appel à une méthode publique de `TradeService` :

- une ligne **avant** l'appel : le nom de la méthode et ses arguments (niveau `INFO`) ;
- une ligne en cas de **succès** : le nom de la méthode et, si elle a renvoyé un troc, son id et son statut (niveau `INFO`) ;
- une ligne en cas d'**échec** : le nom de la méthode et le message de l'exception (niveau `WARN`).

Contraintes :

- `TradeService` est **identique** à celui de la fin du TP 6 ;
- l'aspect **observe** : le client reçoit toujours les mêmes codes (`409`, `403`…), et la transaction de `accept` fait toujours son `ROLLBACK`.

Le scénario de vérification doit produire une histoire lisible :

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

> Organisation conseillée pour un groupe de 4 : deux personnes sur l'aspect ; les deux autres peuvent commencer le TP 8 (tests), dont la préparation (base de test) est indépendante.

### La dépendance

Ajoutez dans `pom.xml`, puis rechargez Maven :

```xml
<!-- AOP, la programmation orientée aspect -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aspectj</artifactId>
</dependency>
```

> Dans beaucoup de tutoriels, ce starter s'appelle `spring-boot-starter-aop` : c'est son ancien nom, renommé en Spring Boot 4, où il est introuvable.

Ce starter apporte les annotations `@Aspect`, `@Before`, etc., venues d'[AspectJ](https://eclipse.dev/aspectj/), un outil d'AOP plus ancien que Spring dont Spring réutilise la syntaxe. Le mécanisme, lui, reste celui de Spring : le proxy.

### Syntaxes utiles

#### Un logger plutôt que `System.out.println`

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(MaClasse.class);

log.info("→ {} {}", "accept", 7);   // affiche « → accept 7 »
log.warn("✗ {} : {}", nom, message);
```

| | `System.out.println` | `log.info(…)` |
|---|---|---|
| Date, heure, thread, classe d'origine | non | oui, sur chaque ligne |
| Niveau d'importance | non | `debug`, `info`, `warn`, `error` |
| Désactiver ou filtrer sans toucher au code | non | oui, dans `application.properties` |
| Envoyer vers un fichier, un outil de supervision | non | oui, par configuration |

SLF4J est déjà fourni par Spring Boot. Les `{}` sont remplacés, dans l'ordre, par les arguments qui suivent. Niveaux : `info` pour ce qui se passe normalement, `warn` pour une anomalie qui n'empêche pas l'application de tourner (un troc refusé), `error` pour une panne. Vous avez déjà réglé un niveau : `logging.level.org.springframework.jdbc.core.JdbcTemplate=DEBUG` (TP 5).

#### Le vocabulaire

| Terme | Sens | Dans notre cas |
|---|---|---|
| **Join point** | un endroit du programme qu'on peut intercepter ; avec Spring, toujours **l'appel d'une méthode d'un bean** | l'appel de `accept(1, 2)` |
| **Pointcut** | le **filtre** qui choisit les join points : *où* agir | « toutes les méthodes de `TradeService` » |
| **Advice** | le code à exécuter, et *quand* : avant, après, en cas d'erreur… | « écrire une ligne de log » |
| **Aspect** | la classe qui regroupe pointcuts et advices | `TradeLoggingAspect` |

#### Un aspect

```java
@Aspect     // org.aspectj.lang.annotation : cette classe contient des advices
@Component  // et c'est un bean : sinon Spring ne la connaît pas
public class UnAspect {
    …
}
```

#### Un pointcut nommé

```java
@Pointcut("execution(* fr.cours.troc.un.package.UneClasse.*(..))")
void nomDuFiltre() {   // méthode vide : elle ne sert que d'étiquette, réutilisable par les advices
}
```

```
execution(  *                 fr.cours.troc.un.package.UneClasse  .*         (..)  )
            type de retour :  la classe, avec son package          méthode :  arguments :
            n'importe lequel  complet                              n'importe  n'importe lesquels
```

Recopiez le nom complet de la classe depuis IntelliJ (clic droit sur la classe > **Copy Reference**) : une faute de frappe ne provoque **aucune** erreur, le filtre ne trouve simplement rien.

#### Les advices

```java
@Before("nomDuFiltre()")
public void avant(JoinPoint jp) { … }

@AfterReturning(pointcut = "nomDuFiltre()", returning = "result")
public void apresSucces(JoinPoint jp, Object result) { … }        // result : ce que la méthode a renvoyé

@AfterThrowing(pointcut = "nomDuFiltre()", throwing = "ex")
public void apresEchec(JoinPoint jp, RuntimeException ex) { … }  // ex : l'exception levée
```

- Les noms dans `returning` et `throwing` doivent être **identiques** à ceux des paramètres.
- Le type du paramètre filtre aussi : `RuntimeException` attrape les `ResponseStatusException` et les erreurs de la base ; `Object` reçoit n'importe quel résultat.
- `@AfterThrowing` **observe** l'exception, il ne l'attrape pas : elle continue jusqu'au client et jusqu'au proxy de la transaction.
- Il existe aussi `@After` (dans tous les cas, comme un `finally`) et `@Around` (voir bonus).

Le `JoinPoint` (`org.aspectj.lang.JoinPoint`) décrit l'appel intercepté :

| Expression | Valeur |
|---|---|
| `jp.getSignature().getName()` | le nom de la méthode appelée (`accept`) |
| `jp.getArgs()` | les arguments reçus, dans un tableau ; `Arrays.toString(…)` l'affiche lisiblement (`[1, 2]`) |

#### Tester un type, et le convertir dans la foulée

```java
if (result instanceof Trade trade) {   // si result est un Trade, il est rangé dans la variable trade
    … trade.getId() …
}
```

### Quand chaque advice s'exécute-t-il ?

```
TradeController ──accept(1, 2)──▶ ┌────────── proxy généré par Spring ──────────┐
                                  │ @Before          → accept [1, 2]            │
                                  │      TradeService.accept(…)  (inchangé)     │
                                  │ @AfterReturning  → ✓ si la méthode a rendu  │
                                  │ @AfterThrowing   → ✗ si elle a levé         │
                                  └─────────────────────────────────────────────┘
```

C'est le **même proxy** que celui de `@Transactional` : il a maintenant deux couches, la transaction et vos traces.

### Vérifier

Commencez par l'advice `@Before` seul : relancez, exécutez **Trocs > Lister trocs d'un utilisateur**, et vérifiez qu'une ligne apparaît avant d'écrire les deux autres advices.

Puis repartez d'une base propre (arrêtez, supprimez `data/`, relancez) et, dans Bruno :

1. **Créer utilisateur** (Alice), **Créer un 2e utilisateur** (Bob) ;
2. **Créer objet (Alice)** (10 tomates), **Créer objet (Bob)** (5 carottes) ;
3. **Proposer troc**, deux fois (trocs 1 et 2) ;
4. **Accepter troc** sur le troc 1 ;
5. **Accepter troc** sur le troc 2 (URL `/trades/2/accept`) : Bob n'a plus qu'une carotte ;
6. **Refuser troc** sur le troc 2, avec `"userId": 1` ;
7. **Consulter troc** (`GET /trades/1`).

La console doit raconter cette histoire, au milieu des lignes SQL (voir les objectifs, plus `→ findById [1]` et `✓ findById : troc 1 EXECUTED` à la fin). Répondez ensuite, par écrit :

1. `accept` appelle `findById` dès sa première ligne. Pourquoi ne voit-on **pas** de ligne `→ findById` pendant un `accept`, alors qu'on en voit une à l'étape 7 ? Et pourquoi aucune ligne pour les méthodes de vérification privées ?
2. Comment feriez-vous pour journaliser aussi `ItemService` ? Que faudrait-il changer dans les services ?

- [ ] `pom.xml` contient `spring-boot-starter-aspectj`.
- [ ] Le scénario produit l'histoire attendue (`→`, `✓`, `✗`), échecs en `WARN`, le reste en `INFO`.
- [ ] Le client reçoit toujours les mêmes codes qu'au TP 6.
- [ ] `TradeService` n'a pas été modifié (vérifiez avec `git diff` sur le fichier, ou l'onglet **Commit** d'IntelliJ).
- [ ] Les deux questions ont une réponse écrite.

## Erreurs fréquentes

| Symptôme | Cause probable |
|---|---|
| Aucune ligne de l'aspect, aucune erreur au démarrage | **Faute dans le pointcut** : package ou nom de classe mal recopié, majuscule oubliée. Spring ne prévient pas : un filtre qui ne trouve rien n'est pas une erreur pour lui. |
| Aucune ligne de l'aspect, et le pointcut est juste | `@Component` oublié : l'aspect n'est pas un bean. |
| `Cannot resolve symbol 'Aspect'` | Dépendance `spring-boot-starter-aspectj` absente, ou Maven pas rechargé. |
| `spring-boot-starter-aop` introuvable dans Maven | Ancien nom du starter : en Spring Boot 4, c'est `spring-boot-starter-aspectj`. |
| Le `JoinPoint` vient du mauvais package | Il faut `org.aspectj.lang.JoinPoint`. |
| `error at ::0 formal unbound in pointcut` au démarrage | Le nom dans `returning` ou `throwing` ne correspond pas au nom du paramètre. |
| Les flèches s'affichent `?` ou `â†'` | Encodage de la console Windows : **Settings > Editor > General > Console**, encodage `UTF-8`. Ou remplacez les symboles par `>`, `OK`, `KO`. |
| Le client reçoit un `200` au lieu d'une erreur | L'advice attrape l'exception au lieu de l'observer : `@AfterThrowing` ne doit contenir ni `try`/`catch`, ni `return`. |
| `ProposeTradeRequest[…]` s'affiche en entier, mais `Trade` donne `fr.cours.troc.trade.Trade@1b2c3d` | Normal : un `record` a une méthode `toString()` générée, pas une classe ordinaire. |

Pour vérifier qu'un advice est bien appelé, le plus sûr reste un **point d'arrêt** dans sa méthode, en mode debug.

## Bonus

### Bonus 1 — Chronométrer avec `@Around`

`@Around` **englobe** l'appel : c'est lui qui décide d'appeler (ou non) la vraie méthode.

```java
@Around("nomDuFiltre()")
public Object timed(ProceedingJoinPoint jp) throws Throwable {  // org.aspectj.lang.ProceedingJoinPoint
    long start = System.nanoTime();
    try {
        return jp.proceed();               // appelle la vraie méthode, et renvoie son résultat
    } finally {
        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("⏱ {} : {} ms", jp.getSignature().getName(), ms);
    }
}
```

1. Quelle opération est la plus lente ? Pourquoi la première requête est-elle souvent bien plus lente que les suivantes ?
2. Mettez **temporairement** `jp.proceed()` en commentaire (avec `return null;`). Que se passe-t-il quand on propose un troc ? Que reçoit le client, qu'y a-t-il en base ?
3. Décrivez en trois lignes ce que contient l'`@Around` de `@Transactional` (indice : `BEGIN`, `proceed()`, puis…).

### Bonus 2 — Choisir les méthodes avec une annotation maison

Créez une annotation `@Timed` dans `fr.cours.troc` :

```java
@Target(ElementType.METHOD)          // se pose sur une méthode
@Retention(RetentionPolicy.RUNTIME)  // reste visible à l'exécution, pour que Spring la lise
public @interface Timed {
}
```

Le pointcut `@annotation(fr.cours.troc.Timed)` choisit les méthodes qui la portent. Déplacez le chronométrage dans un `TimingAspect`, et posez `@Timed` sur `TradeService.accept` et `ItemService.create`. C'est le même principe que `@Transactional` : une annotation qui ne fait rien seule, et un aspect qui la cherche.

### Bonus 3 — Journaliser tous les services

1. Élargissez le pointcut : `execution(* fr.cours.troc..*Service.*(..))` (`..` entre deux packages : n'importe quel sous-package). Déplacez l'aspect dans `fr.cours.troc`, et ajoutez le nom de la classe dans chaque ligne (`jp.getSignature().getDeclaringType().getSimpleName()`).
2. Proposez un troc : pourquoi les appels de `propose` à `userService.findById` et `itemService.findById` sont-ils interceptés, alors que le `findById` interne ne l'était pas ?
3. **Données sensibles** : si `User` avait un champ `password`, que se passerait-il avec `Arrays.toString(jp.getArgs())` sur `create` ? Les logs sont lus par beaucoup de monde (exploitation, support, outils externes) : comment l'éviter ?

## Pour aller plus loin

- [Spring Framework — Programmation orientée aspect avec Spring](https://docs.spring.io/spring-framework/reference/core/aop.html), en particulier [les pointcuts](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/pointcuts.html), [les advices](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/advice.html) et [le fonctionnement par proxy](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)
- [Spring Boot — Logging](https://docs.spring.io/spring-boot/reference/features/logging.html)
- [SLF4J — Manuel](https://www.slf4j.org/manual.html)
- [Baeldung — Introduction to Spring AOP](https://www.baeldung.com/spring-aop) et [Implementing a Custom Spring AOP Annotation](https://www.baeldung.com/spring-aop-annotation) (en anglais)
- [Wikipédia — Programmation orientée aspect](https://fr.wikipedia.org/wiki/Programmation_orient%C3%A9e_aspect)
