package pink.madis.agptweeter

import com.vdurmont.semver4j.Semver
import org.apache.commons.codec.digest.DigestUtils

data class MavenCoords(val groupId: String, val artifactId: String) {
    // compute a key from coords, the hashing is the dumbest way to get a safe (unique) string
    fun toKey(): String = DigestUtils.sha1Hex("$groupId:$artifactId")
}

data class Version(val orig: String, val coords: MavenCoords): Comparable<Version> {
    private val semantic = Semver(orig, Semver.SemverType.COCOAPODS)

    override fun compareTo(other: Version): Int {
        return semantic.compareTo(other.semantic)
    }
}

data class TwitterConfig(
        val consumerKey: String,val consumerSecret: String,
        val accessToken: String, val accessTokenSecret: String
)

data class Config(val artifact: MavenCoords, val twitter: TwitterConfig, val prettyName: String)