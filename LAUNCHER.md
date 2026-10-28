# Sunset — TV Launcher Build Plan

**Sunset** is a custom Google TV home screen (`:launcher` module, `com.tvplayer.launcher`) modeled on the design mockups: illustrated tiles, a header clock + weather, and a Spa screen backed by Home Assistant. Lives in this repo beside the IPTV player (`:app`), which the **Live TV** tile launches.

Same stack as the player: Kotlin, Compose for TV (`tv-material`), Hilt, DataStore, Retrofit/OkHttp/Moshi.

---

## Screens

| Screen | Reached by | Notes |
|---|---|---|
| **Home** | Opening Sunset from the apps row | Live TV · NostalgiaTV · Movies (top row); Music · Apps · Spa (bottom row); Lights + Settings tray |
| **Spa** | Spa tile | Water temp + status, target temp (focus the card, ◀ ▶ to adjust), Jets 1/2, Lights, Waterfall, Start/Stop jets |
| **Lights** | 💡 tray button | Every `light.*` entity in Home Assistant as a toggle, plus All off |
| **Apps** | Apps tile | All installed TV apps (banners), OK launches |
| **App picker** | Hold OK on an app tile, or Launcher settings → Tiles | Chooses which app a tile opens (or None) |
| **Launcher settings** | Hold the ⚙ button, or Menu on Home | Home Assistant URL/token, entity ids, tile apps |

⚙ (click) opens the Android/Google TV system settings.

Tiles that open apps (all configurable): **Live TV** → `com.iptvplayer.app` by default; **NostalgiaTV**, **Movies**, **Music** → unset until chosen (OK on an unset tile opens the picker).

The Pool tile, the room-service / camera / fan tray icons and the palm logo from the mockup are intentionally left out; the header logo and app banner are a sunset.

## Home Assistant

- REST API with a long-lived access token (HA profile → Security). Polls `/api/states` every 5s, only while a launcher screen is visible.
- Toggles call `homeassistant.turn_on/turn_off` (works for `switch`, `light`, `fan`, `input_boolean`); target temp calls `climate.set_temperature` 0.8s after the last ◀/▶ press.
- Default entity ids — change them in Launcher settings to match your setup:

| Purpose | Default |
|---|---|
| Spa thermostat (water + target temp) | `climate.spa` (`current_temperature`, `temperature`, `min_temp`/`max_temp`, `target_temp_step`) |
| Jets 1 / Jets 2 | `switch.spa_jets_1` / `switch.spa_jets_2` |
| Spa lights | `light.spa` |
| Waterfall | `switch.spa_waterfall` |
| Header weather | `weather.home` (`temperature` attribute + state → icon) |

- Typing a ~180-character token with a remote is painful: open the field, then paste from the **Google TV app on your phone** (its remote has a keyboard that accepts paste).
- `usesCleartextTraffic` is on so `http://homeassistant.local:8123` works.

## Installing on the TV

GitHub Actions (`.github/workflows/build.yml`) builds both apps and runs the unit tests on every push. Each push to `main` publishes a release, so the newest APKs are always at:

- Sunset: `https://github.com/troylairson/tvplayer/releases/latest/download/sunset.apk`
- IPTV Player: `https://github.com/troylairson/tvplayer/releases/latest/download/iptv-player.apk`

Type one into **Downloader** on the TV and install. To update later, download the same link again.

These are optimized (R8) release builds. Each release also has `sunset-debug.apk` and `iptv-player-debug.apk`: unoptimized and noticeably slower, but a fallback if the optimizer ever breaks something. Swap `sunset.apk` for `sunset-debug.apk` in the link to get one.

**One-time signing setup:** run `scripts/create-signing-key.sh` on a computer and add the four secrets it prints under the repo's *Settings → Secrets and variables → Actions*. Keep the `.keystore` file it creates. Until the secrets exist, each build is signed with a throwaway key, so updating means uninstalling first (which clears the app's settings).

## Running as a regular app (current)

Sunset does **not** register as a home screen yet. It shows in the apps row like any other app, Back on its home screen exits, and the stock Google TV home is untouched. This avoids Android prompting "choose your home app" after install.

## Making it the default home screen (later)

1. Add the `HOME` intent filter back to `MainActivity` (it's in a comment in `AndroidManifest.xml`).
2. Google TV has no "choose home app" setting, so the stock launcher must be disabled over adb (reversible):

```sh
adb install launcher/build/outputs/apk/debug/launcher-debug.apk
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx   # Google TV home
# undo: adb shell pm enable com.google.android.apps.tv.launcherx
```

Once Sunset is the default home, Back on its home screen is swallowed automatically (checked on every resume). On some builds `com.google.android.tungsten.setupwraith` relaunches the stock home after reboot and must be disabled too — check on the actual Onn/Chromecast before relying on it.

## Phases

### ✅ L1 — Home, Spa, Lights, Apps, settings — as a regular app
Code complete; **not yet built** (dev sandbox can't reach Google Maven). Pure logic (spa status, weather mapping, URL normalization) has JVM unit tests.

### 🔲 L2 — On-device pass
- Build both APKs, install, open Sunset from the apps row; confirm tile focus/scale, art proportions at 1080p, long-press OK on tiles
- Back from Home exits to Google TV
- Wire real HA entity ids; confirm optimistic toggles settle correctly with the 5s poll

### 🔲 L2.5 — Home screen replacement
- Re-add the `HOME` intent filter, disable the stock launcher (see above)
- Check the Home button returns to Sunset (and to its home screen from sub-screens), and Back on Home stays put

### 🔲 L3 — Live updates + polish
- Home Assistant WebSocket API (`subscribe_entities`) instead of polling — instant state changes, less traffic
- Custom font matching the mockups; bundle as a resource
- Tile art refinements; subtle focus animation on art (e.g. sun rises on Live TV focus)

### 🔲 L4 — IPTV integration
- **NostalgiaTV** → IPTV player filtered to a "classic" category via a deep link (needs the player's Phase 7 `iptv://` handling, extended with e.g. `iptv://guide?category=…`)
- Live TV tile subtitle with the last-watched channel / now playing (needs a small content provider or broadcast from the player)

## Gotchas

- **Package visibility (API 30+)** — the manifest `<queries>` for `LEANBACK_LAUNCHER`/`LAUNCHER` intents is what lets the launcher see and start other apps
- **`singleTask` + Home** — pressing Home while the launcher is in front arrives as `onNewIntent(CATEGORY_HOME)`; the nav graph pops back to Home
- **Back on Home** — swallowed only while Sunset is the default home (a launcher must never finish itself); as a regular app it exits
