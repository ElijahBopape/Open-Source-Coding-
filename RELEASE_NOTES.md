# Release notes

## Version 3.0.0 — Final PoE (November 2026)

Everything below was added or changed since the Part 2 prototype (v1.0).
**Innovative features** are marked with ★.

### New required features

| Feature | What changed |
| --- | --- |
| **Single sign-on (SSO)** | "Continue with Google" now works. It uses Android Credential Manager, and the Google token is exchanged for a Firebase credential. Google and email users get the same per-user data. |
| **Offline mode with sync** | Tasks are offline-first. They are stored in a **Room (SQLite)** database, and changes made offline are marked *pending* (cloud icon). **WorkManager** uploads them automatically when the phone reconnects, even if the app was closed. Modules, timetable, exams and study sessions are cached for offline reading. An offline banner shows how many changes are still waiting. |
| **Real-time push notifications** | Uses **Firebase Cloud Messaging**. The API pushes an alert when a task or exam is added, and sends daily "due today / tomorrow / exam soon" reminders. A local WorkManager reminder also works offline. Settings has a *Test push* button. |
| **Multi-language** | English, **Sepedi** and **isiZulu**. Every screen, message, plural, day and month name is translated. The language switches instantly in Settings, and on Android 13+ it also appears under the system *App languages* setting. |
| **App icon and final image assets** | Adaptive launcher icon (graduation cap + tick from the Part 1 design) with a monochrome/themed layer, plus legacy PNG icons, a branded splash screen and Material icons throughout. Play Store icon and feature graphic are in `playstore/`. |

### New user-defined features
- ★ **Exams (User Defined 4):** add or edit exams linked to a module, with upcoming/past views, a countdown badge, venue and duration. You can record the mark after writing, and the app shows your **average mark** (academic progress).
- ★ **Study Planner (User Defined 5):** plan study sessions linked to an exam or task, tick them off, and see a progress bar and the time left. **Smart plan** automatically spreads revision sessions evenly between today and the deadline (one per day at most), so students don't cram the night before.
- ★ **Exam-aware Academic Priority System:** a task whose module has an exam within 7 days moves up one priority level. It shows an "Exam soon – priority raised" label.
- ★ **Connected dashboard:** next class, next exam countdown, average mark, today's study sessions, quick actions and the top priority tasks, all on one screen.

### Improvements
- **Security:** the API now verifies a **Firebase ID token** on every request. Part 2 trusted a plain `X-User-Id` header. Release builds are HTTPS-only.
- **Database:** the REST API now stores data in **Cloud Firestore (NoSQL)** instead of a JSON file. It is hosted on Render.
- **Blob storage:** profile photos are uploaded to **Cloudinary** and shown in Settings.
- **Settings:** edit your display name, change your profile photo, *Sync now* with last-sync time, a push on/off switch (respected by the server), and a dark theme with a proper dark palette. Settings are saved to your Firestore profile, so they follow you to another phone.
- **Login:** added "Forgot password?" (Firebase reset email).
- **Input validation:** every form validates its fields (dates, times, durations 5–600 min, marks 0–100, lengths) on the device. The API validates them again.
- **Android 15/16 ready:** targets API 36, with edge-to-edge layouts, the notification permission and a predictable back stack.
- **Play Store readiness:** signed release build (R8-shrunk), version 3.0.0 (versionCode 3), store listing, privacy policy and screenshots.
- **Testing:** 23 JVM unit tests (sync merge rules, study planner, exam priority, dates and languages), 10 API tests and an Espresso UI test. Both suites run in GitHub Actions.
