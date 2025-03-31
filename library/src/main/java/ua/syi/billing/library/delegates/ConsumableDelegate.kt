package ua.syi.billing.library.delegates

import android.app.Activity
import ua.syi.billing.library.models.PlayMarketConsumable
import ua.syi.billing.library.models.PlayMarketConsumablePurchase
import ua.syi.billing.library.models.PurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.syi.billing.library.models.ActiveConsumable

internal class ConsumableDelegate(client: BillingClient) : PlayMarketDelegate(client) {

    /**
     * Purchase
     */

    fun purchase(activity: Activity, item: PlayMarketConsumable, params: PurchaseParams) {
        super.purchase(activity, item.payload, null, params)
    }

    suspend fun consumePurchase(item: PlayMarketConsumablePurchase): Boolean {
        return consumePurchaseToken(item.purchaseToken)
    }

    suspend fun consumePurchaseToken(purchaseToken: String): Boolean {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        val result = withContext(Dispatchers.IO) {
            client.consumePurchase(consumeParams).billingResult
        }
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * Fetch
     */

    suspend fun getAvailableConsumables(ids: Set<String>): List<PlayMarketConsumable> {
        return getConsumables(ids)
    }

    suspend fun getActiveConsumables(): List<ActiveConsumable> {
        return client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                .build()
        ).purchasesList.map { item ->
            ActiveConsumable(
                productId = item.products.first(),
                purchaseToken = item.purchaseToken,
                isAcknowledged = item.isAcknowledged,
            )
        }
    }

    private suspend fun getConsumables(ids: Set<String>): List<PlayMarketConsumable> {
        if (ids.isEmpty()) return emptyList()
        return getProductDetails(BillingClient.ProductType.INAPP, ids).map { details ->
            details.toConsumable()
        }
    }

    /**
     * Mapper
     */

    private fun ProductDetails.toConsumable(): PlayMarketConsumable {
        val subDetailOffer = oneTimePurchaseOfferDetails!!
        return PlayMarketConsumable(
            id = productId,
            formattedPrice = subDetailOffer.formattedPrice,
            payload = this,
            price = subDetailOffer.priceAmountMicros / 1_000_000.0,
            currency = subDetailOffer.priceCurrencyCode,
        )
    }

}