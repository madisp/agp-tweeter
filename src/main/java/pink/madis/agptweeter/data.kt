package pink.madis.agptweeter

import org.apache.commons.codec.digest.DigestUtils

data class MavenCoords(val groupId: String, val artifactId: String) {
  // compute a key from coords, the hashing is the dumbest way to get a safe (unique) string
  fun toKey(): String = DigestUtils.sha1Hex("$groupId:$artifactId")
}

data class TwitterConfig(
  val consumerKey: String, val consumerSecret: String,
  val accessToken: String, val accessTokenSecret: String
)

data class Config(val key: String, val twitter: TwitterConfig, val prettyName: String)

data class GradleVersion(
  val version: String,
  val buildTime: String,
  val snapshot: Boolean,
  val nightly: Boolean
)

data class StoredVersions(
  val versions: List<String>
)