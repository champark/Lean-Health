# Lean Diary integration

Lean Pedometer exposes a read-only Android `ContentProvider` so Lean Diary can read step totals without Health Connect.

## Contract

- Authority: `com.cpkr.leanpedometer.steps`
- URI for today: `content://com.cpkr.leanpedometer.steps/steps/today`
- URI for a date: `content://com.cpkr.leanpedometer.steps/steps/YYYY-MM-DD`

Returned columns:

| Column | Type | Meaning |
| --- | --- | --- |
| `date` | TEXT | ISO-8601 local date |
| `steps` | INTEGER | Recorded step total for that date |
| `updated_at_epoch_ms` | INTEGER | Last update time for the date |
| `source` | TEXT | `lean_pedometer` |

The provider is read-only. Insert, update and delete are rejected.

## Caller policy

The provider accepts calls from its own process and from the Lean Diary package:

`com.cpkr.lwdiary`

This intentionally avoids a shared signing-key requirement, because Play App Signing can give separate apps different signing keys. Step totals are treated as low-sensitivity local data, and the package allow-list prevents ordinary unrelated apps from querying the provider while Lean Diary is installed.

## Lean Diary fallback

Lean Diary should query Lean Pedometer first. If the provider is not installed, unavailable or returns no value, it can fall back to its existing Health Connect reader.

That keeps the existing Health Connect path intact while making Lean Pedometer the preferred local source.
