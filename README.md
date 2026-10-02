# Lean Pedometer

A deliberately small Android pedometer for the Lean app family.

## MVP

- Reads Android's low-power `TYPE_STEP_COUNTER` sensor.
- Runs continuous tracking in a foreground service of type `health`.
- Stores daily totals locally for 400 days.
- Restarts enabled tracking after reboot/package replacement.
- Shows today's total and the most recent 7 days.
- Exposes a read-only provider for Lean Diary.
- Does not require Health Connect.

## Lean Diary handoff

Lean Diary can query:

`content://com.cpkr.leanpedometer.steps/steps/today`

or:

`content://com.cpkr.leanpedometer.steps/steps/YYYY-MM-DD`

See [docs/INTEGRATION.md](docs/INTEGRATION.md).

The intended Diary behavior is:

1. Prefer Lean Pedometer when installed.
2. Fall back to the existing Health Connect reader otherwise.

## Android baseline

- minSdk 26
- targetSdk 35
- compileSdk 35
- Kotlin 2.2.10
- AGP 8.9.0
- Gradle 8.11.1
- JVM 17

## First-run limitation

The step-counter sensor exposes a cumulative value since device reboot, not a historic midnight snapshot. If the app is first installed during a day that began before the latest reboot, pre-install steps from that day cannot be reconstructed exactly.

## Build

The Gradle wrapper is checked in.

```bash
./gradlew :app:assembleDebug
```

GitHub Actions runs the same debug build for pull requests and pushes to `main`, and uploads the debug APK as a workflow artifact.
