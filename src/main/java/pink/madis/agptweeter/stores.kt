package pink.madis.agptweeter

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec
import com.squareup.moshi.Moshi
import kotlin.text.Charsets.UTF_8

/**
 * High level abstraction of a simple key-value store.
 */
interface Store {
  /**
   * reads an item from the store. Key has to be latin1 A-Za-z0-9-_
   */
  @Throws(IOException::class) fun read(key: String): String?

  /**
   * writes an item into the store. Key has to be latin1 A-Za-z0-9-_
   */
  @Throws(IOException::class) fun write(key: String, value: String)
}

/**
 * A dumb key-value store that keeps everything in memory. Useful for testing.
 */
class MemStore: Store {
  private val mem = HashMap<String, String>()

  override fun read(key: String): String? = mem[key]

  override fun write(key: String, value: String) {
    mem.put(key, value)
  }
}

/**
 * A dumb key-value store that maps to files in a folder.
 */
class FileStore(private val basePath: Path): Store {
  override fun read(key: String): String? {
    val f = basePath.resolve(key)
    return if (Files.exists(f)) Files.readAllBytes(f).toString(UTF_8) else null
  }

  override fun write(key: String, value: String) {
    val f = basePath.resolve(key)
    Files.createDirectories(f.parent)
    Files.write(f, value.toByteArray(UTF_8))
  }
}

const val DYNAMO_PRIMARY_KEY = "coords"
const val DYNAMO_ATTRIBUTE_NAME = "versions"

/**
 * A store backed by AWS DynamoDB
 */
class DynamoStore(private val db: Table): Store {
  override fun read(key: String): String? {
    val spec = GetItemSpec().withPrimaryKey(DYNAMO_PRIMARY_KEY, key).withConsistentRead(true)
    val item: Item? = db.getItem(spec)
    return item?.getString(DYNAMO_ATTRIBUTE_NAME)
  }

  override fun write(key: String, value: String) {
    db.putItem(Item().withPrimaryKey(DYNAMO_PRIMARY_KEY, key).withString(DYNAMO_ATTRIBUTE_NAME, value))
  }
}

/**
 * A specific class that stores versions
 */
class VersionsStore(private val backingStore: Store, moshi: Moshi) {
  private val adapter = moshi.adapter(StoredVersions::class.java)

  fun versions(key: String): Set<String> {
    return backingStore.read(key)?.let { adapter.fromJson(it) }?.versions?.toSet() ?: setOf()
  }

  fun store(key: String, versions: Set<String>) {
    backingStore.write(key, adapter.toJson(StoredVersions(versions.toList())))
  }
}