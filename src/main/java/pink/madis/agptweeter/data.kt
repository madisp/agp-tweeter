package pink.madis.agptweeter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.Instant

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

/**
 * A version that was seen but for some reason not tweeted yet. Best example is Gradle where we want to hold off the
 * tweet until release notes page is available.
 */
data class PendingVersion(
    val version: String,
    val seenAt: Instant
)

/**
 * A list of both tweeted & pending versions
 */
data class StoredVersions(
    val versions: Set<String>,
    val pending: List<PendingVersion>
)

class InstantAdapter : JsonAdapter<Instant>() {
  override fun fromJson(reader: JsonReader): Instant = Instant.parse(reader.nextString())

  override fun toJson(writer: JsonWriter, value: Instant?) {
    writer.value(value.toString())
  }
}