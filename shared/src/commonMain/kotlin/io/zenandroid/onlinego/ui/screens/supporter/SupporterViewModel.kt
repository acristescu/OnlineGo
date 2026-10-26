package io.zenandroid.onlinego.ui.screens.supporter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.zenandroid.onlinego.data.billing.SupporterProduct
import io.zenandroid.onlinego.data.billing.SupporterPurchase
import io.zenandroid.onlinego.data.billing.SupporterStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SupporterViewModel(
  private val store: SupporterStore
) : ViewModel() {

  private val _state = MutableStateFlow(
    SupporterState(loading = true)
  )
  val state: StateFlow<SupporterState> = _state.asStateFlow()

  private val _events = MutableSharedFlow<SupporterEvent>()
  val events: SharedFlow<SupporterEvent> = _events.asSharedFlow()

  fun onResume() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val purchases = store.queryPurchases()
        val products = store.queryAvailableSubscriptions()
          .filter { it.id.startsWith("supporter_") }
          .sortedBy { getBasePrice(it.id) }
        buildItemList(products, purchases)
      } catch (e: Exception) {
        onError(e)
      }
    }
  }


  fun onSubscribeClick() {
    val currentState = _state.value
    currentState.selectedTier?.let { tierIndex ->
      currentState.products?.get(tierIndex)?.let { product ->
        viewModelScope.launch {
          _events.emit(SupporterEvent.LaunchBillingFlow(product, currentState.purchase))
        }
        _state.value = currentState.copy(loading = true)
      }
    }
  }


  private fun buildItemList(
    products: List<SupporterProduct>,
    purchases: List<SupporterPurchase>,
  ) {
    val purchase =
      purchases.firstOrNull { it.productIds[0].startsWith("supporter_") && it.autoRenewing }
    val isSupporter = purchase != null
    val currentPurchaseDetails = purchase?.run {
      products.firstOrNull { purchase.productIds.contains(it.id) }
    }
    val selectedTier = if (currentPurchaseDetails != null) {
      products.indexOf(currentPurchaseDetails)
    } else {
      products.size / 2
    }

    _state.value = SupporterState(
      loading = false,
      supporter = isSupporter,
      numberOfTiers = products.size,
      selectedTier = selectedTier,
      sliderValue = selectedTier.toFloat(),
      products = products,
      currentPurchaseDetails = currentPurchaseDetails,
      subscribeButtonEnabled = if (!isSupporter) true else selectedTier != products.indexOf(
        currentPurchaseDetails
      ),
      purchase = purchase
    )
  }

  fun onUserDragSlider(value: Float) {
    val currentState = _state.value
    val newSelectedTier = value.roundToInt().coerceIn(0, (currentState.numberOfTiers ?: 1) - 1)
    val subscribeButtonEnabled =
      if (currentState.currentPurchaseDetails == null) true
      else newSelectedTier != currentState.products?.indexOf(currentState.currentPurchaseDetails)

    _state.value = currentState.copy(
      selectedTier = newSelectedTier,
      sliderValue = value,
      subscribeButtonEnabled = subscribeButtonEnabled
    )
  }

  private fun getBasePrice(productId: String) =
    productId.subSequence("supporter_".length, productId.length).toString().toInt()

  fun onError(t: Throwable) {
    viewModelScope.launch {
      _events.emit(SupporterEvent.ShowError(t))
    }
  }

}

data class SupporterState(
  val loading: Boolean = false,
  val supporter: Boolean = false,
  val numberOfTiers: Int? = null,
  val selectedTier: Int? = null,
  val sliderValue: Float? = null,
  val products: List<SupporterProduct>? = null,
  val currentPurchaseDetails: SupporterProduct? = null,
  val subscribeButtonEnabled: Boolean = false,
  val purchase: SupporterPurchase? = null
) {
  val currentContributionAmount: String
    get() = currentPurchaseDetails?.formattedPrice ?: ""

  val selectedTierAmount: String
    get() = products?.getOrNull(selectedTier ?: 0)?.formattedPrice ?: ""

  val displaySliderValue: Float
    get() = sliderValue ?: selectedTier?.toFloat() ?: 0f
}

sealed class SupporterEvent {
  data class ShowError(val throwable: Throwable) : SupporterEvent()
  data class LaunchBillingFlow(val product: SupporterProduct, val oldPurchase: SupporterPurchase?) :
    SupporterEvent()
} 