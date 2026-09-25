# Part 2 Demo Video Guide

Everything you need to get set up and record the Part 2 demonstration video, in
order. Keep this open on a second screen while you record.

---

## 1. Before you record — setup checklist

Do these once, in order. Don't record until all boxes are ticked.

- [ ] **Firebase project created**, real `google-services.json` placed at
      `app/google-services.json` (replacing the placeholder)
- [ ] **Email/Password sign-in enabled** in Firebase Authentication
- [ ] **Backend deployed** on Render, and `API_BASE_URL` in `app/build.gradle`
      updated to your real Render URL
- [ ] App **rebuilt** after both of the above changes (Gradle sync + rerun)
- [ ] You know your **own Firebase UID** (Firebase Console → Authentication →
      Users → copy the value in the "User UID" column, once you've registered
      at least one test account) — you'll need this for the API proof in the
      video
- [ ] Screen recording software chosen and tested for 10 seconds (see §2)
- [ ] Quiet room / decent mic for the voice-over

### 1.1 Firebase setup (detailed)

1. Go to <https://console.firebase.google.com> and sign in with any Google account.
2. Click **Add project**. Name it `Student Assistant` (or anything — the name
   is cosmetic). Click **Continue**.
3. On the Google Analytics screen, toggle it **off** (not needed for this
   project) and click **Create project**. Wait for provisioning, then
   **Continue**.
4. On the project overview page, click the **Android icon** (</> is web, the
   little Android robot is what you want) to add an Android app.
5. **Android package name**: enter exactly `com.bopape.studentassistant`
   (must match `applicationId` in `app/build.gradle` — case-sensitive, no
   typos). App nickname is optional. Leave the SHA-1 field blank for now (it's
   only needed later for Google Sign-In in the Final PoE).
6. Click **Register app**, then **Download google-services.json**.
7. Move that downloaded file into your project, replacing the placeholder at
   `app/google-services.json` (same filename, same location).
8. Click through the remaining "Add Firebase SDK" steps in the console (no
   action needed — this repo is already configured) and click **Continue to
   console**.
9. In the left sidebar: **Build → Authentication → Get started**.
10. On the **Sign-in method** tab, click **Email/Password**, toggle it
    **Enable**, click **Save**.
11. Back in Android Studio: **File → Sync Project with Gradle Files** (so the
    new `google-services.json` takes effect), then run the app.
12. **Verify it worked**: in the app, register a test account. Then in the
    Firebase console, go to **Authentication → Users** — your new account
    should appear in the table within a few seconds. This is exactly what
    you'll show on camera later.

### 1.2 Backend deployment (recap — full detail in `backend/README` section of the main README)

1. On <https://render.com>, sign up/log in, click **New → Blueprint**.
2. Connect your GitHub account if prompted, select the
   `ElijahBopape/Open-Source-Coding-` repository. Render reads
   `backend/render.yaml` automatically and shows a service called
   `student-assistant-api` — click **Apply**/**Deploy**.
3. Wait for the first deploy to finish (a few minutes). Copy the URL shown at
   the top of the service page (looks like
   `https://student-assistant-api-xxxx.onrender.com`).
4. In `app/build.gradle`, replace both `API_BASE_URL` values with that URL
   (keep the trailing `/`). Rebuild the app.
5. Sanity check from a terminal: `curl https://your-url.onrender.com/` should
   return `{"status":"ok","service":"student-assistant-api"}`.

> Free Render services "spin down" after 15 minutes of no traffic and take
> ~30-60 seconds to wake up on the next request. Open the app once a couple of
> minutes before you start recording so the server is already warm and your
> video doesn't have an awkward loading pause.

---

## 2. Recording setup

**Emulator is fine for Part 2** — the rubric only requires a physical device
for the *Final PoE* (Part 3). Use whichever is more convenient right now.

Recording software options:
- **Android Studio's emulator toolbar** has a built-in screen record button
  (the dot inside a circle icon in the emulator's side panel) — records the
  emulator screen only, no system audio. Good if you'll voice-over separately
  or narrate live with a mic picked up by your recording tool.
- **Windows**: `Win + G` (Xbox Game Bar) records your whole screen + mic
  narration in one file — simplest option if using the emulator inside
  Android Studio, since it captures everything including any terminal/browser
  windows you switch to.
- **Physical phone**: most Android phones have a built-in screen recorder in
  the quick-settings panel (swipe down twice), which can include mic audio.

Do a 10-second test recording first and check the audio is actually being
picked up before doing the full take.

---

## 3. Shot list / script

Speak naturally — this is a guide for content and order, not a script to read
word-for-word. Aim for roughly 4-7 minutes total: enough to show everything
clearly without padding.

### Scene 1 — Intro (10-15s)
**Say:** your name, student number, module (OPSC6312), and one sentence on
what the app does: *"Student Assistant is an academic organiser for tertiary
students — it brings modules, a class timetable and prioritised tasks
together in one app."*
**Show:** the app icon / launch screen.

### Scene 2 — Register + password security (30-45s)
**Do:** tap "Create account", fill in name/email/password, submit.
**Say:** *"Passwords are never stored or sent as plain text — Firebase
Authentication hashes them, and my app never has access to the raw
password."*
**Show:** immediately after, alt-tab to the Firebase console → Authentication
→ Users tab, point out the new user just appeared with its UID. *"You can see
the account was created in Firebase's hosted authentication service, and
there's no password field visible anywhere — Firebase manages that
securely."*

### Scene 3 — Login + invalid input handling (20-30s)
**Do:** log out (Settings → Sign out), then on the login screen tap "Log in"
with both fields empty.
**Say:** *"The app validates input instead of crashing or silently failing."*
**Show:** the inline error message appears under the email field.
**Do:** then log in properly with the account from Scene 2.

### Scene 4 — Settings (20-30s)
**Do:** open Settings, change the language dropdown, toggle notifications
off/on, toggle dark theme.
**Say:** *"Settings are saved immediately and persist — if I leave this
screen and come back, or restart the app, they're unchanged."*
**Show:** navigate to another tab and back (or restart the app) to prove the
toggle states stuck.

### Scene 5 — Modules (30-45s)
**Do:** add a module (e.g. name "Open Source Coding", code "OPSC6312",
lecturer, venue). Edit it. Optionally add a second module.
**Say:** *"Modules are the foundation — tasks and timetable entries both link
back to a module."*

### Scene 6 — Tasks + Academic Priority System (45-60s)
**Do:** add 2-3 tasks with different priorities and due dates, linked to a
module. Show the "All / High / Today / Done" filter chips. Mark one complete.
**Say:** *"This is the Academic Priority System from my design document —
tasks aren't just sorted alphabetically, they're ordered by priority and then
by how soon they're due, so the most urgent work always rises to the top."*
**Show:** point out the ordering on screen matches that rule.

### Scene 7 — Timetable (20-30s)
**Do:** add a class entry for today's day of the week, linked to a module.
Switch day chips to show it only appears under the correct day.
**Say:** briefly explain the day-chip filtering.

### Scene 8 — Dashboard (15-20s)
**Do:** open the Dashboard tab.
**Say:** *"The dashboard pulls this together automatically — it's showing the
class I just added as the next class, and the high-priority task from
before, both fetched live from the API, not hard-coded."*

### Scene 9 — Proof of the REST API + database (45-60s)
This is the part the brief specifically asks for: showing data really is
stored in the hosted API/database, not just on the device.

**Do:** open a terminal (or Postman) and run:
```bash
curl https://your-render-url.onrender.com/api/modules -H "X-User-Id: YOUR_FIREBASE_UID"
```
(use the UID you copied from Firebase Authentication in step 1 of the setup
checklist — it must be the same account you're logged in as in the app)

**Say:** *"This calls my REST API directly, outside the app, with my
account's ID. The JSON that comes back is the exact module I created a
minute ago — proving it's genuinely stored on my server, not just held
locally on the phone."*
**Show:** the JSON response on screen, and point out it matches what's shown
in the app.

*(Optional, if time allows: repeat for `/api/tasks` or `/api/timetable`.)*

### Scene 10 — Wrap-up (10-15s)
**Say:** summarise what was implemented (register/login with encryption,
settings, custom REST API + database, and the three features), and mention
that SSO, offline sync, push notifications and multi-language are designed
for (show the Settings language field and the "Continue with Google" button
again briefly if you like) and scheduled for the Final PoE, as the brief
allows.

---

## 4. After recording

1. Upload the video to YouTube, set visibility to **Unlisted**.
2. Copy the link and replace `[Video link - to be added]` in `README.md` with
   it.
3. Commit and push that README change.
