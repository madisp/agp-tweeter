package pink.madis.agptweeter

import retrofit2.Call
import retrofit2.http.GET
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

/**
 * Fetches latest version information for a specific artifact from somewhere. Could be local cache, remote repo, etc...
 */
interface Fetcher {
  fun versions(): Set<String>?
}

/**
 * Fetches a latest version from the Google maven repo
 */
class GoogleFetcher(private val coords: MavenCoords): Fetcher {
  override fun versions(): Set<String>? {
    val url = "https://dl.google.com/dl/android/maven2/${coords.groupId.replace('.', '/')}/group-index.xml"
    val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
    val xpath = XPathFactory.newInstance().newXPath().compile("/${coords.groupId}/${coords.artifactId}/@versions")
    val versions = xpath.evaluate(xml).split(',')
    return versions.toSet()
  }
}

/**
 * Fetches a latest version of Gradle
 */
class GradleFetcher(private val endpointVersions: GradleVersionsApi): Fetcher {
  override fun versions(): Set<String>? {
    val versionResponse = endpointVersions.all().execute()
    if (!versionResponse.isSuccessful) {
      return null
    }
    val body = versionResponse.body() ?: return null
    return body.asSequence()
        .filter { !it.nightly && !it.snapshot }
        .map { it.version }
        .toSet()
  }
}

interface GradleVersionsApi {
  @GET("/versions/all")
  fun all(): Call<List<GradleVersion>>
}