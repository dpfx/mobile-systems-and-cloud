# Spec amendments

The specification above is the prompt the app was generated from, so it stays unchanged. Changes found necessary later are amendments; where they differ, the amendment applies.

| # | Section | Change | Reason | Finding |
|---|---|---|---|---|
| A1 | 7 Architecture | `kotlinx-coroutines` added to the allowed libraries | The section requires `callbackFlow` and `StateFlow`, which come from this library, but its library list left it out | F2 |
| A2 | 3 Warning rule | Tapping the notification opens the app | Android: "Every notification must respond to a tap" ([Build a notification][tap]) | F4 |
| A3 | 1 Sensors | A GPS step counts only if it also implies at most 10 m/s; a faster step adds nothing and its fix becomes the new reference | Without a plausibility limit, a position jump was counted as 9,359,974 m | F1 |

Full wording (original and amended): [`docs/spec-amendments.md`][full].

[tap]: https://developer.android.com/develop/ui/views/notifications/build-notification#tap
[full]: https://github.com/dpfx/mobile-systems-and-cloud/blob/main/aufgabe-1/docs/spec-amendments.md
