# Budgeter V2

A paycheck budget for Android, rebuilt in Kotlin from the original C# (.NET MAUI) Budgetor, and
made for **two paychecks**, or any number of income sources.

Kotlin, Jetpack Compose, Gradle Kotlin DSL. Android 10 (API 29) and up; targets Android 17 (API 37).

## How it works

- **Money now:** what's in your account before these paychecks.
- **Income:** Paycheck 1 and Paycheck 2 to start. Rename them, add more (a second job, a side
  gig), or remove one.
- **Budget lines**, worked through in order, each switchable on or off:
  - **$ Amount:** a fixed amount. Shows what share of the paycheck it is.
  - **% Percent:** a percent of the paycheck. Shows it in dollars.
  - **Left over:** takes nothing. Shows what's left at that point in the list.
- **From:** each line is paid by one paycheck, or by **both**. With both, an amount is split
  evenly (odd cents go to the first paycheck) and a percent comes out of each paycheck.
- **Summary:** each paycheck's income, budgeted and left, then:
  *money now + all income − everything budgeted = after these paychecks*. That last figure is
  always on screen at the bottom.

It recalculates as you type (there's no Submit button) and saves automatically to a file in the
app's private storage. Line order can be changed from each line's ⋮ menu.

These are the same three line types as the original's Number, Percent and Remaining. The first
run starts with its example lines (Insurance, YouTube, Gas, Rec, Other) spread over two paychecks.

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
