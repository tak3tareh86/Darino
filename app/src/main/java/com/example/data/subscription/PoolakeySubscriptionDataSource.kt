package com.example.data.subscription

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import ir.cafebazaar.poolakey.Connection
import ir.cafebazaar.poolakey.Payment
import ir.cafebazaar.poolakey.config.PaymentConfiguration
import ir.cafebazaar.poolakey.config.SecurityCheck
import ir.cafebazaar.poolakey.entity.PurchaseInfo
import ir.cafebazaar.poolakey.request.PurchaseRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

sealed class PoolakeyResult<out T> {
    data class Success<T>(val data: T) : PoolakeyResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : PoolakeyResult<Nothing>()
}

class PoolakeySubscriptionDataSource(private val context: Context) {

    private var payment: Payment? = null
    private var activeConnection: Connection? = null

    private fun getOrCreatePayment(): Payment {
        return payment ?: run {
            val paymentConfig = PaymentConfiguration(
                localSecurityCheck = SecurityCheck.Enable(rsaPublicKey = MarketConfig.RSA_PUBLIC_KEY)
            )
            Payment(context = context, config = paymentConfig).also { payment = it }
        }
    }

    fun initConnection(onConnected: ((Boolean) -> Unit)? = null) {
        val p = getOrCreatePayment()
        activeConnection = p.connect {
            connectionSucceed {
                onConnected?.invoke(true)
            }
            connectionFailed { throwable ->
                onConnected?.invoke(false)
            }
            disconnected {
                // Connection closed
            }
        }
    }

    fun disconnect() {
        try {
            activeConnection?.disconnect()
            activeConnection = null
        } catch (e: Exception) {
            // Ignore disconnect errors
        }
    }

    suspend fun getSubscribedProducts(): PoolakeyResult<List<PurchaseInfo>> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val p = getOrCreatePayment()
            p.connect {
                connectionSucceed {
                    p.getSubscribedProducts {
                        querySucceed { purchases ->
                            if (continuation.isActive) {
                                continuation.resume(PoolakeyResult.Success(purchases))
                            }
                        }
                        queryFailed { throwable ->
                            if (continuation.isActive) {
                                continuation.resume(
                                    PoolakeyResult.Error(
                                        "خطا در دریافت اطلاعات اشتراک از بازار: ${throwable.message}",
                                        throwable
                                    )
                                )
                            }
                        }
                    }
                }
                connectionFailed { throwable ->
                    if (continuation.isActive) {
                        continuation.resume(
                            PoolakeyResult.Error(
                                "امکان اتصال به حساب بازار وجود ندارد: ${throwable.message}",
                                throwable
                            )
                        )
                    }
                }
            }
        }
    }

    fun subscribeProduct(
        registry: ActivityResultRegistry,
        productId: String,
        payload: String,
        onFlowBegan: () -> Unit,
        onFailedToBegin: (String) -> Unit,
        onSucceed: (PurchaseInfo) -> Unit,
        onCanceled: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        val p = getOrCreatePayment()
        p.connect {
            connectionSucceed {
                p.subscribeProduct(
                    registry = registry,
                    request = PurchaseRequest(productId = productId, payload = payload)
                ) {
                    purchaseFlowBegan {
                        onFlowBegan()
                    }
                    failedToBeginFlow { throwable ->
                        onFailedToBegin(throwable.localizedMessage ?: "امکان شروع پرداخت در بازار وجود ندارد.")
                    }
                    purchaseSucceed { purchaseInfo ->
                        onSucceed(purchaseInfo)
                    }
                    purchaseCanceled {
                        onCanceled()
                    }
                    purchaseFailed { throwable ->
                        onFailed(throwable.localizedMessage ?: "پرداخت با خطا مواجه شد.")
                    }
                }
            }
            connectionFailed { throwable ->
                onFailedToBegin("امکان اتصال به اپلیکیشن بازار وجود ندارد.")
            }
        }
    }
}
