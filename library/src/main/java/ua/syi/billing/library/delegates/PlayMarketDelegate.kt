package ua.syi.billing.library.delegates

import android.app.Activity
import ua.syi.billing.library.models.PurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class PlayMarketDelegate(protected val client: BillingClient) {

    protected suspend fun getProductDetails(type: String, ids: Set<String>): List<ProductDetails> {
        val productList = ids.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(type)
                .build()
        }
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = withContext(Dispatchers.IO) {
            client.queryProductDetails(queryProductDetailsParams)
        }

        return result.productDetailsList ?: emptyList()
    }

    protected fun purchase(activity: Activity, productDetails: ProductDetails, offerToken: String?, params: PurchaseParams) {
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setObfuscatedAccountId(params.obfuscatedAccountId)
            .setObfuscatedProfileId(params.obfuscatedProfileId)
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .let { if (offerToken != null) it.setOfferToken(offerToken) else it }
                        .build()
                )
            )
            .build()
        client.launchBillingFlow(activity, billingFlowParams)
    }

}