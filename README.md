# Qiyam 🌙

An Android alarm that wakes you at the start of the **last third of the night** (between Maghrib and Fajr), recalculated every day for where you are.

## What it does

- **Night dial**: the night drawn as an arc from Maghrib to Fajr, split into thirds, with the moon showing where you are and a live countdown.
- **A real alarm**: uses Android's alarm-clock API, so it rings on the minute even in deep sleep. The sound fades in, it vibrates, and a full-screen page opens over the lock screen with a reminder (verse or hadith) and the waking dua.
- **Gentle heads-up** a few minutes before (optional).
- **Telegram**: when the alarm rings, your bot messages you with tonight's times and a reminder. Its **✅ I'm up** and **😴 Snooze** buttons control the alarm on your phone. If you don't respond within 3 minutes you get a nudge; after 10 minutes the alarm stops and the bot keeps listening for a late "I'm up".
- **Streaks**: nights you got up, your best run, and the last 14 nights at a glance. Telegram celebrates them too.
- **Self-healing**: re-arms itself after a reboot, an update, or a time-zone change. A setup checklist shows anything Android or Samsung is blocking.

Prayer times are computed on the phone with [Adhan](https://github.com/batoulapps/adhan-java). A high-latitude rule keeps UK summer nights sane.

## Install

Every push to `main` builds the APK in GitHub Actions and publishes it under **Releases**.
Open the latest release on your phone, tap `Qiyam.apk`, and allow installs from your browser when asked.

## Samsung phones

1. Open the app and fix everything on the **Finish setting up** card.
2. Settings › Battery › Background usage limits › **Never sleeping apps** › add Qiyam.
3. Tap **Test the full alarm in 10 seconds**, then lock the phone.

## Telegram

1. In Telegram, message **@BotFather** › `/newbot` › copy the token.
2. In Qiyam, paste the token and tap **Connect**. The app opens your bot; press **Start**.
3. Tap **Send test**.

The token is stored only on your phone.
