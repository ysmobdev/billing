package ua.syi.billing.library.models

class ActiveSubscription(
    val productId: String,
    val formattedPrice: String,
    val purchaseToken: String,
    val renewingAt: Long,
    val purchasedAt: Long,
) {

    override fun toString(): String {
        return "ActiveSubscription(productId='$productId', formattedPrice='$formattedPrice', renewingAt=$renewingAt, purchasedAt=$purchasedAt)"
    }

}