# Confirming ambiguous metadata matches

Blueprint requirement DATA-23 requires ambiguous provider matches to remain a user choice.

From an installed or imported game's Details screen, open **Edit metadata**, then choose
**Find TheGamesDB match**. GameBox performs an exact normalized-title search and displays
every bounded candidate with its provider ID, reported platform, and release year. Nothing
changes until the user activates **Use this match** on a specific candidate.

The confirmed candidate updates provider-owned title, year, genre, description, and artwork
in one Room operation. User corrections remain separate and continue to take precedence.
GameBox records the provider name, external ID, and confirmation time so later code can
distinguish confirmed metadata from an automatic name guess. Catalog refreshes preserve
that provenance.

Candidate responses are capped at 20 entries. Missing IDs, malformed entries, near-title
matches, insecure artwork URLs, and credential-bearing artwork URLs are excluded. Selecting
metadata never creates a download source or makes a game playable.

Automated parser, repository/entity, DAO, and full released-schema migration tests cover
ambiguity, provenance, atomic persistence, override precedence, and missing-game rejection.
A live authenticated TheGamesDB selection remains provider acceptance evidence rather than
being inferred from fixtures.
