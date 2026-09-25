# Student Assistant

**OPSC6312 (Open Source Coding, Intermediate) — Portfolio of Evidence, Part 2: App Prototype**
Student: Elijah Bopape (ST10445847)
Repository: <https://github.com/ElijahBopape/Open-Source-Coding->

![Android CI](https://github.com/ElijahBopape/Open-Source-Coding-/actions/workflows/android-ci.yml/badge.svg)

## For the marker — quick start

The Firebase config committed in this repo (`app/google-services.json`) is the
**real** project config, not a placeholder, so the app builds and runs as-is —
no Firebase account needed to mark this.

1. Clone the repo and open the root folder in Android Studio (this repo root
   *is* the Gradle project). Let it sync.
2. **Start the REST API locally** (needed either way — see note below):
   ```bash
   cd backend
   npm install
   npm start
   ```
   Leave that terminal running. It listens on `http://localhost:3000`.
3. Run the app on an **emulator** (not a physical device, unless you also
   change the API URL — see below) via the green Run button in Android
   Studio, or `./gradlew installDebug`.
4. Register a new account, then use Modules / Tasks / Timetable / Settings as
   normal.

> **Why localhost:** the app is currently built to point at
> `https://student-assistant-api.onrender.com/`, a Render deployment that
> could not be confirmed live before this submission was finalised (time
> constraints). Android emulators reach your machine's `localhost` via the
> special address `10.0.2.2`, and `app/build.gradle`'s **debug** build type
> already targets `http://10.0.2.2:3000/` for exactly this reason — so a
> **debug** build run on the emulator will talk to the API you started in
> step 2 with no further changes needed. A **release** build, or running on a
> physical device, would need `API_BASE_URL` pointed at a REST API address
> reachable from that device instead (a deployed URL, or your machine's LAN
> IP with the phone on the same Wi-Fi).

No demonstration video is included with this submission — I ran out of time
to record and deploy the backend publicly before the deadline. Everything
above has been built and run locally to confirm it works; see
[`AI_USAGE.md`](AI_USAGE.md) for what AI assistance was used to get here.

Student Assistant is an academic organiser for tertiary students. It brings modules,
a class timetable and prioritised assignments/tasks into one place, so a student can
see what needs attention today without juggling a separate calendar app, task app and
notes app. It was designed around the findings of the Part 1 research report, which
compared MyStudyLife, Microsoft To Do and Todoist (see `docs/` in the Part 1
submission) and identified an opportunity for a simpler, student-specific tool that
connects modules, tasks and a timetable rather than treating them as unrelated lists.

## Table of contents

- [Purpose and design considerations](#purpose-and-design-considerations)
- [Features implemented in this prototype](#features-implemented-in-this-prototype)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Setting up the Android app](#setting-up-the-android-app)
- [Setting up the REST API](#setting-up-the-rest-api)
- [Running the tests](#running-the-tests)
- [GitHub Actions](#github-actions)
- [Demonstration video](#demonstration-video)
- [What's deferred to the Final PoE](#whats-deferred-to-the-final-poe)
- [AI usage](#ai-usage)

## Purpose and design considerations

The app follows the requirements and design set out in the Part 1 Planning and Design
document: a clean Material Design interface, a bottom navigation bar (Dashboard,
Modules, Tasks, Timetable, Settings), and an "Academic Priority System" that surfaces
the most urgent work first instead of a plain chronological list. Modules are the
foundation entity - tasks and timetable entries both link back to a module - which
mirrors how students actually think about their workload ("what's due for Programming
2A") rather than a flat, unstructured to-do list.

Design decisions that differ from the Part 1 document, and why:

- **REST API technology**: the design document proposed a PHP + MySQL backend. This
  prototype instead uses **Node.js + Express**, storing data in a small JSON file
  store rather than a native SQL driver (see `backend/store.js`). This keeps `npm
  install` dependency-free (no C++ build toolchain needed on any machine, including
  free CI/hosting runners) while still satisfying the requirement of a REST API the
  student created, connected to a database/storage mechanism, hosted online. The REST
  contract (endpoints, JSON shapes) is unchanged from the design, so swapping in a
  real relational database later (e.g. for the Final PoE) would not require any
  Android-side changes.
- **Google Sign-In (SSO)**, **offline mode with Room/SQLite sync**, **push
  notifications** and **multi-language support** are explicitly marked "(PoE only)"
  in the assessment brief. The UI already anticipates them (a "Continue with Google"
  button, a language preference in Settings) but the underlying functionality is
  intentionally deferred to the Final PoE, as permitted by the brief.

## Features implemented in this prototype

| Requirement (assessment rubric) | Where it lives |
| --- | --- |
| Register/login with encrypted password | `ui/auth/LoginActivity.kt`, `RegisterActivity.kt` (Firebase Authentication - passwords are hashed by Firebase and never handled in plain text by this app) |
| Settings the user can change | `ui/settings/SettingsFragment.kt` (language, notification and theme preferences, persisted with `SessionManager`) |
| REST API created by the student, connected to storage, hosted online | `/backend` (Node.js + Express + JSON file store), consumed via Retrofit in `data/remote/` |
| Feature 1 - Module management | `ui/modules/` |
| Feature 2 - Task/assignment management with priority | `ui/tasks/`, `util/PriorityUtils.kt` |
| Feature 3 - Timetable management | `ui/timetable/` |

The app also has a Dashboard (`ui/dashboard/DashboardFragment.kt`) that ties the three
features together: it shows the next class from the timetable and the highest
priority pending tasks, computed by the same `PriorityUtils` logic used on the Tasks
screen.

## Architecture

```
Android app (Kotlin)  --Retrofit/OkHttp-->  REST API (Node.js/Express)  -->  JSON file store
        |
        +-- Firebase Authentication (register/login, password hashing)
```

- **UI**: one `MainActivity` hosting five `Fragment`s via a `BottomNavigationView`,
  each using View Binding (no findViewById).
- **Networking**: a single Retrofit client (`data/remote/RetrofitClient.kt`) attaches
  the signed-in user's Firebase UID to every request via an `OkHttp` interceptor
  (`AuthHeaderInterceptor`), so the API can scope data per user without its own login
  step. Repository classes (`data/repository/`) wrap each Retrofit call in a
  success/failure result so the UI never crashes on a network error.
- **REST API**: `backend/server.js` exposes `/api/modules`, `/api/tasks` and
  `/api/timetable` (GET/POST/PUT/DELETE), guarded by a required `X-User-Id` header.
- **Academic Priority System**: `util/PriorityUtils.kt` is a small, dependency-free
  Kotlin object that sorts tasks by completion state, then priority, then due date -
  covered by JVM unit tests in `app/src/test`.

## Project structure

```
.
├── app/                        # Android app module (Kotlin)
│   ├── src/main/java/com/bopape/studentassistant/
│   │   ├── data/                # models, Retrofit service, repositories
│   │   ├── ui/                  # auth, dashboard, modules, tasks, timetable, settings
│   │   └── util/                # PriorityUtils, DateUtils, SessionManager, ApiResult
│   ├── src/test/                # JVM unit tests (PriorityUtilsTest)
│   └── src/androidTest/         # Espresso instrumented test (LoginActivityTest)
├── backend/                     # REST API (Node.js + Express)
│   ├── server.js, store.js, routes/
│   └── render.yaml              # Render.com deploy blueprint
└── .github/workflows/           # GitHub Actions CI (build + unit tests)
```

## Setting up the Android app

1. **Firebase project**: create a free project at
   [console.firebase.google.com](https://console.firebase.google.com), add an Android
   app with package name `com.bopape.studentassistant`, enable **Authentication ->
   Sign-in method -> Email/Password**, then download `google-services.json` and
   replace the placeholder file at `app/google-services.json` with it.
2. **API URL**: `app/build.gradle` sets `API_BASE_URL` to the deployed backend (see
   below). Update it if you deploy your own copy of the backend.
3. Open the project root in Android Studio (this repository root *is* the Gradle
   project - there is no separate subfolder to open), let Gradle sync, and run on an
   emulator or device.
4. Command line: `./gradlew assembleDebug` produces
   `app/build/outputs/apk/debug/app-debug.apk`.

## Setting up the REST API

The backend lives in `/backend` and has no native dependencies, so `npm install`
works the same on Windows, macOS, Linux and any CI/hosting runner.

**Run it locally:**

```bash
cd backend
npm install
npm start
# API now listening on http://localhost:3000
```

**Deploy it for free on [Render](https://render.com):**

1. Push this repository to GitHub (already done if you're reading this on GitHub).
2. On Render, choose **New -> Blueprint**, point it at this repository - it will read
   `backend/render.yaml` and configure the service automatically.
   (Or manually: New -> Web Service, root directory `backend`, build command `npm
   install`, start command `npm start`.)
3. Copy the resulting `https://<your-service>.onrender.com` URL into
   `API_BASE_URL` in `app/build.gradle`.

Every request to `/api/*` must include an `X-User-Id` header (the Android app adds
this automatically from the signed-in Firebase user); requests without it receive
`401 Unauthorized`.

> **Note on persistence**: Render's free tier does not guarantee the filesystem
> survives a redeploy. This is fine for demonstrating the prototype (data persists
> for the lifetime of the running service) but for the Final PoE this can be swapped
> for a hosted database (e.g. Render's free PostgreSQL, or Firebase Firestore)
> without changing the Android app's REST contract.

## Running the tests

```bash
./gradlew testDebugUnitTest        # JVM unit tests (PriorityUtils - the Academic Priority System)
./gradlew connectedAndroidTest     # Instrumented tests (needs an emulator/device)
```

`PriorityUtilsTest` verifies the sorting rules behind the Academic Priority System
(high priority first, soonest due date first, completed tasks last).
`LoginActivityTest` confirms the login screen validates an empty form instead of
crashing, per the "handle invalid inputs without crashing" requirement.

## GitHub Actions

`.github/workflows/android-ci.yml` runs on every push/PR to `main`: it sets up JDK 17,
runs the unit test suite, then builds the debug APK, uploading both the APK and the
test report as workflow artifacts. This follows the pattern from the guides linked in
the assessment brief (automated-build-android-app-with-github-action, and
IMAD5112/Github-actions' `build.yml`).

## Demonstration video

[Video link - to be added]

## What's deferred to the Final PoE

Per the assessment brief, the following are explicitly "(PoE only)" and are designed
for but not yet implemented:

- Single sign-on via Google Sign-In (button present, wiring deferred)
- Offline mode with Room/SQLite and background sync
- Real-time push notifications (Firebase Cloud Messaging)
- Multi-language support for Sepedi and isiZulu (the language preference is already
  stored in Settings; translated string resources are the remaining work)

## AI usage

See [`AI_USAGE.md`](AI_USAGE.md) for a disclosure of how AI tools were used while
completing this submission.
