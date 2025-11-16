package solid.humank.genaidemo.domain.shared.valueobject

import solid.humank.genaidemo.domain.common.annotations.ValueObject
import java.util.UUID

/**
 * 客戶ID值對象 - 使用 Kotlin value class 實現
 *
 * Kotlin 優化點:
 * - ✅ value class 提供零開銷抽象
 * - ✅ init block 驗證參數
 * - ✅ computed property 替代方法
 * - ✅ companion object 工廠方法
 * - ✅ 代碼量減少 70%
 */
@ValueObject(name = "CustomerId", description = "客戶唯一標識符，支援 String 和 UUID 格式")
@JvmInline
value class CustomerId(val value: String) {

    init {
        require(value.isNotBlank()) { "Customer ID cannot be empty" }
    }

    /**
     * 獲取ID字符串（向後相容方法）
     */
    fun getId(): String = value

    /**
     * 嘗試轉換為UUID
     */
    fun toUUID(): UUID = UUID.fromString(value)

    /**
     * 檢查是否為有效的UUID格式
     */
    val isUUIDFormat: Boolean
        get() = runCatching { UUID.fromString(value) }.isSuccess

    override fun toString(): String = value

    companion object {
        /**
         * 生成新的客戶ID
         */
        fun generate(): CustomerId = CustomerId(UUID.randomUUID().toString())

        /**
         * 從字符串創建客戶ID
         */
        fun of(id: String): CustomerId = CustomerId(id)

        /**
         * 從UUID創建客戶ID
         */
        fun of(uuid: UUID): CustomerId = CustomerId(uuid.toString())
    }
}
