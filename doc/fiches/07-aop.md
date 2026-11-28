# 🧩 Fiche 7 — AOP

> `TP N` = notion vue à partir du TP N · ✅ à faire · ❌ à éviter · ⚠️ piège · 💡 astuce

## 💡 L'idée · `TP 8`

Une **préoccupation transverse** concerne toutes les classes sans appartenir au métier d'aucune : journaliser, chronométrer, ouvrir une transaction, vérifier des droits. Un **aspect** l'écrit **une fois** et l'applique aux méthodes choisies.

| Déjà vu | C'est un aspect fourni par Spring |
|---|---|
| `@Transactional` (TP 7) | `BEGIN` avant, `COMMIT` / `ROLLBACK` après |
| `@PreAuthorize` (TP 5, bonus) | vérifier un rôle avant |

## 🎭 Le proxy · `TP 7`

```
TradeController ──accept(1, 2)──▶ ┌──────── TradeService$$SpringCGLIB$$0 (proxy) ─────────┐
                                  │ @Transactional   → BEGIN                              │
                                  │ @Before          → → accept [1, 2]                    │
                                  │        TradeService.accept(…)  ← votre code, inchangé │
                                  │ @AfterReturning  → ✓ accept : troc 1 EXECUTED         │
                                  │ @AfterThrowing   → ✗ accept : 409 CONFLICT "…"        │
                                  │ @Transactional   → COMMIT ou ROLLBACK                 │
                                  └───────────────────────────────────────────────────────┘
```

- le conteneur IoC injecte le **proxy** à la place du bean : `tradeService.getClass().getName()` le montre
- ⚠️ appel interne `this.findById(…)`, méthode `private` → **pas** interceptés
- 💡 appel d'un **autre** bean (`itemService.findById`) → intercepté

## 📖 Vocabulaire · `TP 8`

| Terme | Question | troc-api |
|---|---|---|
| **Join point** | quel endroit ? (avec Spring : l'appel d'une méthode d'un bean) | l'appel `accept(1, 2)` |
| **Pointcut** | *où* agir ? (le filtre) | toutes les méthodes de `TradeService` |
| **Advice** | *quoi* et *quand* ? | écrire une ligne avant / après |
| **Aspect** | dans quelle classe ? | `TradeLoggingAspect` |

## 📦 Dépendance · `TP 8`

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aspectj</artifactId>   <!-- ❌ spring-boot-starter-aop : ancien nom -->
</dependency>
```

## ✍️ Un aspect · `TP 8`

```java
@Aspect                                       // org.aspectj.lang.annotation
@Component                                    // ✅ sinon Spring ne le connaît pas
public class UnAspect {

    private static final Logger log = LoggerFactory.getLogger(UnAspect.class);

    @Pointcut("execution(* fr.cours.troc.un.package.UneClasse.*(..))")
    void cible() {                            // méthode vide : une étiquette
    }

    @Before("cible()")
    public void avant(JoinPoint jp) { … }

    @AfterReturning(pointcut = "cible()", returning = "result")
    public void apresSucces(JoinPoint jp, Object result) { … }

    @AfterThrowing(pointcut = "cible()", throwing = "ex")
    public void apresEchec(JoinPoint jp, RuntimeException ex) { … }
}
```

## 🎯 Pointcuts · `TP 8`

```
execution(  *        fr.cours.troc.trade.TradeService  .*        (..)  )
           retour    classe, package complet            méthode   arguments
           quelconque                                   quelconque quelconques
```

| Expression | Cible | TP |
|---|---|:-:|
| `execution(* fr.cours.troc.trade.TradeService.*(..))` | méthodes publiques de `TradeService` | 8 |
| `execution(* fr.cours.troc.trade.TradeService.accept(..))` | `accept` seulement | 8 |
| `execution(* fr.cours.troc..*Service.*(..))` | tous les `…Service` (`..` = sous-packages) | 8 (bonus) |
| `@annotation(fr.cours.troc.Timed)` | méthodes annotées `@Timed` | 8 (bonus) |

⚠️ faute de frappe → **aucune erreur**, aucune trace · 💡 nom complet : clic droit sur la classe > **Copy Reference**

## ⏰ Advices · `TP 8`

| Advice | Quand | Paramètres |
|---|---|---|
| `@Before` | avant | `JoinPoint jp` |
| `@AfterReturning` | après un **succès** | `+ Object result` (nom = `returning`) |
| `@AfterThrowing` | après une **exception** (l'**observe**, ne l'attrape pas) | `+ RuntimeException ex` (nom = `throwing`) |
| `@After` | dans tous les cas (`finally`) | `JoinPoint jp` |
| `@Around` | **englobe** : décide d'appeler la vraie méthode | `ProceedingJoinPoint jp`, renvoie `Object` (bonus) |

| `JoinPoint` | Valeur |
|---|---|
| `jp.getSignature().getName()` | `"accept"` |
| `jp.getArgs()` + `Arrays.toString(…)` | `"[1, 2]"` |
| `jp.getSignature().getDeclaringType().getSimpleName()` | `"TradeService"` |

```java
if (result instanceof Trade trade) {          // tester et convertir en une fois
    log.info("✓ {} : troc {} {}", jp.getSignature().getName(), trade.getId(), trade.getStatus());
}
```

```java
@Around("cible()")                                  // bonus : chronométrer
public Object timed(ProceedingJoinPoint jp) throws Throwable {
    long start = System.nanoTime();
    try {
        return jp.proceed();                        // ⚠️ oublié → la vraie méthode n'est jamais appelée
    } finally {
        log.info("⏱ {} : {} ms", jp.getSignature().getName(), (System.nanoTime() - start) / 1_000_000);
    }
}
```

- ❌ ni `try`/`catch`, ni `return` dans `@AfterThrowing` : le client doit recevoir le même code, la transaction doit faire son `ROLLBACK`
- ⚠️ `Arrays.toString(jp.getArgs())` sur un mot de passe → il finit dans les logs (TP 5)

## 📝 Logger · `TP 8`

```java
private static final Logger log = LoggerFactory.getLogger(MaClasse.class);   // org.slf4j
log.info("→ {} {}", nom, args);                                             // {} remplacés dans l'ordre
```

| Niveau | Pour | Réglage |
|---|---|---|
| `debug` | détail pour le développeur | `logging.level.fr.cours.troc=DEBUG` |
| `info` | ce qui se passe normalement | par défaut |
| `warn` | anomalie qui n'arrête rien (troc refusé) | |
| `error` | panne | |

✅ `log.info(…)` plutôt que `System.out.println` : date, thread, classe, niveau, filtrable par configuration

## ⌨️ IntelliJ · `TP 8`

⭐ = aide Spring, IntelliJ **Ultimate** (licence étudiante gratuite) · raccourcis Windows / Linux ; macOS et liste générale : [fiche 10](10-raccourcis-intellij.md)

| Raccourci | Effet |
|---|---|
| ⭐ icône dans la marge d'un advice | les méthodes ciblées par le pointcut : vérifier qu'il vise ce que l'on croit |
| ⭐ icône dans la marge d'une méthode de service | les advices qui l'interceptent |
| `Ctrl+Alt+Shift+C` sur une méthode | copier son nom complet (`fr.cours.troc.trade.TradeService#…`) pour le pointcut : remplacer `#` par `.` |
| point d'arrêt dans le service + **Frames** | voir le proxy et l'aspect dans la pile d'appels (fiche 9) |

## 🚨 Erreurs fréquentes

| Symptôme | 🔎 Cause | TP |
|---|---|:-:|
| aucune trace, aucune erreur | faute dans le pointcut · `@Component` oublié | 8 |
| `Cannot resolve symbol 'Aspect'` | `spring-boot-starter-aspectj` absent, Maven pas rechargé | 8 |
| `spring-boot-starter-aop` introuvable | ancien nom (Boot 3) | 8 |
| `error at ::0 formal unbound in pointcut` | `returning` / `throwing` ≠ nom du paramètre | 8 |
| `JoinPoint` introuvable ou incompatible | mauvais import : `org.aspectj.lang.JoinPoint` | 8 |
| le client reçoit `200` au lieu d'une erreur | l'advice attrape l'exception | 8 |
| pas de ligne pour `findById` pendant `accept` | appel interne : normal | 8 |
| `Trade@1b2c3d` dans les logs | `toString()` absent (un record l'a, une classe non) | 8 |
| flèches `?` ou `â†'` | console IntelliJ pas en UTF-8 (**Settings > Editor > General > Console**) | 8 |

## 🔗 Pour aller plus loin

[Spring — AOP](https://docs.spring.io/spring-framework/reference/core/aop.html) · [Pointcuts](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/pointcuts.html) · [Advices](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/advice.html) · [Proxies](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html) · [Spring Boot — Logging](https://docs.spring.io/spring-boot/reference/features/logging.html)
