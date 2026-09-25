# GameBox as the Android Home app

GameBox can be selected as the Android **Home app** on a dedicated console phone.

The manifest keeps the ordinary launcher and TV launcher entries and also advertises
`MainActivity` as an Android `HOME` / `DEFAULT` candidate. The activity uses
`singleTask` so repeated Home resolution reuses the GameBox task instead of building
duplicate launcher stacks. GameBox does not silently replace the user's launcher.

If GameBox is not currently the resolved Android Home app, the GameBox Home screen shows
an actionable **Finish console mode** banner. Choose **Choose Home** to open Android's
Home-app selector. The same action remains available under **Settings → System → Choose
default Home app**.

After Android finishes booting and the required device unlock is completed, pressing Home
and normal launcher resolution return to GameBox. When the Android selector returns to
GameBox, the banner immediately rechecks the resolved Home package and disappears once
GameBox is selected.

## Important limits

- Android still owns the boot animation, lock screen, credential/PIN prompt, permission
  dialogs, and other system UI.
- A reboot can require the first credential unlock before GameBox can become the normal
  living-room surface.
- External-display behavior is device firmware behavior and remains a physical acceptance
  gate on the Honor Magic5 Pro.
- The damaged internal display must not be assumed unnecessary until cold-boot, unlock,
  HDMI reconnect and recovery have been tested on the target device.
- Selecting GameBox as Home is reversible through Android's default-app settings.

Android instrumentation verifies that `MainActivity` remains discoverable as a HOME
candidate and retains the expected single-task launch mode. Unit coverage verifies the
package-match policy used by the Home status banner. These tests prove Android
registration/configuration only; they do not prove Honor firmware boot, unlock or
external-display behavior.
