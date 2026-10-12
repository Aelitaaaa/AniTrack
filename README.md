# AniTrack

A native Android anime tracking app built with Kotlin and Jetpack Compose.

Find anime, save favorites, and keep track of the episode you left off on. No account or API key is required. Android 8.0 and newer are supported.

Download the signed release APK from [GitHub Releases](https://github.com/Aelitaaaa/AniTrack/releases).

## Features

- Discovery lists for currently airing, popular, top-rated, and upcoming anime.
- Debounced search with pagination, format/genre/airing filters, ordering, and a removable local search history.
- Anime details, synopsis, genres, studios, main characters, related anime, and YouTube trailers when available.
- Favorites and a watchlist stored on the device.
- Plan to watch, watching, and completed statuses with manual episode editing and progress controls.
- Light, dark, pure black, and system themes with saved preferences. Cover grid or list layouts for the library.
- Recently viewed titles, capped at 30 entries. Search history is capped at 15 entries.
- Cached details for previously opened anime, with an offline fallback when the catalog cannot be reached.
- A local profile with a display name, username, bio, and watch statistics. No online registration or password is needed.
- Five persistent anime badges: Pendatang Baru, Mulai Nonton, Kolektor, Maraton, and Sepuh Anime.
- Automatic catalog fallback to AniList and saved discovery lists for offline browsing, with an optional manual source selection.
- Custom collections with multiple memberships per anime, renaming, and removal.
- Personal ratings from 1 to 10 and private notes on each anime.
- Continue-watching shelf and statistics for episode progress, formats, and personal ratings.
- Incognito mode that hides history and stops recording new searches and viewed titles.
- JSON backup and restore for profiles, badges, lists, collections, notes, settings, and histories.

Saved lists and preferences work without an internet connection. Discovery, search, new details, trailers, and uncached artwork need internet access. AniTrack tracks anime; it does not provide episodes or streaming.

## Tech stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Lifecycle ViewModel, coroutines and StateFlow, Retrofit, OkHttp, Kotlin Serialization, Room, DataStore, Hilt, and Coil.

Dependency versions are pinned in [`gradle/libs.versions.toml`](gradle/libs.versions.toml). The project uses AGP's built-in Kotlin support and KSP for code generation.

## Architecture

Screens observe ViewModel state. ViewModels use a catalog repository, a local library repository, and a preferences repository. Retrofit handles catalog requests; Room stores the library and cached details. Episode bounds and status transitions are pure Kotlin logic in `WatchProgress`.

The catalog shares a request gate across screens and retries an HTTP 429 once. If Jikan is slow, an AniList request starts after two seconds; the first successful response wins and the other request is cancelled. The app temporarily prefers a working backup and avoids repeating connection timeouts for every section when both providers fail. AniList requests stay below its reduced 30 requests/minute quota. Both providers use MyAnimeList ids in saved lists; entries without a MAL id are excluded. Discovery snapshots and details are saved in Room for offline use. Database updates to episode progress run in transactions. Version 2 adds collections, collection memberships, and personal ratings/notes through an explicit migration; the original tables are preserved. Library sorting and statistics use local data.

Profiles and earned badges use DataStore alongside the existing theme preference. A profile belongs to this device and uses its current favorites and watchlist. Badges unlock at these milestones: create a profile; record one episode; save five favorites; record 100 episodes; finish 25 anime and record 500 episodes for Sepuh Anime. Earned badges remain unlocked even if entries are later removed from the watchlist.

## Using your library

Open **Favorites → Collections** to create or rename a collection. On an anime detail page, use **Collections** to add the title to one or more groups. Deleting a collection removes its memberships while keeping favorites and watch progress.

Use **Rating and notes** on the detail page to save a personal score or a note. Personal ratings do not change catalog scores. The library can be filtered by title and sorted by recent activity, title, score, or watched episodes. Choose list or cover grid in **Profile → Settings**.

Open **Profile → View statistics** for recorded episode counts, watch statuses, format breakdowns, and personal ratings. Titles with unknown episode totals are shown separately.

## Backup and privacy

In **Profile → Settings → Backup and restore**, export a `.anitrack.json` file to a location selected with Android's document picker. Restore validates the whole file before writing, merges lists, keeps newer episode progress and notes, and matches collections by normalized name. Reimporting a file does not duplicate memberships. An existing profile is kept when the incoming profile has a different id. Profiles with the same id combine earned badges.

Backups include personal notes and histories. They do not include remote artwork, discovery snapshots, or downloaded anime episodes. The file limit is 10 MB. Database changes are applied in one transaction; profile/settings restoration uses DataStore separately and reports a partial restore if that storage fails.

Incognito hides existing history and stops recording new searches and viewed titles. Manual changes to favorites, collections, ratings, and watch progress are still saved. It does not hide requests from catalog providers. If preferences cannot be read, history recording stops.

## Getting started

Requirements:

- Android Studio compatible with Android Gradle Plugin 9.4.
- JDK 17.
- Android SDK platform 37.0 and Build Tools 36.0.0.

Clone the repository and open its root folder in Android Studio. Let Gradle sync, then run the `app` configuration on an emulator or Android device. Android Studio creates `local.properties` for your SDK path; keep that file out of Git.

## Building

```sh
bash ./gradlew test lintDebug assembleDebug
```

On Windows:

```powershell
.\gradlew.bat test lintDebug assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. It uses the local Android debug signing key.

Tests cover API mapping and failures, provider fallback and cancellation, persistent catalog caching, database uniqueness and transactions, history limits, episode bounds and status changes, profile persistence and validation, badge thresholds, saved preferences, version 1-to-2 migration, collection uniqueness, backup validation/merge/idempotency, privacy, filters, source selection, statistics, and Compose episode/profile/collection/rating controls. Room and Compose tests use Robolectric; a device is not required. Compose controls are tested in the debug variant.

GitHub Actions runs the tests, both lint variants, and debug/release builds, then uploads APKs and reports as artifacts. Live catalog checks are enabled for these builds; local test runs skip them unless `ANITRACK_LIVE_TESTS=true`.

To build and lint an optimized release:

```sh
bash ./gradlew assembleRelease lintRelease
```

The manual **Release APK** workflow runs the tests, both lint variants, debug and optimized release builds, and opt-in live catalog checks. It uploads `app/build/outputs/apk/release/app-release-unsigned.apk` and reports. Release builds use R8 and resource shrinking. Signing is performed separately with a private release key; signing keys and passwords are not included in the repository. An unsigned APK must be aligned and signed before installation.

## API

Anime information and remote artwork come from the [Jikan API](https://jikan.moe/), an unofficial API for [MyAnimeList](https://myanimelist.net/), and the [AniList GraphQL API](https://docs.anilist.co/). Either provider can be unavailable or rate-limited. Scores, summaries, episode counts, and artwork depend on the active source. Characters are loaded separately and limited to twelve entries in the UI.

Artwork is loaded from source URLs and cached by Coil. Anime artwork is not bundled with the app or included in this repository. The app requests safe-for-work catalog results; availability and classification are controlled by the upstream service.

## Project structure

| Directory | Responsibility |
| --- | --- |
| `app/src/main/java/com/dzaky/anitrack/data` | API models, Room, preferences, and repositories |
| `app/src/main/java/com/dzaky/anitrack/domain` | Anime models and episode rules |
| `app/src/main/java/com/dzaky/anitrack/presentation` | Screens, ViewModels, theme, and navigation |
| `app/src/main/java/com/dzaky/anitrack/di` | Hilt providers |
| `app/schemas` | Versioned Room database schema |
| `app/src/test` | Repository, database, mapping, and preference tests |
| `app/src/testDebug` | Compose controls tests |

## License

The application code is available under the [MIT License](LICENSE). Anime information and artwork remain the property of their respective owners.
