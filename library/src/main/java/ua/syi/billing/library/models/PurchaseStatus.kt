package ua.syi.billing.library.models

sealed interface PurchaseStatus {
    object Pending : PurchaseStatus
    class ConfirmationRequired(val purchase: PlayMarketPurchase) : PurchaseStatus
    object PurchaseConfirmed : PurchaseStatus
    object Canceled : PurchaseStatus
    class Error(val code: Int, val message: String) : PurchaseStatus
}
