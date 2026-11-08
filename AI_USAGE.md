# AI Usage Disclosure

**Student:** ME Bopape (ST10445847) · **Module:** OPSC6312 · **Submission:** Final PoE (Part 3)

**Tool used:** Claude (Anthropic, *Claude Code*, model Claude Opus 5.5), used as a coding assistant in my own repository.

**How it was used**

- **Code generation.** I gave Claude the PoE brief, my Part 1 Planning and Design document and my Part 2 code. I asked it to implement the Part 3 requirements and my two remaining design features (Examinations, design 3.6; Study Planner, design 3.8/3.12).
  - It wrote most of the new Kotlin: Room entities/DAOs, the `TaskSyncEngine`/`SyncWorker` offline sync, FCM services, Credential Manager Google sign-in, and the exams/study planner screens.
  - It also wrote the Firestore/FCM changes to the Node.js API.
  - Each commit message describes what was added.
- **Design decisions.** I chose which features to build and the scope of offline mode (tasks fully offline, other screens cached). I also chose Cloudinary for blob storage, to avoid a paid Firebase plan. Claude proposed the "local pending change wins" sync rule and the exam-proximity priority boost. I reviewed and accepted both because they match my design's Academic Priority System.
- **Translations.** The Sepedi and isiZulu strings were drafted by Claude. They should be checked by first-language speakers, and any corrections are made directly in `values-b+nso/strings.xml` and `values-zu/strings.xml`.
- **Assets.** The launcher icon, Play Store icon and feature graphic were generated from code by Claude, using the logo I described in Part 1 (graduation cap + tick, blue and white). The UI icons are Google Material Icons (Apache 2.0).
- **Documentation.** Claude drafted the README, these release notes, the store listing, the privacy policy and my video script. I edited them and recorded the video myself.
- **Debugging and tests.** Claude wrote the unit tests (sync merge, study planner, exam priority, dates/languages) and the API tests, and ran the builds and tests to confirm they pass.

**Configuration.** I created the accounts and entered the secret keys myself (Render, Cloudinary, Firebase service-account key). Claude enabled the Firebase sign-in providers, added the SHA-1 fingerprints and created the Firestore database in my browser, with my permission.

**How it is cited.** Code that follows official documentation includes a reference comment (e.g. Android Developers, Firebase, Retrofit, Cloudinary). This file is the citation for AI assistance overall. Commits made with AI assistance carry a `Co-Authored-By: Claude` trailer.

**Verification.** I ran the app on an Android emulator and checked every feature shown in my demonstration video against the live Render API and Firestore console.
