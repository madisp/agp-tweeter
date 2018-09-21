package pink.madis.agptweeter

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.apache.commons.codec.digest.DigestUtils
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val agpCoords = MavenCoords("com.android.tools.build", "gradle")
val supportLibCoords = MavenCoords("com.android.support", "support-v4")

val okClient = OkHttpClient.Builder().build()!!
val moshi = Moshi.Builder()
  .add(KotlinJsonAdapterFactory())
  .build()!!
val versionsApi = Retrofit.Builder()
  .client(okClient)
  .addConverterFactory(MoshiConverterFactory.create(moshi))
  .baseUrl("https://services.gradle.org")
  .build()
  .create(GradleVersionsApi::class.java)!!

interface ArtifactSource {
  val fetcher: Fetcher
  val key: String
  val prettyName: String
  fun releaseNotes(version: String): String? = null
}

// compute a key from coords, the hashing is the dumbest way to get a safe (unique) string
private fun key(coords: MavenCoords) = DigestUtils.sha1Hex("${coords.groupId}:${coords.artifactId}")

enum class ArtifactConfig(override val fetcher: Fetcher, override val key: String, override val prettyName: String): ArtifactSource {
  AGP(GoogleFetcher(agpCoords), key(agpCoords), "Android Gradle Plugin"),
  SUPPORTLIB(GoogleFetcher(supportLibCoords), key(supportLibCoords), "Android Support Library"),
  GRADLE(GradleFetcher(versionsApi), "gradle", "Gradle");

  override fun releaseNotes(version: String): String? {
    return when (this) {
      AGP, SUPPORTLIB -> null
      GRADLE -> "https://docs.gradle.org/$version/release-notes.html"
    }
  }

  fun toConfig(): Config {
    return Config(
      key,
      TwitterConfig(
        System.getenv("${name}_CONSUMER_KEY"),
        System.getenv("${name}_CONSUMER_SECRET"),
        System.getenv("${name}_ACCESS_TOKEN"),
        System.getenv("${name}_ACCESS_TOKEN_SECRET")
      ),
      prettyName
    )
  }
}