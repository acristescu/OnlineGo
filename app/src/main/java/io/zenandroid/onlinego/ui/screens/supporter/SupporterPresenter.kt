package io.zenandroid.onlinego.ui.screens.supporter

import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.fragment.app.Fragment
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import io.reactivex.disposables.CompositeDisposable
import io.zenandroid.onlinego.playstore.PlayStoreService
import io.zenandroid.onlinego.utils.addToDisposable

class SupporterPresenter(
        private val view: SupporterContract.View,
        private val playStore: PlayStoreService
) : SupporterContract.Presenter {

    private val disposables = CompositeDisposable()
    private var skus: List<SkuDetails>? = null
    private var purchases: List<Purchase>? = null
    private var state = State(
            loading = true
    )

    override fun subscribe() {
        playStore.queryPurchases()
                .subscribe(this::onPurchasesFetched, this::onError)
                .addToDisposable(disposables)
        playStore.queryAvailableSubscriptions()
                .subscribe(this::onAvailableSubscriptionsFetched, this::onError)
                .addToDisposable(disposables)

        view.renderState(state)
    }

    private fun onPurchasesFetched(purchases: List<Purchase>) {
        this.purchases = purchases

        if(this.purchases != null && this.skus != null) {
            buildItemList()
        }
    }

    override fun onSubscribeClick() {
        state.selectedTier?.let {
            state.skus?.get(it)?.let {
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

    private fun onAvailableSubscriptionsFetched(skus: List<SkuDetails>) {
        this.skus = skus
                .filter { it.sku.startsWith("supporter_") }
                .sortedBy { it.priceAmountMicros }
        if(this.purchases != null && this.skus != null) {
            buildItemList()
        }
    }

    private fun buildItemList() {
        purchases?.let { purchases ->
            skus?.let { skus ->
                val purchase = purchases.firstOrNull { it.sku.startsWith("supporter_") && it.isAutoRenewing }
                val isSupporter = purchase != null
                val currentPurchaseDetails = purchase?.run {
                    skus.firstOrNull { it.sku == purchase.sku }
                }
                val supporterLabelText = currentPurchaseDetails?.let {
                    "Your current contribution is <b>${it.price}</b>"
                }
                val supporterButtonText = purchase?.let {
                    "£${getBasePrice(it.sku)}"
                }
                val selectedTier = if(currentPurchaseDetails != null) {
                    skus.indexOf(currentPurchaseDetails)
                } else {
                    skus.size / 2
                }

                val subscribeTitleText = if(isSupporter) {
                    buildSpannedString {
                        bold { append("Thank you for your support!\n\n") }
                        append("Your current contribution is ")
                        bold { append(currentPurchaseDetails?.price) }
                    }
                } else {
                    buildSpannedString {
                        bold { append("Select your monthly contribution") }
                    }
                }

                val subscribeButtonText = if(isSupporter) "Update amount" else "Become a supporter"

                val subscribeButtonEnabled =
                        if (!isSupporter) true
                        else selectedTier != skus.indexOf(currentPurchaseDetails)

                state = state.copy(
                        loading = false,
                        supporter = isSupporter,
                        currentPurchaseDetails = currentPurchaseDetails,
                        skus = skus,
                        numberOfTiers = skus.size,
                        selectedTier = selectedTier,
                        supporterLabelText = supporterLabelText,
                        supporterButtonText = supporterButtonText,
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
                else value.toInt() != state.skus?.indexOf(state.currentPurchaseDetails!!)

        state = state.copy(
                selectedTier = value.toInt(),
                subscribeButtonEnabled = subscribeButtonEnabled
        )

        view.renderState(state)
    }

    private fun getBasePrice(sku: String) =
            sku.subSequence("supporter_".length, sku.length).toString().toInt()

    private fun onError(t: Throwable) {
        view.showError(t)
    }

}

data class State(
        val loading: Boolean = false,
        val supporter: Boolean = false,
        val numberOfTiers: Int? = null,
        val selectedTier: Int? = null,
        val skus: List<SkuDetails>? = null,
        val currentPurchaseDetails: SkuDetails? = null,
        val supporterLabelText: String? = null,
        val supporterButtonText: String? = null,
        val subscribeTitleText: CharSequence? = null,
        val subscribeButtonText: String? = null,
        val subscribeButtonEnabled: Boolean = false,
        val purchase: Purchase? = null
)