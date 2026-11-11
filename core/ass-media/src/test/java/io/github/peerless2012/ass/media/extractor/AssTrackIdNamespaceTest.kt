package io.github.peerless2012.ass.media.extractor

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssTrackIdNamespaceTest {
    @Test
    fun namespacesTheSameTrackNumberForEachExtractor() {
        val firstId = AssTrackIdNamespace().trackId("3")
        val secondId = AssTrackIdNamespace().trackId("3")

        assertNotEquals(firstId, secondId)
        assertTrue(firstId.endsWith(":3"))
        assertTrue(secondId.endsWith(":3"))
    }
}
