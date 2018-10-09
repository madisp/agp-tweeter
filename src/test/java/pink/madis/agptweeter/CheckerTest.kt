package pink.madis.agptweeter

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pink.madis.agptweeter.store.DB_VERSION
import pink.madis.agptweeter.store.MemStore
import pink.madis.agptweeter.store.Store
import pink.madis.agptweeter.store.VersionsStore
import java.io.IOException
import java.time.Instant

class EmptyFetcher: Fetcher {
  override fun versions(): Set<String>? = null
}

class FixedFetcher(private vararg val versions: String): Fetcher {
  override fun versions(): Set<String>? = setOf(*versions)
}

class FailingFetcher: Fetcher {
  override fun versions(): Set<String>? = throw IOException("Expected failure")
}

open class FixedSource(
    override val fetcher: Fetcher = FixedFetcher("1.0.0"),
    override val key: String = "key",
    override val prettyName: String = "Artifact"): ArtifactSource

class FixedReleaseNotesSource: FixedSource() {
  override fun releaseNotes(version: String): String? = "https://example.com/$version/release-notes.html"
}

fun noTweet(tweet: String) { throw IllegalStateException("Unexpected tweet: $tweet") }

class CheckerTest {
  private val tweets = mutableListOf<String>()
  private val tweeter: (String)->Unit = { tweets.add(it) }

  private var time = Instant.parse("2018-12-03T15:15:30.00Z")
  private val clock = { time }

  private val cacheStore = MemStore()
  private val dbStore = MemStore()

  // these are lazy so we can toy around with the backing stores before migrations happen
  private val cache by lazy { VersionsStore(cacheStore, moshi) }
  private val db by lazy { VersionsStore(dbStore, moshi) }

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
      checkAndTweet(FixedSource(fetcher = FailingFetcher()), cache, db, clock, { true }, ::noTweet)
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
      checkAndTweet(FixedSource(fetcher = EmptyFetcher()), cache, db, clock, { true }, ::noTweet)
    }
    catch (e: IOException) {
      assertThat(e).hasMessageThat().isEqualTo("Did not get a proper remote version")
      return
    }
    throw IllegalStateException("Expected checkAndTweet to fail")
  }

  @Test
  fun emptyLocalStateResultsInTweet() {
    checkAndTweet(FixedSource(), cache, db, clock, { true }, tweeter)

    assertThat(tweets).containsExactly("Artifact 1.0.0 is out!")
    // make sure that both cache and db are updated too
    assertThat(cache.versions("key").versions).containsExactly("1.0.0")
    assertThat(db.versions("key").versions).containsExactly("1.0.0")
  }

  @Test
  fun emptyCacheStateResultInNoTweet() {
    db.store("key", StoredVersions(setOf("1.0.0"), emptyList()))

    checkAndTweet(FixedSource(), cache, db, clock, { true }, ::noTweet)

    // make sure that cache gets updated
    assertThat(cache.versions("key").versions).containsExactly("1.0.0")
  }

  @Test
  fun sourceWithReleaseNotesResultsInTweetWithReleaseNotes() {
    checkAndTweet(FixedReleaseNotesSource(), cache, db, clock, { true }, tweeter)
    assertThat(tweets).containsExactly("Artifact 1.0.0 is out! https://example.com/1.0.0/release-notes.html")
  }

  @Test
  fun cacheAndDbAreUpToDate() {
    cache.store("key", StoredVersions(setOf("1.0.0"), emptyList()))

    val db = VersionsStore(object : Store {
      override fun read(key: String) = if (key == "db_version") DB_VERSION.toString() else throw IllegalStateException("Not supposed to hit db")
      override fun write(key: String, value: String) = throw IllegalStateException("Not supposed to hit db")
    }, moshi)

    checkAndTweet(FixedSource(), cache, db, clock, { true }, ::noTweet)
  }

  @Test
  fun cacheAndDbAreOutOfDate() {
    cache.store("key", StoredVersions(setOf("0.9.0"), emptyList()))
    db.store("key", StoredVersions(setOf("0.9.0"), emptyList()))

    checkAndTweet(FixedSource(), cache, db, clock, { true }, tweeter)

    assertThat(tweets).containsExactly("Artifact 1.0.0 is out!")
    // make sure that both cache and db are updated too
    assertThat(cache.versions("key").versions).containsExactly("0.9.0", "1.0.0")
    assertThat(db.versions("key").versions).containsExactly("0.9.0", "1.0.0")
  }

  @Test
  fun olderVersionIsReleased() {
    cache.store("key", StoredVersions(setOf("2.0.0"), emptyList()))
    db.store("key", StoredVersions(setOf("2.0.0"), emptyList()))

    checkAndTweet(FixedSource(fetcher = FixedFetcher("1.0.0", "2.0.0")), cache, db, clock, { true }, tweeter)

    assertThat(tweets).containsExactly("Artifact 1.0.0 is out!")
  }

  @Test
  fun multipleNewVersionsResultsInMultipleTweets() {
    checkAndTweet(FixedSource(fetcher = FixedFetcher("1.0.0", "2.0.0")), cache, db, clock, { true }, tweeter)

    assertThat(tweets).containsExactly(
      "Artifact 1.0.0 is out!",
      "Artifact 2.0.0 is out!"
    )
  }

  @Test
  fun migrateOldDb() {
    cacheStore.write("key", """{"versions":["1.0.0", "2.0.0"]}""")
    dbStore.write("key", """{"versions":["1.0.0", "2.0.0"]}""")

    checkAndTweet(FixedSource(fetcher = FixedFetcher("1.0.0", "2.0.0")), cache, db, clock, { true }, ::noTweet)

    assertThat(cacheStore.read("db_version")).isEqualTo("2")
    assertThat(cacheStore.read("db_keys")).isNotEmpty()
  }

  @Test
  fun releaseNotesComeLater() {
    val source = FixedReleaseNotesSource()
    checkAndTweet(source, cache, db, clock, { false }, ::noTweet)

    // at some time later the URL becomes valid
    time += 5.minutes()
    checkAndTweet(source, cache, db, clock, { true }, tweeter)

    // after which it's not pending any more and doesn't spam anything
    time += 5.minutes()
    checkAndTweet(source, cache, db, clock, { true }, ::noTweet)

    assertThat(tweets).containsExactly(
        "Artifact 1.0.0 is out! https://example.com/1.0.0/release-notes.html"
    )

    assertThat(cache.versions("key").pending.isEmpty())
    assertThat(db.versions("key").pending.isEmpty())
  }

  @Test
  fun releaseNotesNeverCome() {
    val source = FixedReleaseNotesSource()
    checkAndTweet(source, cache, db, clock, { false }, ::noTweet)

    // at some time later the URL becomes valid
    time += 5.minutes()
    checkAndTweet(source, cache, db, clock, { false }, ::noTweet)

    // after an hour we fire out the tweet anyway
    time += 1.hours()
    checkAndTweet(source, cache, db, clock, { false }, tweeter)

    // after which it's not pending any more and doesn't spam anything, even if the relnotes become available
    time += 5.minutes()
    checkAndTweet(source, cache, db, clock, { true }, ::noTweet)

    assertThat(tweets).containsExactly(
        "Artifact 1.0.0 is out! https://example.com/1.0.0/release-notes.html"
    )

    assertThat(cache.versions("key").pending.isEmpty())
    assertThat(db.versions("key").pending.isEmpty())
  }
}