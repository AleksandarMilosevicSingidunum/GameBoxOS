# Controller navigation

GameBox accepts D-pad keys, shoulder-tab keys and the contextual face-button
actions shown in the footer. The Android host also maps the connected controller's
left stick or hat axes to D-pad focus movement.

Analog navigation uses a 0.65 engagement threshold and a lower 0.35 release
threshold to suppress axis noise. A new direction moves immediately. Holding a
direction waits 320 ms before repeating, then repeats at most every 120 ms.
Changing direction moves immediately, and pausing the activity resets held state.

This is production input wiring with deterministic timing tests. Automated tests
do not replace acceptance on each physical controller model; controller-specific
axis mappings and DeX focus behavior remain physical validation items.
