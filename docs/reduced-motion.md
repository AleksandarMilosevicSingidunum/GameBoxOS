# Reduced motion

GameBox OS includes a persisted **Settings → Interface → Reduce motion** option.

When enabled, the production shell removes screen-entry slide/fade transitions and focus/press scale animation from shared actions, navigation tabs, game cards, discovery cards, media/PC tiles, and collapsible game settings. Focus borders, status colors, selection state, controller navigation, and all actions remain available.

The option is independent of Android's system animation controls and is applied immediately through the app-level UI composition. Automated build, instrumentation, and screenshot workflows verify that both phone and DeX layouts still render. Physical viewing-comfort acceptance remains a device/user validation item.
