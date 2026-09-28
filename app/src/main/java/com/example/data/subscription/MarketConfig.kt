package com.example.data.subscription

/**
 * Central configuration for Cafe Bazaar In-App Purchase (Poolakey)
 */
object MarketConfig {
    /**
     * RSA Public Key from Cafe Bazaar Developer Console.
     * Replace this placeholder with your actual RSA key from Cafe Bazaar panel.
     */
    const val RSA_PUBLIC_KEY = "MIHNMA0GCSqGSIb3DQEBAQUAA4G7ADCBtwKBrwDUstQtbq09xoIdCLVqGv5rkrTQGCy6z5urOJrwmN9i6elRdBRAInQG/mvazJEG1XYe6bRnV1JRFPBqM4rRsM5Iwlm+ZW+wdl4kMpUgZs+GdAwr3QpH7G8R0/mzivBIbpXVtF3XG4cHyRyldPyRKUJ9zbslXpQa0iL//5VhM5ZQq5Pz8Qx1Bs9s7KNTex1JFlI7Sj5C4xatBYaonT9TdoUJOZutxayYicYbpO+xl0kCAwEAAQ=="

    /**
     * Product ID for Darino Yearly Subscription configured in Cafe Bazaar panel.
     */
    const val YEARLY_PRODUCT_ID = "darino_yearly_subscription"

    /**
     * Official UI Display Price in Toman (500,000 Toman)
     */
    const val DISPLAY_PRICE_TOMAN = 500_000L

    /**
     * Free Trial Duration in Days (30 Days)
     */
    const val TRIAL_DURATION_DAYS = 30L

    /**
     * Subscription Validity Duration in Days (365 Days)
     */
    const val SUBSCRIPTION_DURATION_DAYS = 365L
}
