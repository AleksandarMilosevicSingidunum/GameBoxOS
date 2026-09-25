# GameBox OS hardware validation checklist

Run these checks on the Honor Magic5 Pro with the purchased UGREEN 7-in-1 hub before production release.

Preliminary evidence only: wired desktop output through a non-final dock was observed with little to no noticeable latency. No controller was available during that test, so it does not close the controller, final-hub, concurrent-I/O, reconnect, or soak gates.

- [ ] GameBox selected as the Android HOME app and recoverable after cold boot/unlock without relying on the damaged internal display
- [ ] Controller navigation, focus restoration, Back/B handling, and reconnect after sleep
- [ ] Honor Magic5 Pro at 16:9 over HDMI with controller input in the chosen mirroring/Desktop Mode path
- [ ] Purchased UGREEN 7-in-1 hub: HDMI, PD charging, Gigabit Ethernet, and USB controller coexistence
- [ ] HDMI disconnect/reconnect restores the GameBox surface and controller focus
- [ ] Ethernet unplug/replug recovers without breaking controller navigation
- [ ] External SSD mount, read/write, disconnect, and safe reconnect when external storage is part of the build
- [ ] Thermal soak under sustained gameplay and download/install workloads
- [ ] Charging behavior remains stable during heavy emulation and hub load
- [ ] Resume after emulator return and play-session recording
- [ ] Low-storage warning and recovery behavior
- [ ] Airplane-mode/offline catalog and library operation
- [ ] Recovery after app process death, external-display mode changes, and device restart
- [ ] Exact hub/phone/cable measurements recorded before final CAD release
