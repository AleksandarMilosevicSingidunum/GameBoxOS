# UI density follow-up (10 September 2026)

The visual review compared Blueprint pages 8–16 with PR281's phone/DeX captures
from workflow 34404188849, head 02b6328. This is not a new gameplay or motion test.

Local draft: desktop Home quick-launch tiles use 100 dp height and 8 dp icon/text
spacing; phone tiles retain 140 dp and 18 dp. Status labels now declare 12 sp and
14 sp line heights rather than inheriting larger Material body line heights.
This addresses part of the excess vertical space observed in the desktop Home
capture. No new render or Android build has verified the edit yet.

Before publishing, reapply only this delta to current main GameBoxApp.kt: the
local checkout has older unrelated sections and must not replace that file whole.
Populated Home/Library captures have been added to the production-activity
screenshot test. They temporarily set six catalog entries' installed flags and
restore them in finally; file installation, artwork availability and gameplay
are not asserted by these layout fixtures. The latest connected-device
instrumentation runs execute this test; fresh visual captures for this exact density
revision remain pending.
Verify normal and enlarged font scales,
and compare desktop/phone renders before merging. Artwork, accurate brand marks,
hero action placement, compact Settings panels and motion validation remain open.


## 10 September screenshot review and accessibility follow-up

PR #282 head 841b325 produced successful phone/DeX captures in workflow 34448327921. Visual inspection confirmed the populated Home bottom row is now visible; it did not establish complete Blueprint parity. Artwork, branding, navigation shape, Details action placement, Settings density and phone Store adaptation remain open. Populated Library flags are layout fixtures, not gameplay evidence.

The follow-up replaces the quick tiles' fixed height with a minimum height and lets their content determine extra vertical space. HomeQuickLaunchLayoutTest exercises 200% text, checks title/subtitle containment and growth, and clicks all four actions. The production screenshot flow now passes on the API-35 emulator in both the wide header-utility and compact controller-Quick-Actions routes; it captures Home, Details, Library, Store, Media, PC, Settings, the Downloads utility destination, and populated Home/Library states. The wide emulator render was also visually checked at 2400 × 1080 / 160 dpi, and the emulator was restored to its original phone profile afterward. These captures replace the earlier revision's evidence, but they do not claim physical DeX, external-controller, hover, or motion acceptance. Final UI acceptance still requires those separate interaction/hardware checks plus explicit loading/error/offline/downloading/recovery-state review.

The compact Store revision now uses a Store heading and exposes a directly reachable
horizontal console rail before the generic search/metadata filters. The focused
production activity test passes on API-35, including accessible labels for All and PSP
console filters. This closes the phone console-selection discoverability gap; artwork,
branding, and physical TV/controller acceptance remain separate visual or hardware
follow-ups. Compact Details now gives the primary import/install action a full-width
phone row; the API-35 production screenshot flow passed after this adjustment.
