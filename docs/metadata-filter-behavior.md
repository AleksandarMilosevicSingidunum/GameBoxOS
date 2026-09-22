# Metadata filter behavior

Blueprint UI-17 follow-up.

- Library exposes genre, region and language filters in phone and wide layouts.
- Library selections are stored separately from Store selections in saved UI state.
- Wide Store shares genre, region, language and favorites selections with its parent;
  opening Details or changing layouts no longer resets a second private filter state.
- Wide filter options use the complete authorized catalog, not an already filtered
  subset. Discovery regions are available in both Store layouts.
- Compact discovery respects the same favorites and metadata predicates as wide Store.
- Missing metadata does not satisfy an explicitly selected filter. The current
  discovery model supplies region but not genre or language; those filters can
  therefore exclude discovery entries without claiming provider metadata exists.
- Wide Store also restores its installed-only and rating-sort controls.
- Active selections remain clearable when a refresh removes all available values.
  Wide Library and Store provide Clear filters actions.

Validation: JVM regressions cover case-insensitive matching, missing metadata,
cycling/removed options and destination-isolated saved-state restoration. Android
instrumentation exercises the production filter button through disappearing and
returning catalog metadata. CI also builds Android/Windows and captures phone,
wide and large-text layouts. These checks do not establish physical controller
acceptance or pixel equality to the Blueprint.
