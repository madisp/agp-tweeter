import com.squareup.moshi.Moshi
import twitter4j.TwitterFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

import twitter4j.conf.ConfigurationBuilder

val utf8 = StandardCharsets.UTF_8
val moshi = Moshi.Builder().build()
const val placeholder = "__VERSION__"

fun check(config: Config) {
  println("Checking for new versions of ${config.artifact}")
  val db = Paths.get(config.db)
  val seenVersions = if (Files.exists(db)) Files.lines(db, utf8).collect(Collectors.toSet()) else emptySet<String>()
  val versions = fetchVersions(config.artifact)
  val newVersions = versions.filterNot { seenVersions.contains(it) }
  if (newVersions.isEmpty()) {
    // nothing to do
    return
  }
  println("${newVersions.size} new versions found!")
  // fire out tweet and serialize the new seen versions
  newVersions.forEach {
    tweet(config.twitter, config.message.replace(placeholder, it))
  }
  Files.write(db, seenVersions + newVersions, utf8)
}

data class MavenCoords(val groupId: String, val artifactId: String)
data class TwitterConfig(
    val consumerKey: String,val consumerSecret: String,
    val accessToken: String, val accessTokenSecret: String
)
data class Config(val artifact: MavenCoords, val twitter: TwitterConfig, val message: String, val db: String)

fun fetchVersions(coords: MavenCoords): List<String> {
  val url = "https://dl.google.com/dl/android/maven2/${coords.groupId.replace('.', '/')}/group-index.xml"
  val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url)
  val xpath = XPathFactory.newInstance().newXPath().compile("/${coords.groupId}/${coords.artifactId}/@versions")
  return xpath.evaluate(xml).split(',')
}

fun tweet(config: TwitterConfig, message: String) {
  val twitter = TwitterFactory(ConfigurationBuilder()
      .setOAuthAccessToken(config.accessToken)
      .setOAuthAccessTokenSecret(config.accessTokenSecret)
      .setOAuthConsumerKey(config.consumerKey)
      .setOAuthConsumerSecret(config.consumerSecret).build()).instance
  twitter.updateStatus(message)
}

object Main {
  @JvmStatic fun main(args: Array<String>) {
    // read in config from the first arg
    if (args.size != 1) {
      throw IllegalArgumentException("Usage: java -cp agp-tweeter-all.jar Main path/to/config/file")
    }

    val configString = String(Files.readAllBytes(Paths.get(args[0])), utf8)
    val config = moshi.adapter(Config::class.java).fromJson(configString)!!

    while (true) {
      try {
        check(config)
      }
      catch (e: Exception) {
        e.printStackTrace()
      }
      finally {
        Thread.sleep(TimeUnit.MINUTES.toMillis(15))
      }
    }
  }
}