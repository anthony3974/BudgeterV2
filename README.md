# Budgeter V2

A paycheck budget for Android, rebuilt in Kotlin from the original C# (.NET MAUI) Budgetor, and
made for **two paychecks**, or any number of income sources.

Kotlin, Jetpack Compose, Gradle Kotlin DSL. Android 10 (API 29) and up; targets Android 17 (API 37).

## How it works

One compact screen, laid out like the original:

- **Top row:** Money now, Paycheck 1, Paycheck 2. The two paychecks are added together into one
  paycheck, and every line draws on that.
- **One row per budget line**, worked through in order: on/off checkbox, type, name, value,
  result.
  - **$** Amount: a fixed amount. The result is its share of the paycheck.
  - **%** Percent: a percent of the paycheck. The result is that in dollars.
  - **=** Left over: takes nothing. The result is what's left at that point in the list (red if
    below zero).
- **The type button** opens the line's menu: change the type, move the line up or down, or
  delete it. **+ Add line** adds one.
- **The bottom bar** always shows what's budgeted, what's left of the paycheck, and
  *money now + paycheck − budgeted* = what you'll have after these paychecks.

It recalculates as you type (there's no Submit button) and saves automatically to a file in the
app's private storage. Budgets saved by 1.0, which kept a list of incomes, open with the first
income as Paycheck 1 and the rest added up as Paycheck 2.

These are the same three line types as the original's Number, Percent and Remaining. The first
run starts with its example lines (Insurance, Other, YouTube, Gas, Rec).

## Building and installing

Open the folder in Android Studio, or from a terminal with the phone plugged in over adb:

```
scripts/install.sh            # debug build, installed over the top and started
scripts/install.sh --release  # release build
```

Command-line builds need a full JDK (`javac`). The script uses one Gradle downloaded into
`~/.gradle/jdks` when `JAVA_HOME` doesn't have one.

The maths is in `budget/`, plain Kotlin with no Android dependencies: `./gradlew :budget:test`.

### Signing

Debug and release builds are both signed with one stable key, so every build installs over the
previous one and keeps your budget. The key is **not** in the repo:

- `~/.android/budgeter-v2-release.jks`: the key itself.
- `keystore.properties` (git-ignored, project root): its path, alias and password.

Back both up. Without them a new build can't update the installed app.

### Icon

`scripts/make_icons.py` draws the budget-pie icon as the adaptive icon and as PNGs. F-Droid needs
the PNGs, and `android.enableResourceOptimizations=false` keeps their file names in release APKs so
F-Droid can find them.

## Layout

```
budget/   pure Kotlin: BudgetEngine (the maths), Money (parsing what's typed) + tests
app/
  data/   BudgetForm (the screen as typed) and FormStore (saves it as JSON)
  ui/     BudgetScreen, theme, keep-focused-field-above-the-keyboard helper
```
