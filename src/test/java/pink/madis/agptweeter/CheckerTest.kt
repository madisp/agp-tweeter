package pink.madis.agptweeter

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException

class EmptyFetcher: Fetcher {
    override fun latestVersion(): Version? = null
}

class FixedFetcher(private val version: String): Fetcher {
    override fun latestVersion(): Version? = Version(version)
}

class FailingFetcher: Fetcher {
    override fun latestVersion(): Version? = throw IOException("Expected failure")
}

class FixedSource(
    override val fetcher: Fetcher = FixedFetcher("1.0.0"),
    override val key: String = "key",
    override val prettyName: String = "Artifact"): ArtifactSource

val noTweet: (String)->Unit = { throw IllegalStateException("Unexpected tweet: $it") }

class CheckerTest {
    @Test
    fun testEmptyLocalVersion() {
        assertThat(isNewRemoteVersion(Version("0.0.1"), EmptyFetcher())).isTrue()
    }

    @Test
    fun testLaterLocalVersion() {
        assertThat(isNewRemoteVersion(Version("1.5.3"), FixedFetcher("2.0.0"))).isFalse()
    }

    @Test
    fun testOlderLocalVersion() {
        assertThat(isNewRemoteVersion(Version("2.0.1"), FixedFetcher("2.0.0"))).isTrue()
    }

    @Test
    fun testSameLocalVersion() {
        assertThat(isNewRemoteVersion(Version("2.0.0"), FixedFetcher("2.0.0"))).isFalse()
    }

    @Test
    fun failingRemoteResultsInException() {
        try {
            checkAndTweet(FixedSource(fetcher = FailingFetcher()), MemStore(), MemStore(), noTweet)
        }
        catch (e: IOException) {
            assertThat(e).hasMessageThat().isEqualTo("Expected failure")
            return
        }
        throw IllegalStateException("Expected checkAndTweet to fail")
    }

    @Test
    fun emptyRemoteResultsInException() {
        try {
            checkAndTweet(FixedSource(fetcher = EmptyFetcher()), MemStore(), MemStore(), noTweet)
        }
        catch (e: IOException) {
            assertThat(e).hasMessageThat().isEqualTo("Did not get a proper remote version")
            return
        }
        throw IllegalStateException("Expected checkAndTweet to fail")
    }

    @Test
    fun emptyLocalStateResultsInTweet() {
        val cache = MemStore()
        val db = MemStore()
        var tweet: String? = null

        checkAndTweet(FixedSource(), cache, db, { tweet = it })

        assertThat(tweet).isEqualTo("Artifact 1.0.0 is out!")
        // make sure that both cache and db are updated too
        assertThat(cache.read("key")).isEqualTo("1.0.0".toByteArray())
        assertThat(db.read("key")).isEqualTo("1.0.0".toByteArray())
    }

    @Test
    fun emptyCacheStateResultInNoTweet() {
        val cache = MemStore()
        val db = MemStore().apply {
            write("key", "1.0.0".toByteArray())
        }

        checkAndTweet(FixedSource(), cache, db, noTweet)

        // make sure that cache gets updated
        assertThat(cache.read("key")).isEqualTo("1.0.0".toByteArray())
    }

    @Test
    fun cacheAndDbAreUpToDate() {
        val cache = MemStore().apply {
            write("key", "1.0.0".toByteArray())
        }
        val db = object : Store {
            override fun read(key: String): ByteArray? = throw IllegalStateException("Not supposed to hit db")
            override fun write(key: String, bytes: ByteArray) = throw IllegalStateException("Not supposed to hit db")
        }

        checkAndTweet(FixedSource(), cache, db, noTweet)
    }

    @Test
    fun cacheAndDbAreOutOfDate() {
        val cache = MemStore().apply {
            write("key", "0.9.0".toByteArray())
        }
        val db = MemStore().apply {
            write("key", "0.9.0".toByteArray())
        }
        var tweet: String? = null

        checkAndTweet(FixedSource(), cache, db, { tweet = it })

        assertThat(tweet).isEqualTo("Artifact 1.0.0 is out!")
        // make sure that both cache and db are updated too
        assertThat(cache.read("key")).isEqualTo("1.0.0".toByteArray())
        assertThat(db.read("key")).isEqualTo("1.0.0".toByteArray())
    }
}