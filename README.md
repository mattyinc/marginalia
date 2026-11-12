# Marginalia

Short ideas from good books, kept in the margin. A personal Android app for phone and tablet.

- **Stories** — short histories and ideas told in animated scenes and read aloud by the device's voice. Tap the right side to skip, the left to go back, hold to pause.
- **Ideas** — a stack of idea cards. Swipe right to keep one in a stash, left to pass, tap to open.
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

With a keyboard attached: `←` pass, `→` keep, `Enter` open, `Esc` close, `N` new idea, `1` / `2` / `3` switch tabs.

## Stories

`app/src/main/assets/stories.json` holds the stories. Each scene has a `say` line (caption and narration)
and a `kind` that picks its drawing: `title`, `words`, `number`, `timeline`, `bars`, `curve` or `list`.
Add a story by adding an entry to that file.

Scene illustrations are drawn in `art/scenes.py` and named `scene_<story id>_<scene number>`; the app
picks them up by that name. Cover illustrations are drawn in `art/build.py`, which writes the SVG sources to `art/` and the
images the app uses to `res/drawable-nodpi`. Covers are matched to stories in `ui/StoryArt.kt`.

## Starter ideas

`app/src/main/assets/seed.json` holds the ideas loaded on first launch: summaries, in original
wording, of passages from Scripture and public-domain authors. Edit that file to change them.
