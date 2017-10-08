package pink.madis.agptweeter

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val AGP_COORDS = MavenCoords("com.android.tools.build", "gradle")
val SUPPORTLIB_COORDS = MavenCoords("com.android.support", "support-v4")

val okClient = OkHttpClient.Builder().build()
val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
val versionsApi = Retrofit.Builder()
        .client(okClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl("https://services.gradle.org")
        .build()
        .create(GradleVersionsApi::class.java)

enum class ArtifactSource(val fetcher: Fetcher, val key: String, val prettyName: String) {
    AGP(GoogleFetcher(AGP_COORDS), AGP_COORDS.toKey(), "Android Gradle Plugin"),
    SUPPORTLIB(GoogleFetcher(SUPPORTLIB_COORDS), SUPPORTLIB_COORDS.toKey(), "Android Support Library"),
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