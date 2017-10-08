package pink.madis.agptweeter

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
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
}

enum class ArtifactConfig(override val fetcher: Fetcher, override val key: String, override val prettyName: String): ArtifactSource {
    AGP(GoogleFetcher(agpCoords), agpCoords.toKey(), "Android Gradle Plugin"),
    SUPPORTLIB(GoogleFetcher(supportLibCoords), supportLibCoords.toKey(), "Android Support Library"),
    GRADLE(GradleFetcher(versionsApi), "gradle", "Gradle");

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