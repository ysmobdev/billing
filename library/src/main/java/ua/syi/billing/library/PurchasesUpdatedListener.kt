package ua.syi.billing.library

import ua.syi.billing.library.models.PlayMarketConsumablePurchase
import ua.syi.billing.library.models.PlayMarketSubscriptionPurchase
import ua.syi.billing.library.models.PurchaseStatus
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch


internal class PurchasesUpdatedListener(
    private val statusFlow: MutableSharedFlow<PurchaseStatus>,
    private val coroutineScope: CoroutineScope,
    private val log: Log,
) : com.android.billingclient.api.PurchasesUpdatedListener {

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            emitStatus(PurchaseStatus.Canceled)
        } else {
            emitStatus(PurchaseStatus.Error(billingResult.responseCode, billingResult.debugMessage))
        }
    }

    private fun emitStatus(status: PurchaseStatus) {
        coroutineScope.launch {
            statusFlow.emit(status)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        val state = purchase.purchaseState
        log("Handle purchase: ${stateCodeToString(state)} - ${purchase.products.joinToString()}")
        if (state != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            val playMarketPurchase = if (purchase.isAutoRenewing) {
                PlayMarketSubscriptionPurchase(purchase)
            } else {
                PlayMarketConsumablePurchase(purchase)
            }
            emitStatus(PurchaseStatus.ConfirmationRequired(playMarketPurchase))
        }
        if (purchase.isAcknowledged) {
            emitStatus(PurchaseStatus.PurchaseConfirmed)
        }
    }

    private fun stateCodeToString(code: Int): String {
        return when (code) {
            0 -> "UNSPECIFIED_STATE"
            1 -> "PURCHASED"
            2 -> "PENDING"
            else -> "UNKNOWN"
        }
    }

}