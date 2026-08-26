# PalmPay Clone

A Java and XML recreation of the PalmPay-style wallet home screen. It is an offline-first visual demo: no network permission or payment credentials are required.

## What is included

- Edge-to-edge wallet home screen with a masked balance, earnings row, transaction history, and Add Money CTA.
- Header profile/support/notification controls and branded quick actions.
- Reusable service grid and savings promotion cards populated from immutable Java models.
- Working balance visibility toggle, tap feedback, and bottom navigation selection.
- Transfer-to-bank screen with a searchable online Nigerian bank directory, logo loading, offline fallback data, and recent recipients.
- Progressive account-history suggestions, trusted-recipient confirmation, and an amount-entry page.
- Sticky transfer tabs while the recipient list scrolls.
- Profile balance customisation persisted with SharedPreferences and reused by the home/amount screens.
- Local vector drawables and shape resources so the UI stays sharp on every density.

## Build

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`. The project uses Java 17, Android XML layouts, AndroidX, ConstraintLayout, and Material Components.

See [ASSETS.md](ASSETS.md) for the visual asset sources and licensing notes.

## Paystack automatic bank retrieval

The Transfer to Bank screen resolves a completed 10-digit account number to its bank using, in order: the local transfer history, the CBN NUBAN check-digit algorithm, and (when configured) Paystack account resolution for wallet providers such as OPay and PalmPay. To enable the Paystack step, put your secret key in `local.properties` as `PAYSTACK_API_KEY=sk_...` (or export `PAYSTACK_API_KEY`); without a key the app gracefully skips that step.
