package pink.madis.agptweeter

import java.nio.charset.StandardCharsets.UTF_8
import java.io.IOException


fun isNewRemoteVersion(remoteVersion: Version, local: Fetcher): Boolean {
  val localVersion = local.latestVersion() ?: return true // no local, so definitely later
  return remoteVersion > localVersion
}

fun checkAndTweet(artifactSource: ArtifactSource, cache: Store, db: Store, tweet: (String)->Unit) {
  println("Checking for new versions of ${artifactSource.prettyName}")

  val remote = artifactSource.fetcher

  val remoteVersion = remote.latestVersion() ?: throw IOException("Did not get a proper remote version")

  println("Latest remote version is ${remoteVersion.orig}")

  // hit cache first, because the most common case is that we're not hitting anything
  if (!isNewRemoteVersion(remoteVersion, StoreFetcher(cache, artifactSource.key))) {
    println("Local cache already has version, no tweet")
    return
  }

  if (!isNewRemoteVersion(remoteVersion, StoreFetcher(db, artifactSource.key))) {
    println("DynamoDB already has this version, no tweet")
    // update local cache too so that we don't hit dynamodb for the next run
    cache.write(artifactSource.key, remoteVersion.orig.toByteArray(UTF_8))
    return
  }

  val tweetContents = "${artifactSource.prettyName} ${remoteVersion.orig} is out!"
  println("Tweet: $tweetContents")
  tweet(tweetContents)

  // we've definitely tweeted now, update both cache and db
  cache.write(artifactSource.key, remoteVersion.orig.toByteArray(UTF_8))
  db.write(artifactSource.key, remoteVersion.orig.toByteArray(UTF_8))
}