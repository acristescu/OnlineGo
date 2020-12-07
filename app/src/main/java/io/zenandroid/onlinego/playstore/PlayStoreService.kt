package io.zenandroid.onlinego.playstore

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingFlowParams.ProrationMode.IMMEDIATE_WITHOUT_PRORATION
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class PlayStoreService(
        private val context: Context
) {
    private var connectionInProgress = false
    private val connectionSubject = PublishSubject.create<Unit>()
    private val possibleSubscriptions = (1..25).map { "supporter_$it" }

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

    fun connect(): Completable {
        return when {
            billingClient.isReady -> Completable.complete()
            connectionInProgress -> connectionSubject.firstOrError().ignoreElement()
            else -> Completable.create { emitter ->
                connectionInProgress = true
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        connectionInProgress = false
                        if (billingResult.responseCode == BillingResponseCode.OK) {
                            Log.e("PlayStoreService", "Setup Billing Done")
                            FirebaseCrashlytics.getInstance().log("Setup Billing Done")
                            emitter.onComplete()
                            connectionSubject.onNext(Unit)
                        } else {
                            emitter.onError(Exception("${billingResult.responseCode}:${billingResult.debugMessage}"))
                            connectionSubject.onError(Exception("${billingResult.responseCode}:${billingResult.debugMessage}"))
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        connectionInProgress = false
                        Log.e("PlayStoreService", "Billing client Disconnected")
                        FirebaseCrashlytics.getInstance().log("Billing client Disconnected")
                    }
                })
            }
        }
    }

    fun queryAvailableSubscriptions() = connect().andThen(
            Single.create<List<SkuDetails>> {
                val params = SkuDetailsParams.newBuilder()
                        .setSkusList(possibleSubscriptions)
                        .setType(BillingClient.SkuType.SUBS)

                billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
                    if (billingResult.responseCode == BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                        it.onSuccess(skuDetailsList)
                    } else {
                        it.onError(Exception("${billingResult.responseCode}:${billingResult.debugMessage}"))
                    }
                }
            }
    )

    fun queryPurchases() = connect().andThen(
            Single.create<List<Purchase>> {
                val billingResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
                if (billingResult.responseCode == BillingResponseCode.OK && billingResult.purchasesList != null) {
                    it.onSuccess(billingResult.purchasesList!!)
                } else {
                    it.onError(Exception("${billingResult.responseCode}:"))
                }
            }
    )

    fun launchBillingFlow(activity: Activity, skuDetails: SkuDetails, oldSkuPurchase: Purchase?) =
        connect().andThen(
                Completable.create {
                    val params = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .apply {
                                if(oldSkuPurchase != null) {
                                    setOldSku(oldSkuPurchase.sku, oldSkuPurchase.purchaseToken)
                                    setReplaceSkusProrationMode(IMMEDIATE_WITHOUT_PRORATION)
                                }
                            }
                            .build()
                    val billingResult = billingClient.launchBillingFlow(activity, params)
                    if(billingResult.responseCode != BillingResponseCode.OK) {
                        it.onError(Exception("${billingResult.responseCode}:${billingResult.debugMessage}"))
                    } else {
                        it.onComplete()
                    }
                }
        )

}