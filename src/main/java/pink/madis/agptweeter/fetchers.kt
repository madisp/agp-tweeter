package pink.madis.agptweeter

import retrofit2.Call
import retrofit2.http.GET
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory
import kotlin.text.Charsets.UTF_8

/**
 * Fetches latest version information for a specific artifact from somewhere. Could be local cache, remote repo, etc...
 */
interface Fetcher {
    fun latestVersion(): Version?
}

/**
 * Fetches a cached latest version from the given store
 */
class StoreFetcher(private val store: Store, private val key: String): Fetcher {
    override fun latestVersion(): Version? {
        return store.read(key)?.toString(UTF_8)?.let { Version(it) }
    }
}

/**
 * Fetches a latest version from the Google maven repo
 */
class GoogleFetcher(private val coords: MavenCoords): Fetcher {
    override fun latestVersion(): Version? {
        val url = "https://dl.google.com/dl/android/maven2/${coords.groupId.replace('.', '/')}/group-index.xml"
        val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
        val xpath = XPathFactory.newInstance().newXPath().compile("/${coords.groupId}/${coords.artifactId}/@versions")
        val versions = xpath.evaluate(xml).split(',')
        return versions.map { Version(it) }.max()
    }
}
/**
 * Fetches a latest version of Gradle
 */
class GradleFetcher(private val endpointVersions: GradleVersionsApi): Fetcher {
    override fun latestVersion(): Version? {
        val versionResponse = endpointVersions.all().execute()
        if (!versionResponse.isSuccessful) {
            return null
        }
        val version = versionResponse.body()?.filter { !it.nightly && !it.snapshot }?.maxBy { it.version }
        return version?.let { Version(it.version) }
    }
}

interface GradleVersionsApi {
    @GET("/versions/all")
    fun all(): Call<List<GradleVersion>>
}