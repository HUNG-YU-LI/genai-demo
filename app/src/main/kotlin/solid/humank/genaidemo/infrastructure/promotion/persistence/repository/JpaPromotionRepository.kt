package solid.humank.genaidemo.infrastructure.promotion.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import solid.humank.genaidemo.infrastructure.promotion.persistence.entity.JpaPromotionEntity
import java.time.LocalDateTime

/**
 * 促銷 JPA Repository - Kotlin 介面
 *
 * Kotlin 優化點:
 * - ✅ 語法更簡潔
 * - ✅ 保持 Spring Data JPA 的功能性
 * - ✅ 使用 Kotlin 命名參數風格
 */
@Repository
interface JpaPromotionRepository : JpaRepository<JpaPromotionEntity, String> {

    /**
     * 根據類型查找促銷
     */
    fun findByType(type: String): List<JpaPromotionEntity>

    /**
     * 根據狀態查找促銷
     */
    fun findByStatus(status: String): List<JpaPromotionEntity>

    /**
     * 查找活躍的促銷 - 使用命名參數
     */
    @Query(
        """
        SELECT p FROM JpaPromotionEntity p
        WHERE p.status = 'ACTIVE'
        AND p.startDate <= :now
        AND p.endDate >= :now
        """
    )
    fun findActivePromotions(@Param("now") now: LocalDateTime): List<JpaPromotionEntity>

    /**
     * 根據類型和狀態查找促銷
     */
    fun findByTypeAndStatus(type: String, status: String): List<JpaPromotionEntity>

    /**
     * 查找即將過期的促銷（7天內）- Kotlin extension query
     */
    @Query(
        """
        SELECT p FROM JpaPromotionEntity p
        WHERE p.status = 'ACTIVE'
        AND p.endDate BETWEEN :now AND :sevenDaysLater
        """
    )
    fun findExpiringSoon(
        @Param("now") now: LocalDateTime,
        @Param("sevenDaysLater") sevenDaysLater: LocalDateTime
    ): List<JpaPromotionEntity>
}
