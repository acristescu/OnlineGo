package io.zenandroid.onlinego.data.billing

internal object UnavailableSupporterStore : SupporterStore {
  private const val MESSAGE = "Subscriptions are not available on iOS yet"

  override suspend fun queryPurchases(): List<SupporterPurchase> =
    throw UnsupportedOperationException(MESSAGE)

  override suspend fun queryAvailableSubscriptions(): List<SupporterProduct> =
    throw UnsupportedOperationException(MESSAGE)
}
