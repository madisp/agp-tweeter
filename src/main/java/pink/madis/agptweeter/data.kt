package pink.madis.agptweeter

data class MavenCoords(
    val groupId: String,
    val artifactId: String
)

data class TwitterConfig(
    val consumerKey: String,
    val consumerSecret: String,
    val accessToken: String,
    val accessTokenSecret: String
)

data class Config(
    val key: String,
    val twitter: TwitterConfig,
    val prettyName: String
)

data class GradleVersion(
    val version: String,
    val buildTime: String,
    val snapshot: Boolean,
    val nightly: Boolean
)

data class StoredVersions(
    val versions: List<String>
)