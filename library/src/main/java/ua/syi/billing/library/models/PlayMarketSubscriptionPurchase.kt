package ua.syi.billing.library.models

import com.android.billingclient.api.Purchase

class PlayMarketSubscriptionPurchase internal constructor(purchase: Purchase) :
    PlayMarketPurchase(purchase) {

    override fun toString(): String {
        return "PlayMarketSubscriptionPurchase(id='${purchase.products.joinToString()}')"
    }
}