package io.zenandroid.onlinego.playstore

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class PlayStoreService(
        private val context: Context
) {
    private val connectionMutex = Mutex()
    private val possibleSubscriptions = (1..25).map {
        QueryProductDetailsParams.Product.newBuilder()
            .setProductId("supporter_$it")
            .setProductType(ProductType.SUBS)
            .build()
    }

    private val billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                FirebaseCrashlytics.getInstance().log("billingResult: $billingResult purchases: $purchases")
                Log.v("PlayStoreService", "billingResult: $billingResult purchases: $purchases")
                if(billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
                    for(purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases()
            .build()

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                val billingResponseCode = billingResult.responseCode
                val billingDebugMessage = billingResult.debugMessage

                Log.v("PlayStoreService","response code: $billingResponseCode")
                Log.v("PlayStoreService","debugMessage : $billingDebugMessage")
                FirebaseCrashlytics.getInstance().log("response code: $billingResponseCode debugMessage : $billingDebugMessage")
            }
        }
    }

    suspend fun connect() {
        if (billingClient.isReady) return

        connectionMutex.withLock {
            // Double-check after acquiring lock (another caller may have connected while we waited)
            if (billingClient.isReady) return

            suspendCoroutine { continuation ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingResponseCode.OK) {
                            Log.e("PlayStoreService", "Setup Billing Done")
                            FirebaseCrashlytics.getInstance().log("Setup Billing Done")
                            continuation.resume(Unit)
                        } else {
                            continuation.resumeWithException(Exception("${billingResult.responseCode}:${billingResult.debugMessage}"))
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        Log.e("PlayStoreService", "Billing client Disconnected")
                        FirebaseCrashlytics.getInstance().log("Billing client Disconnected")
                    }
                })
            }
        }
    }

    suspend fun queryAvailableSubscriptions(): List<ProductDetails> {
        connect()
        return suspendCoroutine { continuation ->
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(possibleSubscriptions)

            billingClient.queryProductDetailsAsync(params.build()) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingResponseCode.OK && !productDetailsList.isNullOrEmpty()) {
                    continuation.resume(productDetailsList)
                } else {
                    continuation.resumeWithException(Exception("${billingResult.responseCode}:${billingResult.debugMessage}"))
                }
            }
        }
    }

    suspend fun queryPurchases(): List<Purchase> {
        connect()
        return suspendCoroutine { continuation ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(ProductType.SUBS)
                    .build()
            ) { billingResult, purchaseList ->
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    continuation.resume(purchaseList)
                } else {
                    continuation.resumeWithException(Exception("${billingResult.responseCode}:"))
                }
            }
        }
    }

    suspend fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        oldProductPurchase: Purchase?
    ) {
        connect()
        val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken
            ?: throw Exception("Cannot get offerToken")
        val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList).apply {
                if (oldProductPurchase != null) {
                    setSubscriptionUpdateParams(
                        SubscriptionUpdateParams.newBuilder()
                            .setOldPurchaseToken(oldProductPurchase.purchaseToken)
                            .setSubscriptionReplacementMode(SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE)
                            .build()
                    )
                }
            }.build()
        val billingResult = billingClient.launchBillingFlow(activity, params)
        if (billingResult.responseCode != BillingResponseCode.OK) {
            throw Exception("${billingResult.responseCode}:${billingResult.debugMessage}")
        }
    }
}