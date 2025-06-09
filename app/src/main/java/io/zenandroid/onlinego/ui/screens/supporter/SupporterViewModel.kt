package io.zenandroid.onlinego.ui.screens.supporter

import android.app.Activity
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.playstore.PlayStoreService
import io.zenandroid.onlinego.utils.addToDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SupporterViewModel(
    private val playStore: PlayStoreService
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private var products: List<ProductDetails>? = null
    private var purchases: List<Purchase>? = null
    
    private val _state = MutableStateFlow(
        SupporterState(loading = true)
    )
    val state: StateFlow<SupporterState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SupporterEvent>()
    val events: SharedFlow<SupporterEvent> = _events.asSharedFlow()

    fun onViewResumed() {
        playStore.queryPurchases()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onPurchasesFetched, this::onError)
            .addToDisposable(disposables)
        playStore.queryAvailableSubscriptions()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onAvailableSubscriptionsFetched, this::onError)
            .addToDisposable(disposables)
    }

    fun onViewPaused() {
        disposables.clear()
    }

    private fun onPurchasesFetched(purchases: List<Purchase>) {
        this.purchases = purchases

        if(this.purchases != null && this.products != null) {
            buildItemList()
        }
    }

    fun onSubscribeClick(activity: Activity) {
        val currentState = _state.value
        currentState.selectedTier?.let { tierIndex ->
            currentState.products?.get(tierIndex)?.let { product ->
                playStore.launchBillingFlow(activity, product, currentState.purchase)
                    .subscribe({}, this::onError)
                    .addToDisposable(disposables)

                _state.value = currentState.copy(loading = true)
            }
        }
    }

    private fun onAvailableSubscriptionsFetched(products: List<ProductDetails>) {
        this.products = products
            .filter { it.productId.startsWith("supporter_") }
            .sortedBy { getBasePrice(it.productId) }
        if(this.purchases != null && this.products != null) {
            buildItemList()
        }
    }

    private fun buildItemList() {
        purchases?.let { purchases ->
            products?.let { prods ->
                val purchase = purchases.firstOrNull { it.products[0].startsWith("supporter_") && it.isAutoRenewing }
                val isSupporter = purchase != null
                val currentPurchaseDetails = purchase?.run {
                    prods.firstOrNull { purchase.products.contains(it.productId) }
                }
                val supporterLabelText = currentPurchaseDetails?.let {
                    "Your current contribution is <b>${it.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice}</b>"
                }
                val selectedTier = if(currentPurchaseDetails != null) {
                    prods.indexOf(currentPurchaseDetails)
                } else {
                    prods.size / 2
                }

                val subscribeTitleText = if(isSupporter) {
                    buildSpannedString {
                        bold { append("Thank you for your support!\n\n") }
                        append("Your current contribution is ")
                        bold { append(currentPurchaseDetails?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice) }
                    }
                } else {
                    buildSpannedString {
                        bold { append("Select your monthly contribution") }
                    }
                }

                val subscribeButtonText = if(isSupporter) "Update amount" else "Become a supporter"

                val subscribeButtonEnabled =
                    if (!isSupporter) true
                    else selectedTier != prods.indexOf(currentPurchaseDetails)

                _state.value = _state.value.copy(
                    loading = false,
                    supporter = isSupporter,
                    currentPurchaseDetails = currentPurchaseDetails,
                    products = prods,
                    numberOfTiers = prods.size,
                    selectedTier = selectedTier,
                    supporterLabelText = supporterLabelText,
                    subscribeTitleText = subscribeTitleText,
                    subscribeButtonText = subscribeButtonText,
                    subscribeButtonEnabled = subscribeButtonEnabled,
                    purchase = purchase
                )
            }
        }
    }

    fun onUserDragSlider(value: Float) {
        val currentState = _state.value
        val subscribeButtonEnabled =
            if (currentState.currentPurchaseDetails == null) true
            else value.toInt() != currentState.products?.indexOf(currentState.currentPurchaseDetails!!)

        _state.value = currentState.copy(
            selectedTier = value.toInt(),
            subscribeButtonEnabled = subscribeButtonEnabled
        )
    }

    private fun getBasePrice(productId: String) =
        productId.subSequence("supporter_".length, productId.length).toString().toInt()

    private fun onError(t: Throwable) {
        viewModelScope.launch {
            _events.emit(SupporterEvent.ShowError(t))
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

data class SupporterState(
    val loading: Boolean = false,
    val supporter: Boolean = false,
    val numberOfTiers: Int? = null,
    val selectedTier: Int? = null,
    val products: List<ProductDetails>? = null,
    val currentPurchaseDetails: ProductDetails? = null,
    val supporterLabelText: String? = null,
    val subscribeTitleText: CharSequence? = null,
    val subscribeButtonText: String? = null,
    val subscribeButtonEnabled: Boolean = false,
    val purchase: Purchase? = null
)

sealed class SupporterEvent {
    data class ShowError(val throwable: Throwable) : SupporterEvent()
} 