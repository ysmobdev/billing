package ua.syi.billing.library.models

import com.android.billingclient.api.Purchase

class PlayMarketConsumablePurchase(purchase: Purchase) : PlayMarketPurchase(purchase) {

    override fun toString(): String {
        return "PlayMarketConsumablePurchase(id='${purchase.products.joinToString()}')"
    }
}