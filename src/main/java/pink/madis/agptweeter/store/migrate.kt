package pink.madis.agptweeter.store

import com.squareup.moshi.Moshi
import pink.madis.agptweeter.ArtifactConfig
import java.lang.IllegalStateException
import java.time.Instant

const val DB_VERSION: Int = 2

private var Store.version: Int
  get()      = Integer.parseInt(read("db_version") ?: "1")
  set(value) = write("db_version", value.toString())

private var Store.keys: Set<String>
  // we either have the keys or we have to guess...
  get()      = read("db_keys")?.split(',')?.toSet() ?: ArtifactConfig.values().map { it.key }.toSet() + "key" /* tests */
  set(value) = write("db_keys", value.joinToString(separator = ","))

fun migrate(store: Store, moshi: Moshi): MigratedStore {
  println("Checking $store for migration...")
  val version = store.version

  if (version == DB_VERSION) {
    println("Nothing to migrate")
    return MigratedStore(store)
  }

  val keys = store.keys

  for (v in version until DB_VERSION) {
    upgrade(store, v, moshi, keys)
  }
  println("Migration of $store completed")
  return MigratedStore(store)
}

/**
 * Upgrade the db schema by 1
 */
private fun upgrade(store: Store, currentVersion: Int, moshi: Moshi, keys: Collection<String>) {
  println("Upgrading schema of $store: $currentVersion -> ${currentVersion+1}")
  when (currentVersion) {
    0    -> { /* initial version, nothing to do */ }
    1    -> {
      // store keys
      store.keys = keys.toSet()
      // add the pending field to keys
      val v1Adapter = moshi.adapter(Version1::class.java)
      val v2Adapter = moshi.adapter(Version2::class.java)
      keys.asSequence()
          .map { it to store.read(it)?.let(v1Adapter::fromJson) }
          .map { (k, v) -> if (v == null) null else (k to v) }
          .filterNotNull()
          .forEach { (k, value) -> store.write(k, v2Adapter.toJson(Version2(value))) }
    }
    else -> { throw IllegalStateException("Don't know how to migrate $store from $currentVersion") }
  }
  store.version = currentVersion + 1
}

class MigratedStore internal constructor(private val store: Store): Store by store {
  override fun write(key: String, value: String) {
    store.write(key, value)
    if (key != "db_keys") {
      store.keys = store.keys + key
    }
  }
}

private data class Version1(val versions: List<String>)
private data class PendingVersion2(val version: String, val seenAt: Instant)
private data class Version2(val versions: List<String>, val pending: List<PendingVersion2>) {
  constructor(v: Version1) : this(v.versions, emptyList())
}