package pink.madis.agptweeter

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException

class EmptyFetcher: Fetcher {
  override fun versions(): Set<String>? = null
}

class FixedFetcher(private vararg val versions: String): Fetcher {
  override fun versions(): Set<String>? = setOf(*versions)
}

class FailingFetcher: Fetcher {
  override fun versions(): Set<String>? = throw IOException("Expected failure")
}

class FixedSource(
    override val fetcher: Fetcher = FixedFetcher("1.0.0"),
    override val key: String = "key",
    override val prettyName: String = "Artifact"): ArtifactSource

val noTweet: (String)->Unit = { throw IllegalStateException("Unexpected tweet: $it") }

class CheckerTest {
  private val tweets = mutableListOf<String>()
  private val tweeter: (String)->Unit = { tweets.add(it) }

  @Test
  fun testEmptyLocalVersion() {
    assertThat(isNewRemoteVersion(setOf("0.0.1"), setOf())).isTrue()
  }

  @Test
  fun testLaterLocalVersion() {
    assertThat(isNewRemoteVersion(setOf("1.5.3"), setOf("2.0.0"))).isTrue()
  }

  @Test
  fun testOlderLocalVersion() {
    assertThat(isNewRemoteVersion(setOf("2.0.1"), setOf("2.0.0"))).isTrue()
  }

  @Test
  fun testSameLocalVersion() {
    assertThat(isNewRemoteVersion(setOf("2.0.0"), setOf("2.0.0"))).isFalse()
  }

  @Test
  fun failingRemoteResultsInException() {
    try {
      checkAndTweet(FixedSource(fetcher = FailingFetcher()), VersionsStore(MemStore(), moshi), VersionsStore(MemStore(), moshi), noTweet)
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
      checkAndTweet(FixedSource(fetcher = EmptyFetcher()), VersionsStore(MemStore(), moshi), VersionsStore(MemStore(), moshi), noTweet)
    }
    catch (e: IOException) {
      assertThat(e).hasMessageThat().isEqualTo("Did not get a proper remote version")
      return
    }
    throw IllegalStateException("Expected checkAndTweet to fail")
  }

  @Test
  fun emptyLocalStateResultsInTweet() {
    val cache = VersionsStore(MemStore(), moshi)
    val db = VersionsStore(MemStore(), moshi)

    checkAndTweet(FixedSource(), cache, db, tweeter)

    assertThat(tweets).containsExactly("Artifact 1.0.0 is out!")
    // make sure that both cache and db are updated too
    assertThat(cache.versions("key")).containsExactly("1.0.0")
    assertThat(db.versions("key")).containsExactly("1.0.0")
  }

  @Test
  fun emptyCacheStateResultInNoTweet() {
    val cache = VersionsStore(MemStore(), moshi)
    val db = VersionsStore(MemStore(), moshi).apply {
      store("key", setOf("1.0.0"))
    }

    checkAndTweet(FixedSource(), cache, db, noTweet)

    // make sure that cache gets updated
    assertThat(cache.versions("key")).containsExactly("1.0.0")
  }

  @Test
  fun cacheAndDbAreUpToDate() {
    val cache = VersionsStore(MemStore(), moshi).apply {
      store("key", setOf("1.0.0"))
    }
    val db = VersionsStore(object : Store {
      override fun read(key: String): String? = throw IllegalStateException("Not supposed to hit db")
      override fun write(key: String, value: String) = throw IllegalStateException("Not supposed to hit db")
    }, moshi)

    checkAndTweet(FixedSource(), cache, db, noTweet)
  }

  @Test
  fun cacheAndDbAreOutOfDate() {
    val cache = VersionsStore(MemStore(), moshi).apply {
      store("key", setOf("0.9.0"))
    }
    val db = VersionsStore(MemStore(), moshi).apply {
      store("key", setOf("0.9.0"))
    }

    checkAndTweet(FixedSource(), cache, db, tweeter)

    assertThat(tweets).containsExactly("Artifact 1.0.0 is out!")
    // make sure that both cache and db are updated too
    assertThat(cache.versions("key")).containsExactly("1.0.0")
    assertThat(db.versions("key")).containsExactly("1.0.0")
  }

  @Test
  fun olderVersionIsReleased() {
    val cache = VersionsStore(MemStore(), moshi).apply {
      store("key", setOf("2.0.0"))
    }
    val db = VersionsStore(MemStore(), moshi).apply {
      store("key", setOf("2.0.0"))
    }

    checkAndTweet(FixedSource(fetcher = FixedFetcher("1.0.0", "2.0.0")), cache, db, tweeter)

    assertThat(tweets).containsExactly("Artifact 1.0.0 is out!")
  }

  @Test
  fun multipleNewVersionsResultsInMultipleTweets() {
    val cache = VersionsStore(MemStore(), moshi)
    val db = VersionsStore(MemStore(), moshi)

    checkAndTweet(FixedSource(fetcher = FixedFetcher("1.0.0", "2.0.0")), cache, db, tweeter)

    assertThat(tweets).containsExactly(
      "Artifact 1.0.0 is out!",
      "Artifact 2.0.0 is out!"
    )
  }
}