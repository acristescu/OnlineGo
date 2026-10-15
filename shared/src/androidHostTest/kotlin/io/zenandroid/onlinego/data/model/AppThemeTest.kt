package io.zenandroid.onlinego.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeTest {

  @Test
  fun `persisted values stay the locale independent english keys`() {
    // These are what end up in the datastore. Changing them silently resets everybody's theme,
    // so they must not be derived from the displayed (translatable) label.
    assertEquals("System Default", AppTheme.SYSTEM_DEFAULT.storedValue)
    assertEquals("Light", AppTheme.LIGHT.storedValue)
    assertEquals("Dark", AppTheme.DARK.storedValue)
  }

  @Test
  fun `every theme round trips through its stored value`() {
    AppTheme.entries.forEach {
      assertEquals(it, AppTheme.fromStoredValue(it.storedValue))
    }
  }

  @Test
  fun `values persisted by older builds are still understood`() {
    // SettingsRepository used to default to "System default" while MainActivity compared against
    // "System Default", so both capitalisations are out there.
    assertEquals(AppTheme.SYSTEM_DEFAULT, AppTheme.fromStoredValue("System default"))
    assertEquals(AppTheme.SYSTEM_DEFAULT, AppTheme.fromStoredValue("System Default"))
    assertEquals(AppTheme.LIGHT, AppTheme.fromStoredValue("Light"))
    assertEquals(AppTheme.DARK, AppTheme.fromStoredValue("Dark"))
  }

  @Test
  fun `unknown and missing values fall back to the default`() {
    assertEquals(AppTheme.DEFAULT, AppTheme.fromStoredValue(null))
    assertEquals(AppTheme.DEFAULT, AppTheme.fromStoredValue(""))
    // A localized label must never be accepted as a stored value.
    assertEquals(AppTheme.DEFAULT, AppTheme.fromStoredValue("Clair"))
  }
}
