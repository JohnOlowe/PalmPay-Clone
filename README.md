# PalmPay Clone

A Java and XML recreation of the PalmPay-style wallet home screen. It is an offline-first visual demo: no network permission or payment credentials are required.

## What is included

- Edge-to-edge wallet home screen with a masked balance, earnings row, transaction history, and Add Money CTA.
- Header profile/support/notification controls and branded quick actions.
- Reusable service grid and savings promotion cards populated from immutable Java models.
- Working balance visibility toggle, tap feedback, and bottom navigation selection.
- Transfer-to-bank screen with a searchable online Nigerian bank directory, logo loading, offline fallback data, and recent recipients.
- Sticky transfer tabs while the recipient list scrolls.
- Local vector drawables and shape resources so the UI stays sharp on every density.

## Build

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`. The project uses Java 17, Android XML layouts, AndroidX, ConstraintLayout, and Material Components.

See [ASSETS.md](ASSETS.md) for the visual asset sources and licensing notes.
