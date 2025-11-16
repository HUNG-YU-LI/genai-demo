package solid.humank.genaidemo.domain.shared.valueobject

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.util.UUID

/**
 * ProductId 測試 - 使用 Kotest 風格
 *
 * Kotest 優勢:
 * - ✅ 更自然的 BDD 風格 (describe/it)
 * - ✅ Property-based testing
 * - ✅ Kotlin DSL 語法
 * - ✅ 豐富的 matchers
 * - ✅ 更簡潔的測試代碼
 */
class ProductIdTest : DescribeSpec({

    describe("ProductId creation") {
        it("should create ProductId with valid string") {
            val id = ProductId.of("product-123")
            id.value shouldBe "product-123"
        }

        it("should create ProductId with UUID") {
            val uuid = UUID.randomUUID()
            val id = ProductId.of(uuid)
            id.value shouldBe uuid.toString()
        }

        it("should generate new ProductId") {
            val id = ProductId.generate()
            id.value.shouldNotBeBlank()
            id.isUUIDFormat shouldBe true
        }

        it("should throw exception for blank ProductId") {
            shouldThrow<IllegalArgumentException> {
                ProductId.of("")
            }
        }

        it("should throw exception for empty ProductId") {
            shouldThrow<IllegalArgumentException> {
                ProductId.of("   ")
            }
        }
    }

    describe("ProductId UUID operations") {
        it("should convert to UUID when format is valid") {
            val uuid = UUID.randomUUID()
            val id = ProductId.of(uuid)
            id.toUUID() shouldBe uuid
        }

        it("should detect valid UUID format") {
            val uuid = UUID.randomUUID()
            val id = ProductId.of(uuid)
            id.isUUIDFormat shouldBe true
        }

        it("should detect invalid UUID format") {
            val id = ProductId.of("not-a-uuid")
            id.isUUIDFormat shouldBe false
        }

        it("should throw exception when converting invalid UUID") {
            val id = ProductId.of("invalid-uuid")
            shouldThrow<IllegalArgumentException> {
                id.toUUID()
            }
        }
    }

    describe("ProductId equality") {
        it("should be equal for same value") {
            val id1 = ProductId.of("product-123")
            val id2 = ProductId.of("product-123")
            id1 shouldBe id2
        }

        it("should not be equal for different values") {
            val id1 = ProductId.of("product-123")
            val id2 = ProductId.of("product-456")
            id1 shouldNotBe id2
        }

        it("should have same hashCode for same value") {
            val id1 = ProductId.of("product-123")
            val id2 = ProductId.of("product-123")
            id1.hashCode() shouldBe id2.hashCode()
        }
    }

    describe("ProductId toString") {
        it("should return value as string") {
            val id = ProductId.of("product-123")
            id.toString() shouldBe "product-123"
        }
    }

    // Property-based testing - Kotest 強大功能
    describe("ProductId property-based tests") {
        it("should preserve value for any non-blank string") {
            checkAll(Arb.string(minSize = 1)) { str ->
                if (str.isNotBlank()) {
                    val id = ProductId.of(str)
                    id.value shouldBe str
                    id.toString() shouldBe str
                }
            }
        }

        it("should generate unique IDs") {
            val ids = List(1000) { ProductId.generate() }
            val uniqueIds = ids.toSet()
            uniqueIds.size shouldBe ids.size
        }
    }
})
