# IPTV Player — Build Plan

Android TV app (Kotlin + Jetpack Compose for TV) targeting Chromecast HD and Onn Google TV boxes. Sideloaded APK, no Play Store. Designed to later embed as one tile inside a custom Android TV launcher.

---

## Tech Stack

| Layer | Library |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose for TV (`androidx.tv:tv-material`) + Compose Foundation lazy lists |
| Video | Media3 / ExoPlayer |
| DI | Hilt |
| Database | Room |
| Preferences | DataStore |
| Network | Retrofit + OkHttp + Moshi |
| Images | Coil |
| Async | Kotlin Coroutines + Flow |

**Target:** compileSdk 35, minSdk 26, Gradle 8.9, AGP 8.7

---

## Input Sources

- **Xtream Codes API** — authenticate with server URL + username + password; fetch live categories, live streams, and short EPG
- M3U + XMLTV support planned as Phase 4 enhancement

---

## Phases

### ✅ Phase 1 — Auth + Channel List
- Xtream Codes API client (Retrofit + Moshi)
- Credential storage (DataStore, plain text, `allowBackup=false`) — encryption deferred to Phase 7
- Room DB: favorites, recents, hidden categories
- Settings screen: enter credentials, test connection, navigate to guide on success
- Guide screen stub: shows channel count + plays first channel
- Player screen stub: shows stream ID
- Full nav graph, Hilt DI wired, dark TV theme

### ✅ Phase 2 — Full-Screen Playback + D-pad + OSD
- `PlayerManager` — singleton ExoPlayer with:
  - Low-latency buffer config (1.5s to playback, ~2s tune time)
  - Frame-rate switching only when seamless (`VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS`) — this is *not* tunneling; tunneling (`setTunnelingEnabled`) is untested and off
  - Decoder fallback enabled
  - Streams loaded through OkHttp (`OkHttpDataSource`) so cross-host / http↔https redirects work
  - `BehindLiveWindowException` → automatic rejoin at the live edge
  - Stopped in `MainActivity.onStop`, resumed at the live edge in `onStart` (no audio after Home)
- **D-pad controls**: Up/Down = channel change, OK/Menu = toggle OSD, Back = guide
- **Tuning animation** — animated pixel static on `Canvas`, disappears on first frame rendered
- **OSD banner** (slides from top, auto-dismisses after 5s):
  - Channel logo (Coil), number, name, country flag emoji
  - Now-playing show title + `LinearProgressIndicator` + minutes remaining
  - 3-hour EPG timeline with current event highlighted and hour tick markers
- Short EPG fetched from Xtream API per channel, cached in memory (title/description are base64-decoded)

### 🔲 Phase 3 — Channel Guide

**Prerequisites (from the Phase 1–2 review — do first):**
- Cancel the in-flight EPG job on each tune (stale EPG can overwrite the banner while surfing)
- Debounce tuning ~300ms on held Up/Down; only `recordRecent` after ~5s of watching
- Load channels once in `GuideViewModel.init`, not `LaunchedEffect(Unit)` (currently refetches every return)
- Expire the short-EPG cache once cached events have ended
- Cache *all* channels; derive favorites/recents/hidden via `combine` with the Room flows (not baked into `Channel` at fetch time)
- `PlayerView` `AndroidView`: `onRelease = { it.player = null }`
- Rework `TuningOverlay` (~32k `drawRect` per frame) into a few pre-generated noise bitmaps

**Guide:**
- Guide stays a separate nav destination (`guide`), with Player pushed on top as today; Back from Player returns to it, and the guide is reachable from the Player via a dedicated key (Guide / `KEYCODE_GUIDE`)
- Five tabs: **All / Favorites / Recent / News / Sports**
  - Sports tab: live-game marker (pulsing red dot) based on EPG title heuristics
- `LazyColumn` (Compose Foundation 1.7+, **not** the deprecated `TvLazyColumn`) channel rows: logo, number, name, now-playing show from EPG cache
- Long-press OK → toggle favorite (Room)
- Recent tab: last 50 watched channels ordered by time
- Search: activates on letter key, filters by name + category
- **Live preview cell** (bottom-right corner): a small `PlayerView` attached to the *same* `PlayerManager` ExoPlayer, showing the last-watched channel. No second decoder instance and no re-buffer — the player just switches surface when the guide opens/closes
- Category filtering: hidden categories excluded from all tabs
- Guide → Settings entry point (`onOpenSettings` is not wired to any key yet)

### 🔲 Phase 4 — Category Filtering + EPG Refresh
- Category manager in Settings: checklist of all categories, toggle hide/show
- Changes propagate via `StateFlow` to guide in real time
- Background EPG refresh every hour via coroutine loop in `IptvApplication`
- Full XMLTV pull (`/xmltv.php`) parsed with SAX parser (not DOM — avoids OOM)
- Short EPG used for OSD banner and guide rows; full XMLTV used for timeline accuracy

### 🔲 Phase 5 — Captions (CEA-608)
- Force CEA-608 extraction from MPEG-TS streams that don't announce caption tracks
- `DefaultExtractorsFactory` with `TS_EXTRACTOR_FLAG_DETECT_ACCESS_UNITS`
- `setSelectUndeterminedTextLanguage(true)` on track selector
- Remote key (Info / long-press OK) toggles captions on/off
- Caption font size stored in DataStore preferences

### 🔲 Phase 6 — Multi-View
- 2×1 and 2×2 grid layouts, each cell an independent ExoPlayer instance
- All cells muted except the active cell
- Minimal buffer config per cell to limit memory use
- D-pad navigates between cells; OK goes full-screen on active cell
- Entry: long-press guide button or `KEYCODE_PROG_BLUE`
- Hardware limit: Chromecast HD (Amlogic S905Y4) supports 2 concurrent hardware decode sessions; cells 3 & 4 in 4-up mode will software-decode (acceptable quality tradeoff at lower bitrates)

### 🔲 Phase 7 — Polish + Launcher Integration
- Channel logo retry interceptor (Coil) — up to 3 retries with 500ms delay, initials fallback
- Country flag assets bundled locally (no network fetch)
- Credential encryption (Keystore-wrapped key; `security-crypto` is deprecated — drop that dependency)
- Login: check `user_info.auth`/`status` and make auth models nullable so bad credentials give a clear error; normalize server URL scheme; URL-encode user/pass in stream URLs
- HLS → MPEG-TS fallback (`.ts`) when `.m3u8` fails
- Deep-link entry point: `iptv://play?stream_id=X` intent filter on `MainActivity` so a future launcher can tune directly to a channel (PlayerViewModel must load channels itself when the in-memory cache is empty — same fix covers restore after process death)
- ProGuard rules, APK size check, sideload smoke test on physical hardware

---

## Key Architecture Decisions

### Single Activity + Compose Navigation
One `MainActivity` hosts the entire nav graph. TV remote key events (`onKeyDown`) must be handled in one place — multiple activities create routing nightmares.

### PlayerManager as Application-Scoped Singleton
The primary ExoPlayer lives in `PlayerManager` (`@Singleton` via Hilt). This means:
- Buffer is preserved when the guide opens — no re-buffering on dismiss
- The guide preview is the same player on a different (smaller) surface, not a second decode
- Because it outlives screens, `MainActivity` must stop it in `onStop` and resume in `onStart`
- Config changes (rare on TV) don't destroy the player

Multi-view players are `@ActivityRetainedScoped` — released when leaving multi-view.

### Room for Lists, DataStore for Credentials
Favorites and recents are relational (queried, joined with channel data) → Room. Credentials are a single opaque blob → DataStore. Never mix them.

### Xtream Stream URL Construction
Built in `ChannelRepository`, not in the player. Player receives a completed URI.
- HLS: `{server}/live/{user}/{pass}/{id}.m3u8` (preferred)
- MPEG-TS: `{server}/live/{user}/{pass}/{id}.ts` (fallback)

### EPG Strategy
- **Short EPG** (`get_short_epg&stream_id=X&limit=5`) → OSD banner and guide rows (fast, per-channel)
- **Full XMLTV** (`/xmltv.php`) → background refresh every hour, SAX-parsed, in-memory map

---

## Android TV Gotchas

- **`PlayerView` must be `AndroidView`**, not the experimental Compose shim — surface lifecycle races cause black screens
- **Compose TV focus is aggressive** — in multi-view, disable it with `Modifier.focusProperties { canFocus = false }` and handle D-pad manually
- **Chromecast HD: 2 hardware decode sessions max** — the guide preview reuses the main player (1 slot); multi-view 4-up needs software fallback for cells 3 & 4
- **D-pad Up/Down intercepted by system launcher** — `PlayerScreen` must consume these keys and return `true`; use `Modifier.focusable()` + `FocusRequester.requestFocus()` on the root `Box`
- **OSD banner auto-dismiss** — use a `Job` in the ViewModel, not `LaunchedEffect`, to avoid recomposition jank on every channel change
- **CEA-608 on Amlogic** — firmware may intercept 608 at the HAL level; test on physical hardware early
- **Cleartext HTTP** — most Xtream servers are `http://`; `usesCleartextTraffic="true"` is required at targetSdk 28+
- **Xtream short EPG is base64** — `title`/`description` must be decoded (some panels send plain text; decoder falls back)
- **Two Material libraries** — use `androidx.tv.material3.Text`; mobile M3 components (`OutlinedTextField`, progress indicators) read their own theme, so `IptvTheme` also provides a dark mobile `MaterialTheme`
- **ExoPlayer error → STATE_IDLE** — `onPlaybackStateChanged(IDLE)` fires right after `onPlayerError`; don't let it overwrite the error state
- **HLS live stream buffering** — default Media3 `DefaultLoadControl` buffers 50s ahead (10-15s tune delay); override to 2s min / 10s max
