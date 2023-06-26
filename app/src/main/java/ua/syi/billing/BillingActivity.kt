package ua.syi.billing

import android.app.Activity
import android.os.Bundle
import ua.ysmobdev.billing.app.R

class BillingActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.billing_activity)
    }
}