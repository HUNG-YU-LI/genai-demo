package solid.humank.genaidemo.application.promotion.dto

import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionStatus
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType
import java.time.LocalDateTime

/**
 * 促銷DTO - Kotlin data class 優化版本
 *
 * Kotlin 優化點:
 * - ✅ data class 自動生成 equals/hashCode/toString
 * - ✅ Null safety (description 為 nullable)
 * - ✅ Default values 減少建構子重載
 * - ✅ Computed properties 封裝業務邏輯
 * - ✅ Kotlin range operator (in)
 * - ✅ 更簡潔的語法
 */
data class PromotionDto(
    val id: String,
    val name: String,
    val description: String?,  // Nullable for optional field
    val type: PromotionType,
    val status: PromotionStatus,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val usageLimit: Int = Int.MAX_VALUE,  // Smart default
    val usageCount: Int = 0,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    /**
     * 檢查促銷是否當前活躍 - computed property
     */
    val isActive: Boolean
        get() = status == PromotionStatus.ACTIVE &&
                LocalDateTime.now() in startDate..endDate

    /**
     * 計算剩餘可用次數 - computed property
     */
    val remainingUsage: Int
        get() = (usageLimit - usageCount).coerceAtLeast(0)

    /**
     * 檢查是否即將過期（7天內）
     */
    val isExpiringSoon: Boolean
        get() = LocalDateTime.now().plusDays(7) >= endDate

    /**
     * 計算使用率百分比
     */
    val usagePercentage: Double
        get() = if (usageLimit > 0) {
            (usageCount.toDouble() / usageLimit * 100).coerceIn(0.0, 100.0)
        } else 0.0
}
