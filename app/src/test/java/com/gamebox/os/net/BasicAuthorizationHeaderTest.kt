package com.gamebox.os.net

import com.gamebox.os.catalog.CatalogCredentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BasicAuthorizationHeaderTest {
    @Test
    fun encodesUsernameAndPasswordAsUtf8() {
        assertEquals(
            "Basic dXNlcjpww6Rzcw==",
            basicAuthorizationHeader(CatalogCredentials(username = "user", password = "päss")),
        )
    }

    @Test
    fun omitsHeaderWhenCredentialsAreIncompleteOrBlank() {
        assertNull(basicAuthorizationHeader(CatalogCredentials(username = "user")))
        assertNull(basicAuthorizationHeader(CatalogCredentials(username = " ", password = "secret")))
    }
}
