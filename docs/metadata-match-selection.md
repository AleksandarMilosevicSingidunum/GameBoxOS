# Confirming metadata matches

Blueprint requirement DATA-23 requires ambiguous provider matches to remain a user choice.

From an installed or imported game's Details screen, open **Edit metadata**, then choose
**Find metadata match**. GameBox asks Wikidata and Libretro for exact-title candidates and
displays their provider, reported platform, and release year. Nothing changes until the user
activates **Use this match** on a specific candidate.

The confirmed candidate updates provider-owned title, year, genre, description, and artwork
in one Room operation. User corrections remain separate and continue to take precedence.
GameBox records the provider name, external ID, and confirmation time so later code can
distinguish confirmed metadata from an automatic name guess. Catalog refreshes preserve
that provenance.

Provider clients bound their network responses and reject malformed or unsafe artwork URLs.
Selecting metadata never creates a download source or makes a game playable.

Automated provider, repository/entity, DAO, and released-schema migration tests cover
candidate matching, provenance, atomic persistence, override precedence, and missing-game
rejection. Live metadata-source availability remains a separate provider acceptance check.
