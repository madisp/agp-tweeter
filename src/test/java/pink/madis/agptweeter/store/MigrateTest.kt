package pink.madis.agptweeter.store

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import pink.madis.agptweeter.moshi
import java.lang.IllegalStateException

private fun Store.readOnly() = object : Store {
  override fun read(key: String) = this@readOnly.read(key)

  override fun write(key: String, value: String) {
    throw IllegalStateException("Not supposed to write here")
  }
}

class MigrateTest {
  private val store = MemStore()

  @Test
  fun migrateEmpty() {
    migrate(store, moshi)

    assertThat(store.read("db_version")).isEqualTo("2")
    assertThat(store.read("db_keys")).isNotEmpty()
  }

  @Test
  fun migrateTwice() {
    migrate(store, moshi)
    // second migration is completely read-only - nothing to migrate
    migrate(store.readOnly(), moshi)
  }
}