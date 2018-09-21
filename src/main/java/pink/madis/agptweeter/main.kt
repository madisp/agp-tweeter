package pink.madis.agptweeter

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.nio.file.Paths

class Input

val fileStore = FileStore(Paths.get("/tmp/agp-tweeter")).also { migrate(it) }
val cache = VersionsStore(fileStore, moshi)

val dynamoStore = DynamoStore(DynamoDB(AmazonDynamoDBClient()).getTable("agp-tweeter")).also { migrate(it) }
val db = VersionsStore(dynamoStore, moshi)

@Suppress("unused") // used by AWS
class Handler: RequestHandler<Input, String> {

  override fun handleRequest(input: Input?, context: Context?): String {
    ArtifactConfig.values().forEach {
      val config = it.toConfig()
      val twitter = TwitterFactory(ConfigurationBuilder()
        .setOAuthAccessToken(config.twitter.accessToken)
        .setOAuthAccessTokenSecret(config.twitter.accessTokenSecret)
        .setOAuthConsumerKey(config.twitter.consumerKey)
        .setOAuthConsumerSecret(config.twitter.consumerSecret).build()).instance
      checkAndTweet(it, cache, db) { msg -> twitter.updateStatus(msg) }
    }
    return ""
  }
}

/**
 * Populates dynamodb with latest versions without tweeting anything
 */
@Suppress("unused") // used by AWS
class PopulateHandler: RequestHandler<Input, String> {
  private val oldAgpVersions = listOf("0.1", "0.10.0", "0.10.1", "0.10.2", "0.10.4", "0.11.0", "0.11.1", "0.11.2", "0.12.0", "0.12.1", "0.12.2",
      "0.13.0", "0.13.1", "0.13.2", "0.13.3", "0.14.0", "0.14.1", "0.14.2", "0.14.3", "0.14.4", "0.2", "0.3",
      "0.4.1", "0.4.2", "0.4.3", "0.4", "0.5.0", "0.5.1", "0.5.2", "0.5.3", "0.5.4", "0.5.5", "0.5.6", "0.5.7",
      "0.6.0", "0.6.1", "0.6.2", "0.6.3", "0.7.0", "0.7.1", "0.7.2", "0.7.3", "0.8.0", "0.8.1", "0.8.2", "0.8.3",
      "0.9.0", "0.9.1", "0.9.2", "1.0.0-rc1", "1.0.0-rc2", "1.0.0-rc3", "1.0.0-rc4", "1.0.0", "1.0.1", "1.1.0-rc1",
      "1.1.0-rc2", "1.1.0-rc3", "1.1.0", "1.1.1", "1.1.2", "1.1.3", "1.2.0-beta1", "1.2.0-beta2", "1.2.0-beta3",
      "1.2.0-beta4", "1.2.0-rc1", "1.2.0", "1.2.1", "1.2.2", "1.2.3", "1.3.0-beta1", "1.3.0-beta2", "1.3.0-beta3",
      "1.3.0-beta4", "1.3.0", "1.3.1", "1.4.0-beta1", "1.4.0-beta2", "1.4.0-beta3", "1.4.0-beta4", "1.4.0-beta5",
      "1.4.0-beta6", "1.5.0-beta1", "1.5.0-beta2", "1.5.0-beta3", "1.5.0", "2.0.0-alpha1", "2.0.0-alpha2",
      "2.0.0-alpha3", "2.0.0-alpha5", "2.0.0-alpha6", "2.0.0-alpha7", "2.0.0-alpha8", "2.0.0-alpha9",
      "2.0.0-beta2", "2.0.0-beta4", "2.0.0-beta5", "2.0.0-beta6", "2.0.0-beta7", "2.0.0-rc1", "2.0.0-rc2",
      "2.0.0-rc3", "2.0.0", "2.1.0-alpha1", "2.1.0-alpha3", "2.1.0-alpha4", "2.1.0-alpha5", "2.1.0-beta1",
      "2.1.0-beta3", "2.1.0-rc1", "2.1.0", "2.1.2", "2.1.3", "2.2.0-alpha1", "2.2.0-alpha2", "2.2.0-alpha3",
      "2.2.0-alpha4", "2.2.0-alpha5", "2.2.0-alpha6", "2.2.0-alpha7", "2.2.0-beta1", "2.2.0-beta2", "2.2.0-beta3",
      "2.2.0-rc1", "2.2.0-rc2", "2.2.0", "2.2.1", "2.2.2", "2.2.3", "2.3.0-alpha1", "2.3.0-alpha2", "2.3.0-alpha3",
      "2.3.0-beta1", "2.3.0-beta2", "2.3.0-beta3", "2.3.0-beta4", "2.3.0-rc1", "2.3.0", "2.3.1", "2.3.2", "2.3.3",
      "2.4.0-alpha1", "2.4.0-alpha3", "2.4.0-alpha4", "2.4.0-alpha5", "2.4.0-alpha6", "2.4.0-alpha7",
      "2.5.0-alpha-preview-01", "2.5.0-alpha-preview-02")
  private val oldSupportLibVersions = listOf("13.0.0", "20.0.0", "22.1.0", "23.0.1", "23.3.0", "24.0.0-beta1", "25.0.0", "25.3.0", "18.0.0", "21.0.0", "22.1.1",
      "23.1.0", "23.4.0", "24.1.0", "25.0.1", "25.3.1", "19.0.0", "21.0.2", "22.2.0", "23.1.1", "24.0.0", "24.1.1", "25.1.0",
      "26.0.0-alpha1", "19.0.1", "21.0.3", "22.2.1", "23.2.0", "24.0.0-alpha1", "24.2.0", "25.1.1", "19.1.0", "22.0.0",
      "23.0.0", "23.2.1", "24.0.0-alpha2", "24.2.1", "25.2.0")

  override fun handleRequest(input: Input?, context: Context?): String {
    ArtifactConfig.values().forEach {
      val versions = it.fetcher.versions()!!.toMutableSet()
      when (it) {
        ArtifactConfig.AGP -> versions.addAll(oldAgpVersions)
        ArtifactConfig.SUPPORTLIB -> versions.addAll(oldSupportLibVersions)
        ArtifactConfig.GRADLE -> {}
      }
      cache.store(it.key, versions)
      db.store(it.key, versions)
    }
    return ""
  }
}