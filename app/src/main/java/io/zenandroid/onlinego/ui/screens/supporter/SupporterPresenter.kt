package io.zenandroid.onlinego.ui.screens.supporter

import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.playstore.PlayStoreService
import io.zenandroid.onlinego.utils.addToDisposable

class SupporterPresenter(
        private val view: SupporterContract.View,
        private val playStore: PlayStoreService
) : SupporterContract.Presenter {

    private val disposables = CompositeDisposable()
    private var products: List<ProductDetails>? = null
    private var purchases: List<Purchase>? = null
    private var state = State(
            loading = true
    )

    override fun subscribe() {
        playStore.queryPurchases()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onPurchasesFetched, this::onError)
                .addToDisposable(disposables)
        playStore.queryAvailableSubscriptions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onAvailableSubscriptionsFetched, this::onError)
                .addToDisposable(disposables)

        view.renderState(state)
    }

    private fun onPurchasesFetched(purchases: List<Purchase>) {
        this.purchases = purchases

        if(this.purchases != null && this.products != null) {
            buildItemList()
        }
    }

    override fun onSubscribeClick() {
        state.selectedTier?.let {
            state.products?.get(it)?.let {
                playStore.launchBillingFlow((view as Fragment).requireActivity(), it, state.purchase)
                        .subscribe({}, this::onError)
                        .addToDisposable(disposables)

                state = state.copy(loading = true)
                view.renderState(state)
            }
        }
    }

    override fun unsubscribe() {
        disposables.clear()
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

                state = state.copy(
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
                view.renderState(state)
            }
        }
    }

    override fun onUserDragSlider(value: Float) {
        val subscribeButtonEnabled =
                if (state.currentPurchaseDetails == null) true
                else value.toInt() != state.products?.indexOf(state.currentPurchaseDetails!!)

        state = state.copy(
                selectedTier = value.toInt(),
                subscribeButtonEnabled = subscribeButtonEnabled
        )

        view.renderState(state)
    }

    private fun getBasePrice(productId: String) =
            productId.subSequence("supporter_".length, productId.length).toString().toInt()

    private fun onError(t: Throwable) {
        view.showError(t)
    }

}

data class State(
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