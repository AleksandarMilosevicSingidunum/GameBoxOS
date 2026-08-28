package com.gamebox.os

import com.gamebox.os.download.InstalledContentStatus
import com.gamebox.os.download.InstalledContentValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InstalledContentValidatorTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val checksum = "94ee059335e587e501cc4bf90613e0814f00a7b08bc7c648fd865a2af6a22cc2"

    @Test fun distinguishesVerifiedMissingAndAlteredContent() {
        val root = temporaryFolder.root
        val validator = InstalledContentValidator(root)
        val relative = "retro/test/content/test.txt"

        assertEquals(InstalledContentStatus.MISSING, validator.validate(relative, checksum))
        val content = root.resolve(relative)
        content.parentFile.mkdirs()
        content.writeText("TEST")
        assertEquals(InstalledContentStatus.VERIFIED, validator.validate(relative, checksum))
        content.writeText("ALTERED")
        assertEquals(InstalledContentStatus.ALTERED, validator.validate(relative, checksum))
    }

    @Test fun traversalIsRejectedBeforeReading() {
        assertThrows(IllegalArgumentException::class.java) {
            InstalledContentValidator(temporaryFolder.root)
                .validate("../outside.txt", checksum)
        }
    }
}
