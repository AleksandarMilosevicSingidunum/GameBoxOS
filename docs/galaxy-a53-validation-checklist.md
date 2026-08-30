# Galaxy A53 / DeX validation checklist

Run this checklist on a Galaxy A53 with a supported game controller and, separately, in Samsung DeX.

## Controller

- Pair the controller over Bluetooth and confirm Android reports a gamepad or joystick input device.
- Launch GameBox and confirm Home shows the controller name under device status.
- Navigate with D-pad/left stick, activate with A/Enter, return with B/Escape, and switch tabs with LB/RB.
- Disconnect and reconnect the controller while GameBox is open; confirm the status returns to connected without restarting the app.
- Launch Galaxy Patrol and verify the approved NES/FCEUmm handoff opens RetroArch.

## DeX and display

- Start GameBox in phone portrait, phone landscape, and DeX windowed modes.
- Confirm cards reflow without clipping, focus remains visible, and all primary actions remain reachable.
- Resize the DeX window and verify the UI remains usable at narrow and wide aspect ratios.
- Connect and disconnect HDMI/USB-C display output; confirm GameBox returns to the foreground and navigation still works.

## Storage and recovery

- Select an external SAF library, install the authorized fixture, and verify it.
- Kill and relaunch GameBox; confirm catalog, download state, and save record are restored.
- Uninstall content while retaining saves, reinstall it, and verify the save record remains available.
- Remove the external volume and confirm the UI reports unavailable content without deleting local metadata.

Record device model, One UI version, Android version, controller model, DeX/HDMI adapter, and pass/fail evidence for each item in the release validation report.
