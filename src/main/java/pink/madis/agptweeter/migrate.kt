package pink.madis.agptweeter

import java.lang.IllegalStateException

const val DB_VERSION: Int = 1

private var Store.version: Int
  get()      = Integer.parseInt(read("db_version") ?: "1")
  set(value) = write("db_version", value.toString())

fun migrate(store: Store) {
  println("Checking $store for migration...")
  for (v in store.version + 1 .. DB_VERSION) {
    store.upgradeTo(v)
  }
  println("Migration of $store completed")
}

/**
 * Upgrade the db schema by 1
 */
private fun Store.upgradeTo(newVersion: Int) {
  println("Upgrading schema of $this: ${newVersion-1} -> $newVersion")
  when (newVersion) {
    1    -> { /* initial version, nothing to do */ }
    else -> { throw IllegalStateException("Don't know how to migrate $this to $newVersion") }
  }
  version = newVersion
}