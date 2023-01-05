package pink.madis.agptweeter

import pink.madis.agptweeter.store.VersionsStore
import java.io.IOException
import java.time.Duration
import java.time.Instant

typealias Tweeter = (String) -> Unit
typealias UrlChecker = (String) -> Boolean
typealias Clock = () -> Instant

fun Int.hours(): Duration = Duration.ofHours(this.toLong())
fun Int.minutes(): Duration = Duration.ofMinutes(this.toLong())

fun isNewRemoteVersion(remoteVersions: Set<String>, localVersions: Set<String>): Boolean {
  return !localVersions.containsAll(remoteVersions)
}

fun checkAndTweet(
    artifactSource: ArtifactSource,
    cache: VersionsStore,
    db: VersionsStore,
    clock: Clock,
    urlChecker: UrlChecker,
    tweet: Tweeter
) {
  println("Checking for new versions of ${artifactSource.prettyName}")

  val remote = artifactSource.fetcher

  val remoteVersions = remote.versions() ?: throw IOException("Did not get a proper remote version")

  val localVersions = cache.versions(artifactSource.key)

  // hit cache first, because the most common case is that we're not hitting anything
  if (localVersions.versions.containsAll(remoteVersions)) {
    println("Local cache already has all versions, no tweet")
    return
  }

  val knownVersions = db.versions(artifactSource.key)
  if (knownVersions.versions.containsAll(remoteVersions)) {
    println("DynamoDB already has all versions, no tweet")
    // update local cache too so that we don't hit dynamodb for the next run
    cache.store(artifactSource.key, knownVersions)
    return
  }

  val knownPendingVersions = knownVersions.pending.map { it.version }.toSet()
  val pendingVersions = knownVersions.pending +
      remoteVersions.filter { it !in knownVersions.versions && it !in knownPendingVersions }.map { PendingVersion(it, clock()) }

  val toTweet = pendingVersions.filter { canTweet(artifactSource, clock, urlChecker, it) }.map { it.version }.toSet()

  toTweet.forEach { version ->
    val releaseNotes = artifactSource.releaseNotes(version)
    var tweetContents = "${artifactSource.prettyName} $version is out!"
    if (releaseNotes != null) {
      tweetContents += " $releaseNotes"
    }
    println("Tweet: $tweetContents")
    tweet(tweetContents)
  }

  // we've definitely tweeted now, update both cache and db
  val newStoredVersions = StoredVersions(knownVersions.versions + toTweet, pendingVersions.filter { it.version !in toTweet })
  db.store(artifactSource.key, newStoredVersions)
  cache.store(artifactSource.key, newStoredVersions)
}

fun canTweet(artifactSource: ArtifactSource, clock: Clock, checker: UrlChecker, pendingVersion: PendingVersion): Boolean {
  val notes = artifactSource.releaseNotes(pendingVersion.version)
  return when {
    // no release notes URL at all, OK to tweet
    notes.isNullOrBlank() -> true
    // release notes timed out by now (older than 1hr, ok to tweet)
    // TODO(madis) should we still include the relnotes URL in this case?
    (pendingVersion.seenAt + 1.hours()).isBefore(clock()) -> true
    // release notes are live
    checker(notes) -> true
    else -> false
  }
}
