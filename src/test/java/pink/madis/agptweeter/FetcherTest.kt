package pink.madis.agptweeter

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.text.Charsets.UTF_8

class FetcherTest {
    @Test
    @Ignore("Actually hits network, manual running only")
    fun latestGoogleVersion() {
        val coords = MavenCoords("com.android.tools.build", "gradle")
        val version = GoogleFetcher(coords).latestVersion()
        println("Latest AGP version is $version")
        assertTrue(version!! > Version("0.0.1"))
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
        assertTrue(version!! > Version("0.0.1"))
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

        val coords = MavenCoords("com.android.tools.build", "gradle")
        store.write("key", "0.0.1".toByteArray(UTF_8))

        assertTrue(fetcher.latestVersion()!!.orig == "0.0.1")

        store.write("key", "0.0.10".toByteArray(UTF_8))
        assertTrue(fetcher.latestVersion()!!.orig == "0.0.10")

        // other coordinates should still be empty
        assertNull(StoreFetcher(store, "key2").latestVersion())
    }
}