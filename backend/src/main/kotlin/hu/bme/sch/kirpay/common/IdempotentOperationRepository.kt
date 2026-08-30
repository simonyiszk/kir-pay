package hu.bme.sch.kirpay.common

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface IdempotentOperationRepository : JpaRepository<IdempotentOperation, Int> {
  @Query(value = "select * from idempotent_operations where idempotency_key = :key", nativeQuery = true)
  fun findByIdempotencyKey(key: String): IdempotentOperation?

  @Modifying
  @Query(value = "delete from idempotent_operations where created_at < :before", nativeQuery = true)
  fun deleteByCreatedAtBefore(before: Long): Int
}
