package pink.madis.agptweeter

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory
import kotlin.text.Charsets.UTF_8

/**
 * Fetches latest version information from somewhere. Could be local cache, remote repo, etc...
 */
interface Fetcher {
    fun latestVersion(coords: MavenCoords): Version?
}

/**
 * Fetches a cached latest version from the given store
 */
class StoreFetcher(val store: Store): Fetcher {
    override fun latestVersion(coords: MavenCoords): Version? {
        return store.read(coords.toKey())?.toString(UTF_8)?.let { Version(it, coords) }
    }
}

/**
 * Fetches a latest version from the Google maven repo
 */
class GoogleFetcher: Fetcher {
    override fun latestVersion(coords: MavenCoords): Version? {
        val url = "https://dl.google.com/dl/android/maven2/${coords.groupId.replace('.', '/')}/group-index.xml"
        val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
        val xpath = XPathFactory.newInstance().newXPath().compile("/${coords.groupId}/${coords.artifactId}/@versions")
        val versions = xpath.evaluate(xml).split(',')
        return versions.map { Version(it, coords) }.max()
    }
}