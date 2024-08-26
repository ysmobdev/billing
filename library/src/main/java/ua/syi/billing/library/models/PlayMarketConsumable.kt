package ua.syi.billing.library.models

import com.android.billingclient.api.ProductDetails

class PlayMarketConsumable(
    val id: String,
    val formattedPrice: String,
    val price: Double,
    val currency: String,
    internal val payload: ProductDetails,
) : Purchasable {

    override fun toString(): String {
        return "PlayMarketConsumable(id='$id', formattedPrice='$formattedPrice')"
    }

}