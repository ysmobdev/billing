package ua.syi.billing.library.models

class PurchaseParams internal constructor(
    val obfuscatedAccountId: String = "",
    val obfuscatedProfileId: String = "",
) {

    companion object {
        fun withUserId(id: String): PurchaseParams {
            return PurchaseParams(obfuscatedAccountId = id, obfuscatedProfileId = id)
        }
    }

}