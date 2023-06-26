package ua.syi.billing.library

import android.app.Activity
import android.content.Context
import ua.syi.billing.library.delegates.ConsumableDelegate
import ua.syi.billing.library.delegates.SubscriptionDelegate
import ua.syi.billing.library.models.ActiveSubscription
import ua.syi.billing.library.models.PlayMarketConsumable
import ua.syi.billing.library.models.PlayMarketConsumablePurchase
import ua.syi.billing.library.models.PlayMarketPurchase
import ua.syi.billing.library.models.PlayMarketSubscription
import ua.syi.billing.library.models.PlayMarketSubscriptionPurchase
import ua.syi.billing.library.models.Purchasable
import ua.syi.billing.library.models.PurchaseParams
import ua.syi.billing.library.models.PurchaseStatus
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

typealias Log = (String) -> Unit

class BillingServiceImpl(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val log: Log
) : BillingService {

    private val _purchaseStatus = MutableSharedFlow<PurchaseStatus>()
    override val purchaseStatus: SharedFlow<PurchaseStatus>
        get() = _purchaseStatus.asSharedFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(PurchasesUpdatedListener(_purchaseStatus, coroutineScope, log))
        .enablePendingPurchases()
        .build()

    private val subscriptionDelegate = SubscriptionDelegate(billingClient)
    private val consumableDelegate = ConsumableDelegate(billingClient)

    private suspend fun startConnection() = suspendCancellableCoroutine<Unit> { continuation ->
        var isResumed = false
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                log("Billing client has been disconnected")
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (isResumed) return
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    log("Billing client has been connected")
                    continuation.resume(Unit)
                } else if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    log("Billing client received error: [${billingResult.responseCode}] ${billingResult.debugMessage}")
                    continuation.resumeWithException(IOException("Billing error code ${billingResult.responseCode}"))
                }
                isResumed = true
            }
        })
    }

    private suspend fun checkConnection() {
        if (billingClient.isReady.not()) {
            startConnection()
        }
    }

    override suspend fun getAvailableSubscriptions(
        ids: Set<String>,
        discountIds: Set<String>
    ): List<PlayMarketSubscription> {
        Utils.retryIO { checkConnection() }
        log("Request available subscriptions: ${ids.joinToString()} / ${discountIds.joinToString()}")
        return subscriptionDelegate.getAvailableSubscriptions(ids, discountIds)
    }

    override suspend fun getActiveSubscription(): ActiveSubscription? {
        Utils.retryIO { checkConnection() }
        return subscriptionDelegate.getActiveSubscription().also { item ->
            log("Request active subscription: $item")
        }
    }

    override suspend fun getAvailableConsumables(ids: Set<String>): List<PlayMarketConsumable> {
        Utils.retryIO { checkConnection() }
        log("Request available consumables: ${ids.joinToString()}")
        return consumableDelegate.getAvailableConsumables(ids)
    }

    override suspend fun purchase(
        activity: Activity,
        item: Purchasable,
        params: PurchaseParams
    ) {
        Utils.retryIO { checkConnection() }
        log("Purchase: $item")
        return when (item) {
            is PlayMarketSubscription -> subscriptionDelegate.purchase(activity, item, params)
            is PlayMarketConsumable -> consumableDelegate.purchase(activity, item, params)
            else -> throw IllegalArgumentException("Unsupported playMarket type: $item")
        }
    }

    override suspend fun confirmPurchase(item: PlayMarketPurchase): Boolean {
        Utils.retryIO { checkConnection() }
        log("Confirm purchase: $item")
        return when (item) {
            is PlayMarketSubscriptionPurchase -> subscriptionDelegate.acknowledgePurchase(item)
            is PlayMarketConsumablePurchase -> consumableDelegate.consumePurchase(item)
            else -> throw IllegalArgumentException("Unsupported playMarketPurchase: $item")
        }.also { isSuccess ->
            log("Confirm purchase result: $isSuccess")
        }
    }

}