package ua.syi.billing.library.models

import com.android.billingclient.api.ProductDetails

class PlayMarketSubscription internal constructor(
    val id: String,
    val period: Period,
    val price: Long,
    val formattedPrice: String,
    val formattedBasePrice: String?,
    val introductoryFormattedPrice: String?,
    val introductoryPriceDays: Int,
    val discount: Int,

    internal val payload: ProductDetails,
    internal val offerToken: String?,
): Purchasable {

    internal fun copy(
        discount: Int? = null,
        price: Long? = null,
        formattedPrice: String? = null,
        formattedBasePrice: String? = null
    ): PlayMarketSubscription {
        return PlayMarketSubscription(
            id = id,
            period = period,
            price = price ?: this.price,
            formattedPrice = formattedPrice ?: this.formattedPrice,
            formattedBasePrice = formattedBasePrice ?: this.formattedBasePrice,
            introductoryFormattedPrice = introductoryFormattedPrice,
            introductoryPriceDays = introductoryPriceDays,
            discount = discount ?: this.discount,
            payload = payload,
            offerToken = offerToken,
        )
    }

    override fun toString(): String {
        return "Subscription(id='$id', period=$period, formattedPrice='$formattedPrice')"
    }

    enum class Period(val value: String) {
        Monthly("P1M"),
        Annual("P1Y"),
    }

}