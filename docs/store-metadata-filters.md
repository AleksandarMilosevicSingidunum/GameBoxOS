# Store metadata filters

Blueprint requirement UI-17 is now implemented for the wide Store rail.

- Genre filters use authorized-catalog metadata.
- Region filters combine authorized-catalog and cached TheGamesDB metadata.
- Language filters use authorized-catalog metadata; TheGamesDB records without a language do not pretend to match.
- Selecting a filter excludes records whose value is unknown.
- Each controller-reachable filter button cycles through available values and returns to All.
- Clear filters resets console, favorites, installed, sort, query, genre, region and language together.

The compact Store retains its existing chip-based authorized-catalog genre filtering.
CI production screenshots assert that all three wide filter controls render. A screenshot does
not validate TheGamesDB data availability, controller hardware, or provider correctness.
