# GameBox Windows Companion

This optional native Windows companion complements the Android/DeX GameBox OS product.

It provides a GameBox-style dark library UI, local EXE/shortcut/BAT/CMD registration, persistent JSON storage under `%LOCALAPPDATA%\GameBoxOS`, search, favorites, safe missing-file handling, and explicit library-only removal. Enter plays the selected game; Ctrl+F searches; Ctrl+O adds a game.

The companion never downloads game content, deletes installed files, reads Android save data, or stores streaming credentials. Moonlight and Winlator remain user-installed runtime integrations.

Build and test:

```powershell
dotnet run --project windows/GameBox.Windows.Core.Tests/GameBox.Windows.Core.Tests.csproj -c Release
dotnet publish windows/GameBox.Windows/GameBox.Windows.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true
```
