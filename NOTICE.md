# Third-Party Notices — MishkatLauncher

This file tracks every direct dependency and data asset license per **Rule R2** and **§12** of `PROJECT_BRIEF.md`.

## Direct Code Dependencies (Sprints S0–S5)

| Artifact | Version | License | Source |
|---|---|---|---|
| `org.jetbrains.kotlin:kotlin-stdlib` / Compose compiler plugin / Serialization plugin | 2.4.21 | Apache-2.0 | https://github.com/JetBrains/kotlin |
| `com.google.devtools.ksp:symbol-processing-api` (build-time KSP plugin) | 2.3.13 | Apache-2.0 | https://github.com/google/ksp |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.11.0 | Apache-2.0 | https://github.com/Kotlin/kotlinx.coroutines |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.12.0 | Apache-2.0 | https://github.com/Kotlin/kotlinx.serialization |
| `androidx.core:core-ktx` | 1.19.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| `androidx.activity:activity-compose` | 1.13.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.11.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| `androidx.compose:compose-bom` (`foundation`, `ui`, `ui-tooling`) | 2026.09.00 | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| `androidx.tv:tv-material` | 1.1.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/tv |
| `androidx.datastore:datastore-preferences` | 1.3.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| `androidx.media3:media3-exoplayer` / `media3-exoplayer-hls` / `media3-ui` / `media3-session` / `media3-common` | 1.11.1 | Apache-2.0 | https://github.com/androidx/media |
| `androidx.room:room-runtime` / `room-ktx` / `room-compiler` | 2.8.5 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/room |
| `io.coil-kt.coil3:coil-compose` | 3.6.3 | Apache-2.0 | https://github.com/coil-kt/coil |
| `com.batoulapps.adhan:adhan` | 1.2.1 | MIT | https://github.com/batoulapps/adhan-java |
| `androidx.work:work-runtime-ktx` | 2.12.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/work |
| `junit:junit` (test only) | 4.13.2 | EPL-1.0 | https://github.com/junit-team/junit4 |

## Data Sources & Attributions (S1–S5)
- **Islamic Radio Streams (`assets/stations_snapshot.json`)**: sourced from the public MP3Quran API (`https://mp3quran.net/api/v3/radios`).
- **Offline Cities Coordinates (`assets/prayer/cities_pack.json`)**: geographic coordinates and timezones derived from GeoNames (`https://www.geonames.org/`, licensed under **CC-BY 4.0**).
- **Adhkar Texts (`assets/prayer/adhkar.json`)**: classical prophetic supplications selected from *Hisn al-Muslim* (Public Domain heritage text).
- **Weather Forecasts**: Open-Meteo API (`https://open-meteo.com/`).
