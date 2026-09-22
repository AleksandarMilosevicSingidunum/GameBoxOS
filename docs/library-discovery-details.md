# Library discovery details

Follow-up for the audit's unified metadata/details gap (EXT-05, DATA-14).

Library Details observes the exact game ID in the persisted discovery cache rather
than scanning the first catalog page. Imported titles can therefore keep their
background, rating and screenshot gallery when opened from Library. Both Details
paths share the same enlarge/close gallery. Library also displays its description
and region. Both single-file and disc-set discovery imports carry region forward.

Library title, description, install state and favorites remain authoritative.
A user artwork correction wins over cached hero art. An explicit provider rematch
suppresses the previous discovery identity's gallery until matching media is available.
Missing cache data falls back to Library metadata without inventing media.
The cache stores image URLs; offline image rendering still depends on the existing
artwork cache. This is not a guarantee that every provider supplies screenshots.

Reimport replaces content references and verified size/state, not metadata identity.
It preserves corrections, original provider values (including Reset metadata),
confirmed provider match, platform, region, language, history, favorites, saves and
emulator settings. A different game ID is rejected before merging.

Regression coverage checks metadata persistence/reset round trips, wrong identities,
gallery precedence and a Room close/reopen lookup beyond the first 250 catalog rows.
Live provider acceptance and populated visual equivalence remain separate gates.
