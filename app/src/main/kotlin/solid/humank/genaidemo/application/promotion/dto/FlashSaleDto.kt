package solid.humank.genaidemo.application.promotion.dto

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 閃購促銷DTO - Kotlin data class 優化版本
 *
 * Kotlin 優化點:
 * - ✅ data class 簡潔語法
 * - ✅ Computed properties 計算折扣相關資訊
 * - ✅ Extension properties 增強可讀性
 */
data class FlashSaleDto(
    val promotionId: String,
    val productId: String,
    val productName: String,
    val originalPrice: BigDecimal,
    val specialPrice: BigDecimal,
    val quantityLimit: Int,
    val remainingQuantity: Int,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val isAvailable: Boolean
) {
    /**
     * 計算折扣金額
     */
    val discountAmount: BigDecimal
        get() = originalPrice - specialPrice

    /**
     * 計算折扣百分比
     */
    val discountPercentage: Double
        get() = if (originalPrice > BigDecimal.ZERO) {
            ((originalPrice - specialPrice).toDouble() / originalPrice.toDouble() * 100)
                .coerceIn(0.0, 100.0)
        } else 0.0

    /**
     * 檢查是否已售罄
     */
    val isSoldOut: Boolean
        get() = remainingQuantity <= 0

    /**
     * 檢查是否庫存緊張（剩餘 < 20%）
     */
    val isLowStock: Boolean
        get() = remainingQuantity.toDouble() / quantityLimit < 0.2

    /**
     * 檢查是否正在進行中
     */
    val isOngoing: Boolean
        get() {
            val now = LocalDateTime.now()
            return isAvailable && now in startDate..endDate
        }

    /**
     * 格式化折扣訊息（例如："限時特價 ¥899，省 ¥100"）
     */
    fun formatDiscountMessage(): String =
        "限時特價 ¥$specialPrice，省 ¥$discountAmount（${String.format("%.1f", discountPercentage)}% OFF）"
}
