package ua.syi.billing.library

import android.app.Activity
import ua.syi.billing.library.models.ActiveSubscription
import ua.syi.billing.library.models.PlayMarketConsumable
import ua.syi.billing.library.models.PlayMarketPurchase
import ua.syi.billing.library.models.PlayMarketSubscription
import ua.syi.billing.library.models.Purchasable
import ua.syi.billing.library.models.PurchaseParams
import ua.syi.billing.library.models.PurchaseStatus
import kotlinx.coroutines.flow.SharedFlow
import ua.syi.billing.library.models.ActiveConsumable
import ua.syi.billing.library.models.Type

interface BillingService {

    val purchaseStatus: SharedFlow<PurchaseStatus>

    suspend fun getAvailableSubscriptions(
        ids: Set<String>,
        discountIds: Set<String> = emptySet()
    ): List<PlayMarketSubscription>

    suspend fun getActiveSubscription(): ActiveSubscription?

    suspend fun getAvailableConsumables(
        ids: Set<String>
    ): List<PlayMarketConsumable>

    suspend fun getActiveConsumables(): List<ActiveConsumable>

    suspend fun purchase(activity: Activity, item: Purchasable, params: PurchaseParams)

    suspend fun confirmPurchase(item: PlayMarketPurchase): Boolean

    suspend fun confirmPurchaseToken(purchaseToken: String, type: Type): Boolean

}