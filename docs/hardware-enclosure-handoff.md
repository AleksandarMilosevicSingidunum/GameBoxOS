# GameBox Hardware and Enclosure Handoff

Status: design-input specification prepared for the Honor Magic5 Pro target. The phone has passed a preliminary wired external-display latency check with a non-final dock; exact UGREEN hub measurements, controller testing, concurrent I/O, thermal characterization, CAD release, fabrication, and full target-hardware acceptance remain pending.

This handoff implements the software-to-hardware boundary defined by the GameBox Development Blueprint. It intentionally does not invent enclosure dimensions. Final geometry must be based on the exact Honor Magic5 Pro, the purchased UGREEN 7-in-1 hub, charger, fan/cooling hardware, optional storage, and real cable layout after bring-up and the thermal baseline are complete. Do not infer the hub envelope or connector keep-out zones from marketing images because its ports are distributed across multiple sides.

## 1. Entry gate

Do not begin final CAD until all of the following evidence exists:

- Beta build installed on a clean Honor Magic5 Pro without developer-only setup.
- Stable wired HDMI/external-display output through the purchased UGREEN 7-in-1 powered hub. A preliminary low-latency wired desktop test with a different dock has passed; this is not yet evidence for the final hub.
- Controller, Ethernet, charging, and USB storage operating concurrently.
- GameBox verified at 1080p output first, then 4K output if the final display path supports it.
- External-display disconnect/reconnect and power/wake recovery recorded.
- Baseline 30-60 minute PS2 or GameCube thermal session on the stock chassis.
- Sustained streaming session and a 2-3 hour living-room soak completed.
- External SSD mount/unmount behavior tested if an SSD is part of the first build.
- Controller-only recovery path demonstrated.
- Exact cable bend radii, connector clearances, button access, and airflow needs measured.

If an item is incomplete, record it as an open design input; do not replace it with a guessed dimension.

## 2. Required physical architecture

| Element | Selection requirement | Evidence before sign-off |
| --- | --- | --- |
| Honor Magic5 Pro | Healthy USB-C video/data; external-display output; recoverable power-button access; removable without destructive disassembly; damaged/flickering internal screen must not be required for normal console operation | USB-C/HDMI stress test, cold-boot/no-touch recovery test, button-access photo, removal procedure |
| UGREEN 7-in-1 USB-C hub | HDMI, PD input, Gigabit Ethernet, USB ports, and stable simultaneous operation. Preserve the commercial shell and treat the hub as a replaceable internal module; final rear/front I/O may use short panel extensions only after signal testing. | 1080p/4K matrix, PD + Ethernet + controller concurrent soak, exact caliper envelope, connector keep-out map |
| Power supply | Reputable USB PD supply with measured headroom for phone, hub, and attached USB devices | Negotiated charging state and full-load stability log |
| Cooling | Quiet sustained airflow; no unverified thermal-pad pressure or electrical-insulation risk | Stock baseline and after-change temperature/frame-time comparison |
| External SSD | Optional; include only when capacity requires it and unplug/reconnect recovery is proven | SAF persistence, mount/unmount, copy interruption, and recovery results |
| Enclosure | Ventilated, serviceable, antenna-aware, short internal cable paths, accessible recovery controls | CAD review, airflow path review, service procedure |
| Controller path | Bluetooth and USB/dongle operation without blocked antennas or ports | Disconnect/reconnect and two-controller matrix |

## 3. Measurement worksheet

Complete this table with measured values and attach annotated photos or drawings before CAD release.

| Measurement | Value | Method/evidence |
| --- | --- | --- |
| Honor Magic5 Pro dimensions, camera-island protrusion, and retained-case state | TBD | Caliper measurement |
| USB-C plug protrusion and minimum bend radius | TBD | Installed cable measurement |
| UGREEN 7-in-1 hub envelope, attached-host-cable exit, and connector keep-out zones on every side | TBD | Caliper + cable fit + annotated photos |
| PD, HDMI, Ethernet, USB cable exit directions | TBD | Annotated layout photo |
| Power-button and recovery-tool access zone | TBD | Reachability test |
| Fan/heatsink envelope and mounting clearances | TBD | Selected cooling assembly |
| Intake and exhaust free area | TBD | Airflow design review |
| SSD envelope and removable connector access | TBD or N/A | Selected SSD/enclosure |
| Minimum antenna keep-out region | TBD | Connectivity comparison |
| Service fastener and tool clearances | TBD | Assembly trial |

## 4. Bring-up and validation matrix

Record pass/fail, date, build version, hardware revision, and evidence link for every row.

| Scenario | Expected result |
| --- | --- |
| Cold connect phone -> powered UGREEN hub -> TV | Stable HDMI and charging; GameBox becomes the normal HOME surface after unlock or is recoverable without using the damaged internal display |
| 1080p TV | Full-screen UI with no cropped overscan |
| 4K TV | Clean scaling; heavy emulators may render internally below 4K |
| Ethernet + HDMI + controller + storage | All devices operate simultaneously |
| Keyboard/mouse attached | PC and Settings remain usable; controller focus still works |
| Switch between Honor Desktop Mode / mirroring and GameBox | GameBox state is preserved and controller-focused reopening is obvious |
| HDMI disconnect/reconnect | Activity survives display change and restores window/focus state |
| Ethernet unplug/replug during Moonlight | Clear recovery state; controller remains usable |
| Controller disconnect/reconnect | Recovery works in GameBox, dialogs, emulator return, and streaming |
| External SSD unplug/reconnect | No crash or unsafe deletion; storage state becomes recoverable |
| 30-60 minute heavy-emulation baseline | Temperature, frequency, frame-time, and charging behavior recorded |
| 2-3 hour mixed-use soak | No critical navigation, launch, storage, network, or thermal failure |

## 5. Enclosure design rules

- Favor serviceability over minimum size for V1.
- Prefer a replaceable hub and removable phone over glued assemblies.
- Keep the Honor Magic5 Pro power button and a documented no-internal-display recovery path physically reachable.
- Provide strain relief without exceeding measured connector bend limits.
- Keep HDMI, PD, Ethernet, and USB connections inspectable during diagnostics.
- Avoid enclosing antennas in unvalidated conductive structures.
- Separate intake and exhaust paths; prevent recirculation and blocked vents.
- Establish stock thermal behavior before adding active cooling.
- Do not apply thermal pads to unknown components or create uncontrolled board pressure.
- Preserve access to storage media and all components likely to be replaced.
- Use PETG or another material appropriate to measured enclosure temperature; record the selected material and limits.
- Label external ports and recovery controls.
- Ensure assembly can be opened with ordinary tools without disconnecting or damaging the phone.

## 6. Bill of materials decision record

The released BOM must identify exact part numbers and revisions for:

- Honor Magic5 Pro donor and retained case state.
- UGREEN 7-in-1 USB-C hub, including exact model/SKU once physically confirmed.
- USB PD power supply and cable.
- HDMI cable.
- Ethernet cable.
- USB controller/dongle path.
- Fan, heatsink, grille, and controller if active cooling is required.
- External SSD and enclosure, or an explicit N/A decision.
- Internal extension cables, adapters, fasteners, inserts, feet, and filters.
- Enclosure material and print/manufacturing settings.

For every powered or signal-carrying component, record why it was chosen, its tested load, failure/recovery behavior, and replacement path.

## 7. CAD release checklist

- [ ] All worksheet dimensions are measured, not estimated.
- [ ] Connector keep-out zones and bend radii are modeled.
- [ ] Phone removal and UGREEN hub replacement are possible without disturbing the thermal assembly.
- [ ] Power-button/recovery access is demonstrated.
- [ ] Intake/exhaust paths and fan service are documented.
- [ ] Antenna placement has a validation plan.
- [ ] SSD is removable without dismantling unrelated components.
- [ ] Cable strain relief does not load the USB-C port.
- [ ] Sharp edges, exposed conductors, and pinch points are eliminated.
- [ ] Revision identifier is visible on the model and build record.

## 8. Prototype acceptance

Prototype V1 is accepted only when:

1. Every bring-up matrix row has terminal evidence.
2. The mixed-use soak passes without a critical data-loss, focus, launch, storage, or charging failure.
3. Thermal results are no worse than the documented safety/performance limits.
4. HDMI, Ethernet, controller, charging, and optional SSD remain stable concurrently.
5. The phone can be recovered, powered, and removed without destructive disassembly.
6. The BOM, wiring diagram, CAD revision, assembly steps, recovery steps, and known limitations are archived.

## 9. Handoff package

The final hardware handoff must contain:

- Completed measurement worksheet.
- Exact BOM with procurement links and substitutions policy.
- Wiring and port map.
- CAD source plus printable/manufacturing exports.
- Assembly and disassembly procedure.
- Cooling configuration and before/after thermal evidence.
- Bring-up matrix and soak-test record.
- Recovery-button and no-display recovery procedure.
- Photos of cable routing, airflow, and service access.
- Known limitations and V2 backlog.

Until those artifacts and physical results exist, repository status must remain “handoff prepared; preliminary wired-display test only; physical execution not validated,” not “hardware complete.”
