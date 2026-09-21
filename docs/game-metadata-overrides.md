# Per-game metadata corrections

Game Details exposes **Edit metadata** for library titles. A user can override the
display title, release year, genre, HTTPS artwork URL, and description. Blank
fields continue using provider metadata.

Corrections are stored separately from provider values in Room. Catalog refresh
updates the provider copy while retaining and reapplying user corrections. Choosing
**Use provider metadata** atomically clears every correction and immediately reveals
the newest provider values.

Artwork corrections accept credential-free HTTPS URLs only. Values are length
bounded, control characters are rejected, and release years are limited to
1900–2100. These controls implement the correction/precedence portion of DATA-14
and the per-game artwork correction portion of DATA-27. They do not claim that an
ambiguous TheGamesDB match has been confirmed; provider candidate selection remains
a separate DATA-23 flow.
