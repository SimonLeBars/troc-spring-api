# ⌨️ Fiche 10 — Raccourcis IntelliJ

> `TP N` = notion vue à partir du TP N · ✅ à faire · ❌ à éviter · ⚠️ piège · 💡 astuce

Raccourcis du keymap par défaut (Windows / Linux), équivalent macOS en 2ᵉ colonne.
💡 **Help > Keyboard Shortcuts PDF** : la liste officielle à imprimer · plugin **Key Promoter X** : signale le raccourci de chaque clic.

## 🥇 Les 5 à connaître par cœur · `TP 0`

| Windows / Linux | macOS | Effet |
|---|---|---|
| `Shift` `Shift` | `⇧` `⇧` | chercher partout : classe, fichier, action, réglage |
| `Alt+Entrée` | `⌥↩` | 💡 corriger ce qui est souligné : import, créer la méthode, ajouter au constructeur… |
| `Alt+Inser` | `⌘N` | générer : constructeur, getters, `toString`, méthode de test |
| `Ctrl+Shift+A` | `⌘⇧A` | lancer une action par son nom (« reformat », « reload maven ») |
| `Ctrl+Espace` | `⌃Espace` | complétion (`Ctrl+Shift+Espace` : seulement ce qui a le bon type) |

## 🧭 Naviguer · `TP 0`

| Windows / Linux | macOS | Effet |
|---|---|---|
| `Ctrl+N` · `Ctrl+Shift+N` | `⌘O` · `⌘⇧O` | ouvrir une classe · un fichier (`schema.sql`, `application.properties`) |
| `Ctrl+E` | `⌘E` | fichiers récents |
| `Ctrl+B` ou `Ctrl+clic` | `⌘B` | aller à la déclaration |
| `Ctrl+Alt+B` | `⌘⌥B` | aller à l'implémentation (interface → classe) |
| `Alt+F7` | `⌥F7` | qui utilise cette classe / méthode ? |
| `Ctrl+F12` | `⌘F12` | plan du fichier (une 2ᵉ fois : membres hérités) |
| `Ctrl+Alt+←` / `→` | `⌘[` / `⌘]` | revenir / repartir (après un `Ctrl+B`) |
| `Ctrl+Shift+F` | `⌘⇧F` | chercher un texte dans tout le projet (`"/items"`) |
| `F2` | `F2` | erreur suivante dans le fichier |
| `Alt+1` · `Alt+F12` | `⌘1` · `⌥F12` | arborescence du projet · terminal |

## ✍️ Écrire · `TP 0`

| Windows / Linux | macOS | Effet |
|---|---|---|
| `Ctrl+D` · `Ctrl+Y` | `⌘D` · `⌘⌫` | dupliquer · supprimer la ligne |
| `Alt+Shift+↑` / `↓` | `⌥⇧↑` / `↓` | déplacer la ligne |
| `Ctrl+/` | `⌘/` | commenter / décommenter |
| `Ctrl+W` | `⌥↑` | étendre la sélection (mot → expression → bloc) |
| `Ctrl+Shift+Entrée` | `⌘⇧↩` | terminer l'instruction (`;`, `{ }`) |
| `Ctrl+P` · `Ctrl+Q` | `⌘P` · `F1` | paramètres attendus · documentation |
| `Ctrl+Alt+L` · `Ctrl+Alt+O` | `⌘⌥L` · `⌃⌥O` | reformater · nettoyer les imports |

⚠️ **AZERTY :** `Ctrl+/` ne répond pas toujours → `/` du pavé numérique, ou réassigner dans **Settings > Keymap**.

## 🧩 Abréviations · `TP 0`

Taper l'abréviation puis `Tab`.

| Taper | Obtenir |
|---|---|
| `psvm` · `sout` | `public static void main(…)` · `System.out.println()` |
| `fori` · `iter` | boucle `for` indexée · `for (… : …)` |
| `expr.var` | `var x = expr;` : nommer un résultat |
| `expr.return` · `expr.nn` | `return expr;` · `if (expr != null)` |
| `liste.for` · `expr.sout` | boucle sur la liste · afficher l'expression |

## 🔧 Refactorer · `TP 1`

`Ctrl+Alt+Shift+T` (`⌃T`) : le menu complet des refactorings.

| Windows / Linux | macOS | Effet |
|---|---|---|
| `Shift+F6` | `⇧F6` | renommer partout (classe, fichier, appels) ✅ jamais à la main |
| `Ctrl+Alt+V` · `Ctrl+Alt+C` | `⌘⌥V` · `⌘⌥C` | extraire une variable · une constante |
| `Ctrl+Alt+M` | `⌘⌥M` | extraire une méthode |
| `Ctrl+Alt+F` · `Ctrl+Alt+P` | `⌘⌥F` · `⌘⌥P` | extraire un champ · un paramètre |
| `Ctrl+Alt+N` | `⌘⌥N` | l'inverse : remettre en ligne |
| `F6` | `F6` | déplacer une classe dans un autre package |

## ▶️ Lancer · `TP 0`

| Windows / Linux | macOS | Effet |
|---|---|---|
| `Shift+F10` · `Shift+F9` | `⌃R` · `⌃D` | lancer · déboguer la configuration courante |
| `Ctrl+Shift+F10` | `⌃⇧R` | lancer ce qui est sous le curseur (`main`, test) |
| `Alt+Shift+F10` | `⌃⌥R` | choisir la configuration à lancer |
| `Ctrl+F5` · `Ctrl+F2` | `⌘R` · `⌘F2` | relancer · arrêter (✅ avant de relancer : sinon `Port 8080 was already in use`) |
| `Alt+4` | `⌘4` | console de l'application |

## ⚙️ Réglages qui font gagner du temps · `TP 0`

- **Settings > Editor > General > Auto Import** : cocher *Add unambiguous imports on the fly* et *Optimize imports on the fly*
- **Settings > Tools > Actions on Save** : *Reformat code* + *Optimize imports*
- ⚠️ deux classes du même nom (`Item` du projet, `Item` d'une bibliothèque) : vérifier l'import proposé par `Alt+Entrée`

## 🐞 Déboguer · `TP 8`

| Windows / Linux | macOS | Effet |
|---|---|---|
| `Ctrl+F8` · `Ctrl+Shift+F8` | `⌘F8` · `⌘⇧F8` | poser un point d'arrêt · tous les voir (condition : clic droit sur le point) |
| `F8` · `F7` | `F8` · `F7` | ligne suivante · entrer dans la méthode |
| `Shift+F8` · `F9` | `⇧F8` · `⌘⌥R` | sortir de la méthode · continuer jusqu'au prochain point |
| `Alt+F8` | `⌥F8` | évaluer une expression (`items.findAll()`) pendant la pause |

💡 condition (`userId == 2`) : le programme ne s'arrête que si elle est vraie · utiliser le débogueur : [fiche 9](09-outils-depannage.md).

## 🍃 Par thème

Chaque fiche a sa section ⌨️ IntelliJ, avec les gestes utiles pour son thème (⭐ = aide Spring, version **Ultimate**, licence étudiante gratuite) :

| Fiche | Exemples |
|---|---|
| [1 — Beans](01-beans-ioc.md) | générer le constructeur, aller à l'implémentation, `Could not autowire` |
| [2 — HTTP et REST](02-http-rest.md) | fenêtre **Endpoints**, `HttpStatus.` + complétion |
| [3 — Configuration](03-configuration.md) | complétion dans `application.properties`, une configuration de lancement par profil |
| [4 — DTO](04-dto-validation-erreurs.md) | **Convert to record**, **Create missing branches** |
| [5 — Sécurité](05-securite.md) | ouvrir et déboguer un filtre de Spring Security |
| [6 — Persistance](06-persistance-transactions.md) | méthodes héritées du repository, complétion `findBy…`, fenêtre **Database** |
| [7 — AOP](07-aop.md) | icônes aspect ↔ méthodes ciblées, **Copy Reference** pour le pointcut |
| [8 — Tests](08-tests.md) | créer le test, relancer les rouges, couverture |

## 🔗 Pour aller plus loin

[IntelliJ — Raccourcis](https://www.jetbrains.com/help/idea/mastering-keyboard-shortcuts.html) · [IntelliJ — Postfix completion](https://www.jetbrains.com/help/idea/auto-completing-code.html#postfix_completion) · [IntelliJ — Refactoring](https://www.jetbrains.com/help/idea/refactoring-source-code.html) · [IntelliJ — Debugger](https://www.jetbrains.com/help/idea/debugging-code.html)
