package ua.syi.billing.library.delegates

import android.app.Activity
import ua.syi.billing.library.Utils
import ua.syi.billing.library.models.ActiveSubscription
import ua.syi.billing.library.models.PlayMarketPurchase
import ua.syi.billing.library.models.PlayMarketSubscription
import ua.syi.billing.library.models.PurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

internal class SubscriptionDelegate(
    client: BillingClient
) : PlayMarketDelegate(client) {

    /**
     * Purchase
     */

    fun purchase(activity: Activity, item: PlayMarketSubscription, params: PurchaseParams) {
        super.purchase(activity, item.payload, item.offerToken, params)
    }

    suspend fun acknowledgePurchase(item: PlayMarketPurchase): Boolean {
        if (item.purchase.isAcknowledged) return true
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(item.purchase.purchaseToken)
        val result = client.acknowledgePurchase(acknowledgePurchaseParams.build())
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * Fetch
     */

    suspend fun getActiveSubscription(): ActiveSubscription? {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)

        val purchasesResult = client.queryPurchasesAsync(params.build())

//        val purchaseHistoryResult = client.queryPurchaseHistory(params.build())
//        val purchases = purchaseHistoryResult.purchaseHistoryRecordList
        val purchases = purchasesResult.purchasesList
        val purchase = purchases.maxByOrNull { it.purchaseTime } ?: return null
        val subscriptions = getSubscriptions(purchase.products.toSet())
        val sub = subscriptions.first { sub -> purchase.products.any { p -> p == sub.id } }

        val nextBillingAt = Instant.fromEpochMilliseconds(purchase.purchaseTime)
            .plus(Utils.extractDuration(sub.period.value, Utils.MONTH_REGEX), DateTimeUnit.MONTH, TimeZone.UTC)
            .plus(Utils.extractDuration(sub.period.value, Utils.YEAR_REGEX), DateTimeUnit.YEAR, TimeZone.UTC)
            .toEpochMilliseconds()
        return ActiveSubscription(
            productId = sub.id,
            formattedPrice = sub.formattedPrice,
            purchaseToken = purchase.purchaseToken,
            renewingAt = nextBillingAt,
            purchasedAt = purchase.purchaseTime,
        )
    }

    suspend fun getAvailableSubscriptions(
        ids: Set<String>,
        discountIds: Set<String>
    ): List<PlayMarketSubscription> {
        val baseSubscriptions = getSubscriptions(ids)
        val discountSubscriptions = getSubscriptions(discountIds)
        val discounts = calculateDiscounts(baseSubscriptions, discountSubscriptions)
        return applyDiscounts(baseSubscriptions, discountSubscriptions, discounts)
    }

    private suspend fun getSubscriptions(ids: Set<String>): List<PlayMarketSubscription> {
        if (ids.isEmpty()) return emptyList()
        return getProductDetails(BillingClient.ProductType.SUBS, ids).map { details ->
            details.toSubscription()
        }
    }

    private fun calculateDiscounts(
        baseSubscriptions: List<PlayMarketSubscription>,
        discountSubscriptions: List<PlayMarketSubscription>
    ): Map<PlayMarketSubscription.Period, Int> {
        val annualDiscountSub =
            discountSubscriptions.firstOrNull { it.period == PlayMarketSubscription.Period.Annual }
        val monthlyDiscountSub =
            discountSubscriptions.firstOrNull { it.period == PlayMarketSubscription.Period.Monthly }
        val annualBaseSub =
            baseSubscriptions.firstOrNull { it.period == PlayMarketSubscription.Period.Annual }
        val monthlyBaseSub =
            baseSubscriptions.firstOrNull { it.period == PlayMarketSubscription.Period.Monthly }

        if (annualBaseSub == null || monthlyBaseSub == null) {
            return emptyMap()
        }

        //Если у нас есть скидочная предложение то подсчет делаем относительно главной цены
        val annualDiscount = if (annualDiscountSub != null) {
            annualDiscountSub.price / annualBaseSub.price.toDouble() * 100.0 - 100
        } else {
            annualBaseSub.price / 12.0 / monthlyBaseSub.price.toDouble() * 100.0 - 100
        }

        //Отображение скидки на месяц отображается только при наличии спец предложенияй
        val monthlyDiscount = if (monthlyDiscountSub != null) {
            monthlyDiscountSub.price / monthlyBaseSub.price.toDouble() * 100.0 - 100
        } else {
            0.0
        }

        return mapOf(
            PlayMarketSubscription.Period.Monthly to monthlyDiscount.toInt(),
            PlayMarketSubscription.Period.Annual to annualDiscount.toInt(),
        )
    }

    private fun applyDiscounts(
        subscriptions: List<PlayMarketSubscription>,
        discountSubscriptions: List<PlayMarketSubscription>,
        discounts: Map<PlayMarketSubscription.Period, Int>
    ): List<PlayMarketSubscription> {
        return subscriptions.map { sub ->
            val discountSub = discountSubscriptions.firstOrNull { it.period == sub.period }
            sub.copy(
                discount = discounts.getOrElse(sub.period) { 0 },
                price = discountSub?.price ?: sub.price,
                formattedPrice = discountSub?.formattedPrice ?: sub.formattedPrice,
                formattedBasePrice = discountSub?.let { sub.formattedPrice }
            )
        }
    }

    /**
     * Mapper
     */

    private fun ProductDetails.toSubscription(): PlayMarketSubscription {
        val subDetailOffer =
            subscriptionOfferDetails!!.firstOrNull { it.pricingPhases.pricingPhaseList.size > 1 }
                ?: subscriptionOfferDetails!!.first()
        val billingPeriod = subDetailOffer.pricingPhases.pricingPhaseList.last().billingPeriod
        val mainPricing = subDetailOffer.pricingPhases.pricingPhaseList.first {
            it.billingPeriod == billingPeriod && it.billingCycleCount == 0
        }
        val introductoryPrice =
            subDetailOffer.pricingPhases.pricingPhaseList.firstOrNull { it.billingCycleCount == 1 }
        val period = PlayMarketSubscription.Period.values()
            .first { p -> p.value.equals(billingPeriod, true) }
        return PlayMarketSubscription(
            id = productId,
            period = period,
            price = mainPricing.priceAmountMicros / 1_000_000,
            introductoryFormattedPrice = introductoryPrice?.formattedPrice,
            introductoryPriceDays = introductoryPrice?.billingPeriod?.let { extractDays(it) } ?: 0,
            formattedPrice = mainPricing.formattedPrice,
            formattedBasePrice = null,
            offerToken = subDetailOffer.offerToken,
            payload = this,
            discount = 0,
            currency = mainPricing.priceCurrencyCode,
        )
    }

    private fun extractDays(value: String): Int {
        var days = 0
        days += Utils.extractDuration(value, Utils.DAYS_REGEX)
        days += Utils.extractDuration(value, Utils.WEEKS_REGEX) * 7
        return days
    }


}