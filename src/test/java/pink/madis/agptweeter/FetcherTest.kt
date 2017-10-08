package pink.madis.agptweeter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.junit.Assert.assertNull
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
    val version = GoogleFetcher(coords).latestVersion()
    println("Latest AGP version is $version")
    assertThat(version).isGreaterThan(Version("0.0.1"))
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

    val version = GradleFetcher(versionsApi).latestVersion()
    println("Latest Gradle version is $version")
    assertThat(version).isGreaterThan(Version("0.0.1"))
  }

  @Test
  fun gradleFetcherIgnoresNightlies() {
    val versionsApi = FixedGradleApi(
        GradleVersion(version = "4.4-20171008000020+0000", buildTime = "20171008000020+0000", snapshot = false, nightly = true),
        GradleVersion(version = "4.3-rc-1", buildTime = "20171006000020+0000", snapshot = false, nightly = false)
    )
    val version = GradleFetcher(versionsApi).latestVersion()
    assertThat(version).isEqualTo(Version("4.3-rc-1"))
  }

  @Test
  fun gradleFetcherIgnoresSnapshots() {
    val versionsApi = FixedGradleApi(
        GradleVersion(version = "4.4-20171008000020+0000", buildTime = "20171008000020+0000", snapshot = true, nightly = false),
        GradleVersion(version = "4.3-rc-1", buildTime = "20171006000020+0000", snapshot = false, nightly = false)
    )
    val version = GradleFetcher(versionsApi).latestVersion()
    assertThat(version).isEqualTo(Version("4.3-rc-1"))
  }

  @Test
  fun gradleFetcherSortsBySemanticVersion() {
    val versionsApi = FixedGradleApi(
        GradleVersion(version = "4.4", buildTime = "20171008000020+0000", snapshot = false, nightly = false),
        GradleVersion(version = "30.0", buildTime = "20171006000020+0000", snapshot = false, nightly = false)
    )
    val version = GradleFetcher(versionsApi).latestVersion()
    assertThat(version).isEqualTo(Version("30.0"))
  }

  @Test
  fun emptyStore() {
    assertNull(StoreFetcher(MemStore(), "key").latestVersion())
  }

  @Test
  fun updateStore() {
    // start off with one item
    val store = MemStore()
    val fetcher = StoreFetcher(store, "key")

    store.write("key", "0.0.1".toByteArray())

    assertThat(fetcher.latestVersion()!!.orig).isEqualTo("0.0.1")

    store.write("key", "0.0.10".toByteArray())
    assertThat(fetcher.latestVersion()!!.orig).isEqualTo("0.0.10")

    // other coordinates should still be empty
    assertThat(StoreFetcher(store, "key2").latestVersion()).isNull()
  }
}