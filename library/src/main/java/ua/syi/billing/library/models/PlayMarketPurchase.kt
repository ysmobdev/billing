package ua.syi.billing.library.models

import com.android.billingclient.api.Purchase

abstract class PlayMarketPurchase internal constructor(
    val purchase: Purchase,
) {

    val purchaseToken: String
        get() = purchase.purchaseToken

    val productId: String
        get() = purchase.products.joinToString(",")

}