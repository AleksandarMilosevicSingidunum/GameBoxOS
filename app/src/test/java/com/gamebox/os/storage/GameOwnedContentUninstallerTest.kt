package com.gamebox.os.storage

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GameOwnedContentUninstallerTest {
    @get:Rule val temp = TemporaryFolder()
    private fun file(path: String) = temp.root.resolve(path).apply { parentFile.mkdirs(); writeText("content") }

    @Test fun multiFileImportRemovalRetainsSavesMetadataAndOtherGames() {
        val paths = listOf("imports/disc/game.cue", "imports/disc/track.bin")
        paths.forEach(::file)
        val save = file("saves/disc/progress.sav")
        val backup = file("save-backups/disc/progress.sav")
        val other = file("imports/other/game.cue")
        val metadata = file("imports/disc/not-recorded.json")
        val remover = GameOwnedContentUninstaller(temp.root)
        val manifest = ContentRemovalManifest("disc", paths)
        assertEquals(ContentRemovalPreview(14, 2), remover.preview(manifest))
        assertEquals(2, remover.uninstall(manifest))
        assertEquals(0, remover.uninstall(manifest))
        listOf(save, backup, other, metadata).forEach { assertTrue(it.isFile) }
    }

    @Test fun invalidLaterEntryPreventsPartialDeletion() {
        val good = file("imports/disc/game.chd")
        val protected = file("imports/other/game.chd")
        val remover = GameOwnedContentUninstaller(temp.root)
        for (bad in listOf("imports/other/game.chd", "imports/disc/../other/game.chd",
            "saves/disc/save.dat", "imports/disc", "imports/disc/", "/imports/disc/game.chd")) {
            assertThrows(IllegalArgumentException::class.java) {
                remover.uninstall(ContentRemovalManifest("disc", listOf("imports/disc/game.chd", bad)))
            }
            assertTrue(good.isFile)
            assertTrue(protected.isFile)
        }
    }

    @Test fun downloadedAndBundledContentUseTheirExactOwnedPaths() {
        file("installed/remote/portable/content.iso")
        file("installed/retro/galaxy-patrol/content/galaxy-patrol.nes")
        val remover = GameOwnedContentUninstaller(temp.root)
        assertEquals(1, remover.uninstall(ContentRemovalManifest("portable", listOf("installed/remote/portable/content.iso"))))
        assertEquals(1, remover.uninstall(ContentRemovalManifest("galaxy-patrol", listOf("installed/retro/galaxy-patrol/content/galaxy-patrol.nes"))))
    }
}
