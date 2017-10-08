package pink.madis.agptweeter

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.nio.file.Paths

class Input

val cache = FileStore(Paths.get("/tmp/agp-tweeter"))

val db = DynamoStore(DynamoDB(AmazonDynamoDBClient()).getTable("agp-tweeter"))

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
            checkAndTweet(it, cache, db, { twitter.updateStatus(it) })
        }
        return ""
    }
}