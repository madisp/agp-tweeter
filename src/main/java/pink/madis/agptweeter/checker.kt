package pink.madis.agptweeter

import java.io.IOException

fun isNewRemoteVersion(remoteVersions: Set<String>, localVersions: Set<String>): Boolean {
  return !localVersions.containsAll(remoteVersions)
}

fun checkAndTweet(artifactSource: ArtifactSource, cache: VersionsStore, db: VersionsStore, tweet: (String)->Unit) {
  println("Checking for new versions of ${artifactSource.prettyName}")

  val remote = artifactSource.fetcher

  val remoteVersions = remote.versions() ?: throw IOException("Did not get a proper remote version")

  // hit cache first, because the most common case is that we're not hitting anything
  if (!isNewRemoteVersion(remoteVersions, cache.versions(artifactSource.key))) {
    println("Local cache already has all versions, no tweet")
    return
  }

  val knownVersions = db.versions(artifactSource.key)
  if (!isNewRemoteVersion(remoteVersions, knownVersions)) {
    println("DynamoDB already has all versions, no tweet")
    // update local cache too so that we don't hit dynamodb for the next run
    cache.store(artifactSource.key, remoteVersions)
    return
  }

  val newVersions = remoteVersions - knownVersions

  newVersions.forEach { version ->
    val releaseNotes = artifactSource.releaseNotes(version)
    var tweetContents = "${artifactSource.prettyName} $version is out!"
    if (releaseNotes != null) {
      tweetContents += " $releaseNotes"
    }
    println("Tweet: $tweetContents")
    tweet(tweetContents)
  }

  // we've definitely tweeted now, update both cache and db
  cache.store(artifactSource.key, remoteVersions)
  db.store(artifactSource.key, remoteVersions)
}