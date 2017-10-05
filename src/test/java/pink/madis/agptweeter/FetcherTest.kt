package pink.madis.agptweeter

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import kotlin.text.Charsets.UTF_8

class FetcherTest {
    @Test
    @Ignore("Actually hits network, manual running only")
    fun latestGoogleVersion() {
        val coords = MavenCoords("com.android.tools.build", "gradle")
        val version = GoogleFetcher().latestVersion(coords)
        println("Latest AGP version is $version")
        assertTrue(version!! > Version("0.0.1", coords))
    }

    @Test
    fun emptyStore() {
        assertNull(StoreFetcher(MemStore()).latestVersion(MavenCoords("com.android.tools.build", "gradle")))
    }

    @Test
    fun updateStore() {
        // start off with one item
        val store = MemStore()
        val fetcher = StoreFetcher(store)

        val coords = MavenCoords("com.android.tools.build", "gradle")
        store.write(coords.toKey(), "0.0.1".toByteArray(UTF_8))

        assertTrue(fetcher.latestVersion(coords)!!.orig == "0.0.1")

        store.write(coords.toKey(), "0.0.10".toByteArray(UTF_8))
        assertTrue(fetcher.latestVersion(coords)!!.orig == "0.0.10")

        // other coordinates should still be empty
        assertNull(fetcher.latestVersion(MavenCoords("foo", "bar")))
    }
}