package ua.syi.billing.library.delegates

import android.app.Activity
import ua.syi.billing.library.models.PlayMarketConsumable
import ua.syi.billing.library.models.PlayMarketConsumablePurchase
import ua.syi.billing.library.models.PurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.consumePurchase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ConsumableDelegate(client: BillingClient) : PlayMarketDelegate(client) {

    /**
     * Purchase
     */

    fun purchase(activity: Activity, item: PlayMarketConsumable, params: PurchaseParams) {
        super.purchase(activity, item.payload, null, params)
    }

    suspend fun consumePurchase(item: PlayMarketConsumablePurchase): Boolean {
        val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(item.purchase.purchaseToken)
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
        )
    }

}