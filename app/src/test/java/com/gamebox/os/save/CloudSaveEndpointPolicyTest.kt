package com.gamebox.os.save

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CloudSaveEndpointPolicyTest {
    @Test fun appendsGameScopedObjectToWebDavOrS3Prefix() {
        assertEquals(
            "https://cloud.example/remote.php/dav/files/player/GameBox/galaxy-patrol.gamebox-save",
            CloudSaveEndpointPolicy.objectUri(
                "https://cloud.example/remote.php/dav/files/player/GameBox/",
                "galaxy-patrol",
            ).toString(),
        )
        assertEquals(
            "https://bucket.s3.example/saves/galaxy-patrol.gamebox-save",
            CloudSaveEndpointPolicy.objectUri(
                "https://bucket.s3.example/saves",
                "galaxy-patrol",
            ).toString(),
        )
        assertEquals(
            "https://cloud.example/My%20Saves/galaxy-patrol.gamebox-save",
            CloudSaveEndpointPolicy.objectUri("https://cloud.example/My%20Saves", "galaxy-patrol").toString(),
        )
    }

    @Test fun rejectsEmbeddedCredentialsQueriesAndTraversal() {
        listOf(
            "http://cloud.example/saves",
            "https://user:secret@cloud.example/saves",
            "https://cloud.example/saves?token=secret",
            "https://cloud.example/saves/../other",
            "https://cloud.example/saves/%2e%2e/other",
        ).forEach { endpoint ->
            assertThrows(IllegalArgumentException::class.java) {
                CloudSaveEndpointPolicy.objectUri(endpoint, "galaxy-patrol")
            }
        }
    }

    @Test fun validatesS3RegionAndIgnoresItForWebDav() {
        assertEquals("eu-central-1", CloudSaveEndpointPolicy.requireRegion(CloudSaveProvider.S3, "EU-CENTRAL-1"))
        assertEquals("", CloudSaveEndpointPolicy.requireRegion(CloudSaveProvider.WEBDAV, "not a region"))
        assertThrows(IllegalArgumentException::class.java) {
            CloudSaveEndpointPolicy.requireRegion(CloudSaveProvider.S3, "invalid")
        }
    }
}
