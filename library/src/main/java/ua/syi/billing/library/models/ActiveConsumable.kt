package ua.syi.billing.library.models

class ActiveConsumable(
    val productId: String,
    val purchaseToken: String,
    val isAcknowledged: Boolean,
)