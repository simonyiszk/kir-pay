package hu.bme.sch.kirpay.common

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Component
class IdempotentOperationCleanup(
  private val idempotentOperationRepository: IdempotentOperationRepository,
  private val clock: Clock
) {
  private val logger = LoggerFactory.getLogger(IdempotentOperationCleanup::class.java)


  @Scheduled(cron = "0 0 */6 * * *")
  @Transactional
  fun cleanup() {
    val cutoff = clock.millis() - 7 * 24 * 60 * 60 * 1000L
    val deleted = idempotentOperationRepository.deleteByCreatedAtBefore(cutoff)
    if (deleted > 0) {
      logger.info("Cleaned up {} expired idempotent operations", deleted)
    }
  }
}
