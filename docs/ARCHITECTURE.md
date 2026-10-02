# Architecture

## Goal

Keep the pedometer independent from Lean Diary while giving Lean Diary a stable, tiny read API.

## Flow

1. Android's `TYPE_STEP_COUNTER` reports the cumulative device counter.
2. `StepTrackingService` keeps a foreground health service registered with the sensor.
3. `StepStore` converts changes in the cumulative counter into local per-day totals.
4. `StepProvider` exposes date totals to Lean Diary.
5. Lean Diary reads the provider first and can retain Health Connect as a fallback.

## Persistence

The MVP uses `SharedPreferences` because the stored dataset is tiny: one count and one update timestamp per day, retained for 400 days.

No migration layer is included. The project is still pre-release and the stored shape can be replaced if the design changes.

## Accuracy boundaries

`TYPE_STEP_COUNTER` is cumulative since the most recent device reboot. It does not reveal the exact midnight value retroactively.

Therefore:

- If the service is running across midnight, normal daily counting is accurate.
- If the device rebooted today, the first reading can recover steps since that reboot.
- If Lean Pedometer is first installed after midnight on a device that was already running before midnight, steps taken before the first sample cannot be reconstructed safely. Tracking begins from the first sample.

## Background execution

Continuous sensor registration is attached to a foreground service of type `health`. Android requires the health foreground-service declaration and an eligible runtime permission such as `ACTIVITY_RECOGNITION`.

The app restarts tracking after boot or package replacement when tracking was previously enabled.
