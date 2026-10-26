package io.zenandroid.onlinego.data.billing

data class SupporterProduct(val id: String, val formattedPrice: String)

data class SupporterPurchase(
  val productIds: List<String>,
  val autoRenewing: Boolean,
  val token: String,
)

interface SupporterStore {
  suspend fun queryPurchases(): List<SupporterPurchase>
  suspend fun queryAvailableSubscriptions(): List<SupporterProduct>
}
