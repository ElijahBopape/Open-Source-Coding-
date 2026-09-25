# AI Usage Disclosure

**Student:** ME Bopape (ST10445847) **Module:** OPSC6312 **Submission:** Part 2 - App Prototype

I used Claude (Anthropic's Claude Code) as an AI coding assistant throughout Part 2,
building directly on the app design, requirements and feature list I set out in my
own Part 1 Research Report and Planning and Design document. Below is a summary of
how it was used.

**Code generation.** I asked Claude to scaffold the Android project (Kotlin, Gradle
build files, layouts and screen logic) and the companion REST API (Node.js/Express)
based on the requirements I had already written in Part 1: register/login with
encrypted passwords, a settings screen, a custom REST API connected to a database,
and my three chosen features (Module management, Task management with priority, and
Timetable management). Claude wrote the initial version of most files, including the
data models, the Retrofit networking layer, the Firebase Authentication screens, the
five main fragments, and the Express routes/JSON data store. I reviewed this code,
adjusted the feature scope and priority-sorting logic to match my own design (the
"Academic Priority System" described in my Planning and Design document), and it is
what appears in this repository.

**Debugging.** While getting the project to actually compile, Claude helped diagnose
two build failures: (1) a plugin conflict when Android Gradle Plugin 9's built-in
Kotlin support was combined with an explicit Kotlin plugin declaration, which was
resolved by moving to a more conservative AGP 8.6.1 / Kotlin 1.9.24 toolchain; and (2)
a resource-linking failure ("attribute res-auto:... not found") that turned out to be
a malformed XML namespace URI (`.../apk/res/res-auto` instead of `.../apk/res-auto`)
copied into every layout file - fixed with a project-wide find-and-replace, after
which the app built and its unit tests passed. It also helped debug an `npm install`
failure caused by a native-compiled SQLite dependency lacking a Python toolchain,
which led to switching the backend to a dependency-free JSON file store instead.

**Documentation.** Claude drafted the README, this AI usage disclosure, and code
comments explaining non-obvious decisions (e.g. why Google Sign-In is stubbed out,
why the backend doesn't use a native database driver). I reviewed and edited this
documentation for accuracy before submission.

**What I did not use AI for.** The underlying app idea, its feature list, the UI
mockups, the REST API design, the database entity model and the project plan were all
defined by me in Part 1, before any AI assistance was used for Part 2. AI was not used
to fabricate research, screenshots or citations for the Part 1 submission.

**Verification.** I ran the Android build (`./gradlew assembleDebug`), the unit tests
(`./gradlew testDebugUnitTest`) and the REST API locally against real HTTP requests
myself to confirm the generated code actually works before including it in this
submission, rather than assuming it was correct.
