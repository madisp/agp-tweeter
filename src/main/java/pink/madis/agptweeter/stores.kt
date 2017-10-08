package pink.madis.agptweeter

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec

/**
 * High level abstraction of a simple key-value store.
 */
interface Store {
    /**
     * reads an item from the store. Key has to be latin1 A-Za-z0-9-_
     */
    @Throws(IOException::class) fun read(key: String): ByteArray?

    /**
     * writes an item into the store. Key has to be latin1 A-Za-z0-9-_
     */
    @Throws(IOException::class) fun write(key: String, bytes: ByteArray)
}

/**
 * A dumb key-value store that keeps everything in memory. Useful for testing.
 */
class MemStore: Store {
    private val mem = HashMap<String, ByteArray>()

    override fun read(key: String): ByteArray? = mem[key]

    override fun write(key: String, bytes: ByteArray) {
        mem.put(key, bytes)
    }
}

/**
 * A dumb key-value store that maps to files in a folder.
 */
class FileStore(private val basePath: Path): Store {
    override fun read(key: String): ByteArray? {
        val f = basePath.resolve(key)
        return if (Files.exists(f)) Files.readAllBytes(f) else null
    }

    override fun write(key: String, bytes: ByteArray) {
        val f = basePath.resolve(key)
        Files.createDirectories(f.parent)
        Files.write(f, bytes)
    }
}

/**
 * A store backed by AWS DynamoDB
 */
class DynamoStore(private val db: Table): Store {
    override fun read(key: String): ByteArray? {
        val spec = GetItemSpec().withPrimaryKey("coords", key).withConsistentRead(true)
        val item: Item? = db.getItem(spec)
        return item?.getBinary("bytes")
    }

    override fun write(key: String, bytes: ByteArray) {
        db.putItem(Item().withPrimaryKey("coords", key).withBinary("bytes", bytes))
    }
}