package pink.madis.agptweeter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.junit.Ignore
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.mock.Calls

class FixedGradleApi(private vararg val versions: GradleVersion): GradleVersionsApi {
  override fun all(): Call<List<GradleVersion>> = Calls.response(listOf(*versions))
}

class FetcherTest {
  @Test
  @Ignore("Actually hits network, manual running only")
  fun latestGoogleVersion() {
    val coords = MavenCoords("com.android.tools.build", "gradle")
    val versions = GoogleFetcher(coords).versions()
    println("Found agp versions $versions")
    assertThat(versions).containsAllOf(
      "3.0.0-alpha1",
      "3.0.0-alpha2",
      "3.0.0-alpha3",
      "3.0.0-alpha4",
      "3.0.0-alpha5",
      "3.0.0-alpha6",
      "3.0.0-alpha7",
      "3.0.0-alpha8",
      "3.0.0-alpha9",
      "3.0.0-beta1",
      "3.0.0-beta2",
      "3.0.0-beta3",
      "3.0.0-beta4",
      "3.0.0-beta5",
      "3.0.0-beta6",
      "3.0.0-beta7"
    )
  }

  @Test
  @Ignore("Actually hits network, manual running only")
  fun latestGradleVersion() {
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

    val versions = GradleFetcher(versionsApi).versions()
    println("Found gradle versions $versions")
    assertThat(versions).containsAllOf(
        "4.2.1", "4.2", "4.2-rc-2", "4.2-rc-1",
        "4.1", "4.1-rc-2", "4.1-rc-1", "4.1-milestone-1",
        "4.0.2", "4.0.1", "4.0", "4.0-rc-3", "4.0-rc-2", "4.0-rc-1", "4.0-milestone-2", "4.0-milestone-1",
        "3.5.1", "3.5", "3.5-rc-3", "3.5-rc-2", "3.5-rc-1",
        "3.4.1", "3.4", "3.4-rc-3", "3.4-rc-2", "3.4-rc-1",
        "3.3", "3.3-rc-1",
        "3.2.1", "3.2", "3.2-rc-3", "3.2-rc-2", "3.2-rc-1",
        "3.1", "3.1-rc-1",
        "3.0", "3.0-rc-2", "3.0-rc-1", "3.0-milestone-2", "3.0-milestone-1",
        "2.14.1", "2.14.1-rc-2", "2.14.1-rc-1", "2.14", "2.14-rc-6", "2.14-rc-5", "2.14-rc-4", "2.14-rc-3", "2.14-rc-2", "2.14-rc-1",
        "2.13", "2.13-rc-2", "2.13-rc-1",
        "2.12", "2.12-rc-1",
        "2.11", "2.11-rc-3", "2.11-rc-2", "2.11-rc-1",
        "2.10", "2.10-rc-2", "2.10-rc-1",
        "2.9", "2.9-rc-1",
        "2.8", "2.8-rc-2", "2.8-rc-1",
        "2.7", "2.7-rc-2", "2.7-rc-1",
        "2.6", "2.6-rc-2", "2.6-rc-1",
        "2.5", "2.5-rc-2", "2.5-rc-1",
        "2.4", "2.4-rc-2", "2.4-rc-1",
        "2.3", "2.3-rc-4", "2.3-rc-3", "2.3-rc-2", "2.3-rc-1",
        "2.2.1", "2.2.1-rc-1",
        "2.2", "2.2-rc-2", "2.2-rc-1",
        "2.1", "2.1-rc-4", "2.1-rc-3", "2.1-rc-2", "2.1-rc-1",
        "2.0", "2.0-rc-2", "2.0-rc-1",
        "1.12", "1.12-rc-2", "1.12-rc-1",
        "1.11", "1.11-rc-1",
        "1.10", "1.10-rc-2", "1.10-rc-1",
        "1.9", "1.9-rc-4", "1.9-rc-3", "1.9-rc-2", "1.9-rc-1",
        "1.8", "1.8-rc-2", "1.8-rc-1",
        "1.7", "1.7-rc-2", "1.7-rc-1",
        "1.6", "1.6-rc-1",
        "1.5", "1.5-rc-3", "1.5-rc-2", "1.5-rc-1",
        "1.4", "1.4-rc-3", "1.4-rc-2", "1.4-rc-1",
        "1.3", "1.3-rc-2", "1.3-rc-1",
        "1.2", "1.2-rc-1",
        "1.1", "1.1-rc-2", "1.1-rc-1",
        "1.0", "1.0-rc-3", "1.0-rc-2", "1.0-rc-1",
        "1.0-milestone-9", "1.0-milestone-8a", "1.0-milestone-8", "1.0-milestone-7", "1.0-milestone-6", "1.0-milestone-5", "1.0-milestone-4", "1.0-milestone-3", "1.0-milestone-2", "1.0-milestone-1",
        "0.9.2", "0.9.1", "0.9", "0.9-rc-3", "0.9-rc-2", "0.9-rc-1",
        "0.8",
        "0.7"
    )
  }

  @Test
  fun gradleFetcherIgnoresNightliesAndSnapshots() {
    val versionsApi = FixedGradleApi(
        GradleVersion(version = "4.4-20171008000020+0000", buildTime = "20171008000020+0000", snapshot = false, nightly = true),
        GradleVersion(version = "4.4-20171007000020+0000", buildTime = "20171007000020+0000", snapshot = true, nightly = false),
        GradleVersion(version = "4.3-rc-1", buildTime = "20171006000020+0000", snapshot = false, nightly = false),
        GradleVersion(version = "4.2", buildTime = "20171004000020+0000", snapshot = false, nightly = false)
    )
    val versions = GradleFetcher(versionsApi).versions()
    assertThat(versions).containsExactly("4.3-rc-1", "4.2")
  }

  @Test
  fun emptyStore() {
    assertThat(StoreFetcher(VersionsStore(MemStore(), moshi), "key").versions()).isEmpty()
  }

  @Test
  fun updateStore() {
    // start off with one item
    val store = VersionsStore(MemStore(), moshi)
    val fetcher = StoreFetcher(store, "key")

    store.store("key", setOf("0.0.1"))

    assertThat(fetcher.versions()).containsExactly("0.0.1")

    store.store("key", setOf("0.0.1", "0.0.10"))
    assertThat(fetcher.versions()).containsExactly("0.0.1", "0.0.10")

    // other coordinates should still be empty
    assertThat(StoreFetcher(store, "key2").versions()).isEmpty()
  }
}