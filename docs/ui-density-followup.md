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
are not asserted by these layout fixtures. Execution is pending.
Verify normal and enlarged font scales,
and compare desktop/phone renders before merging. Artwork, accurate brand marks,
hero action placement, compact Settings panels and motion validation remain open.
