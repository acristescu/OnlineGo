package io.zenandroid.onlinego.ui.composables

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.componentsFromLocaleIdentifier
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localeIdentifierFromComponents
import platform.Foundation.preferredLanguages

internal fun appLocale(): NSLocale {
  val current = NSLocale.currentLocale
  val preferred = NSLocale.preferredLanguages.firstOrNull() as? String ?: return current
  if (NSLocale(preferred).languageCode == current.languageCode) return current
  val components = NSLocale.componentsFromLocaleIdentifier(preferred).toMutableMap()
  current.countryCode?.let { components[NSLocaleCountryCode] = it }
  return NSLocale(NSLocale.localeIdentifierFromComponents(components))
}
