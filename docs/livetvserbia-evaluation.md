# Live TV Serbia source evaluation

Evaluated 7 November 2026 for GameBox TV Mode on HONOR HDMI. This is a compatibility and playback probe, not an integration or confirmation of channel distribution rights.

## Free-TV/IPTV comparison — 8 November 2026

The public [Free-TV/IPTV repository](https://github.com/Free-TV/IPTV) has a quality-over-quantity policy and says it includes channels officially provided for free. Its [Serbia playlist](https://raw.githubusercontent.com/Free-TV/IPTV/master/playlists/playlist_serbia.m3u8) was fetched on the connected HONOR over HDMI (1920×1080/60 Hz) and the referenced HTTPS HLS master manifests were checked from Android. The app’s saved `iptv-org · RS` playlist was not changed.

| Entry | HONOR manifest result | Assessment |
| --- | --- | --- |
| RTS 1, RTS 2, RTS 3, RTS Svet | HTTP 200; variants list 1280×720 and 480×270 | Reachable manifest, max advertised variant below requested 1080p; actual playback not tested |
| Kurir TV | HTTP 200; URL and media playlist are 720p | Below requested 1080p; actual playback not tested |
| Red TV | Host DNS lookup failed | Unavailable in this probe |
| Informer TV | HTTP 403 | Unavailable to this client in this probe |
| RTV 1, RTV 2 | RTMP URLs | Unsupported by GameBox’s Media3 player |

No video segments were downloaded and no decoded frames were observed. These results do not prove future availability or distribution rights. Free-TV/IPTV’s contribution rules are more explicit about free-to-air selection, but that policy is not a license for each linked internet relay. iptv-org has the stronger structured channel/EPG and validation workflow; its playlist labels still do not establish rights or playback quality. **Decision:** Free-TV/IPTV is not a stronger default for GameBox’s Serbia/1080p requirements; retain iptv-org RS as primary and preserve official RTS Planeta as the broadcaster fallback already used by GameBox. Do not label any of these entries HD based on playlist metadata.

## Source format

- The [repository page](https://livetvserbiarepo.com/) publishes Kodi 18 and Kodi 21 repository ZIPs. The Kodi 21 descriptor points to the [add-on catalog](https://livetvkodiserbia.com/live/addon/21/addons.xml), which listed `plugin.video.livetvSerbia` 2.0.3 when inspected.
- The add-on uses Kodi Python APIs, `inputstream.adaptive`, a local proxy and resolver modules. Some upstream menu entries contain dynamic Python expressions; the inspected add-on includes code paths that evaluate them. Do not execute remote menu content inside GameBox.
- The add-on's versioned menu exposes nested Serbia and Balkan menus. Static inspection of the Serbia menu found direct HLS/DASH links, Kodi/plugin links, header-suffixed URLs, and dynamic resolver expressions; titles include duplicates and programme text. The sample did not supply a verified XMLTV guide or machine-readable resolution for all entries. Source authorization, DRM, geo restrictions, availability, and guide quality must be assessed per channel.
- GameBox currently reads M3U playlists, and its player does not consume Kodi Python plug-ins or their `|` header syntax directly. An integration would need a narrowly scoped, non-executing data adapter and per-stream header handling, or an explicit Kodi handoff. Do not copy or run the add-on's resolver code without a separate security, licensing, and compatibility review.

## Real HDMI playback probe

Used temporary M3U entries containing direct HTTPS HLS links from the source; no Kodi add-on was installed or executed. Playback was observed in GameBox on the HONOR external 1920×1080 display:

| Candidate | Result | Decoded dimensions at observation |
| --- | --- | --- |
| Kurir TV | Visible playback at 15 and 25 seconds | 1280×720 |
| KTV Zrenjanin | Visible playback at 15 seconds | 1280×720 |
| Fashion TV | Visible playback at 15 seconds | 1920×1080 |

Playback dimensions reflect the selected rendition at that moment; they do not establish each source's maximum available resolution or future availability. Only Fashion TV met the requested 1080p threshold in this small sample. Guide data was not supplied in the test playlist. The [Kurir live page](https://www.kurir.rs/tv) publicly advertises its live broadcast; that alone does not establish distribution permission for every link in the repository.

After testing, the previous `iptv-org · RS` source and TDI Radio TV last-watched selection were restored. Only the temporary `/sdcard/Download/GameBoxTest/livetvserbia-test.txt` was deleted; the existing `blockblast.3dsx` was preserved.

## Remaining work before offering this source

- Validate a representative set of authorized direct 1080p Serbian and international streams on HONOR HDMI, including retries, availability over time, and guide mapping.
- Determine which source entries can be imported without executing upstream expressions, bypassing access controls, or misrepresenting playback rights; handle unsupported entries explicitly.
- Design and test an optional provider with bounded input, safe XML parsing, headers restricted to what each supported source requires, and a reversible choice that preserves the existing playlist and favorites.
- Verify 1080p filtering from reliable stream metadata or actual decoded video rather than channel names; provide a truthful unknown-quality state.
- Validate controller navigation, logos, now/next guide data, and playback error handling on the TV. An optional Kodi handoff requires Kodi and the add-on to be separately installed and trusted; it does not establish in-GameBox playback.
