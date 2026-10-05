package io.zenandroid.onlinego.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {

  @Test
  fun `every language round trips through its locale tag`() {
    AppLanguage.entries.forEach {
      assertEquals(it, AppLanguage.fromLocaleTag(it.localeTag))
    }
  }

  @Test
  fun `regional variants resolve to their language`() {
    assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLocaleTag("en-GB"))
    assertEquals(AppLanguage.ROMANIAN, AppLanguage.fromLocaleTag("ro-MD"))
    assertEquals(AppLanguage.SPANISH, AppLanguage.fromLocaleTag("es-419"))
  }

  @Test
  fun `untranslated or missing languages resolve to null`() {
    assertNull(AppLanguage.fromLocaleTag("fr-FR"))
    assertNull(AppLanguage.fromLocaleTag("und"))
    assertNull(AppLanguage.fromLocaleTag(""))
    assertNull(AppLanguage.fromLocaleTag(null))
  }
}
