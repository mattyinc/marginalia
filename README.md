# Marginalia

Short ideas from good books, kept in the margin. A personal Android app for phone and tablet.

- **Today** — a stack of idea cards. Swipe right to keep one in a stash, left to pass, tap to open.
- **Library** — your stashes and everything in them, with search across every idea.
- **New idea** — the gold plus button. Write down an idea from whatever you are reading.
- **Your note** — open any idea and write beside it.

Everything is stored on the device. There is no account and no server.

## Getting the app

Every push to `main` builds an APK on GitHub Actions and publishes it under **Releases**.
Open the latest release on the tablet, download `marginalia.apk` and install it. Builds are
signed with the same key, so a new build installs over the old one and keeps your library.

## Building locally

```
./gradlew assembleDebug
```

Needs JDK 17 and the Android SDK (command-line tools are enough).

## Keyboard

With a keyboard attached: `←` pass, `→` keep, `Enter` open, `Esc` close, `N` new idea, `1` / `2` switch tabs.

## Starter ideas

`app/src/main/assets/seed.json` holds the ideas loaded on first launch: summaries, in original
wording, of passages from Scripture and public-domain authors. Edit that file to change them.
