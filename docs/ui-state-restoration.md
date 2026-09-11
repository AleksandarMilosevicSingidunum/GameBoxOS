# UI state restoration

GameBox keeps navigation state in a saveable activity-level model so tab changes,
details navigation, and Android saved-instance restoration do not reset the user's
working context.

The restored state currently includes:

- selected destination and selected authorized game;
- last focused game per destination;
- Library query, platform, genre and Favorites mode;
- Store query, console, platform, genre, region, language and Favorites mode;
- selected Store discovery result, when that result is still present in the cache;
- primary vertical scroll positions for Home, Library, Store, Downloads, Media,
  PC Hub, Settings, authorized-game details and discovery details.

Invalid focused IDs are discarded against the current game collection. Empty
filter values are removed from the saved payload, and the decoder accepts payloads
from the earlier focus-only format. Transient network synchronization progress and
error messages are intentionally not restored because they must reflect the current
repository operation after recreation.
