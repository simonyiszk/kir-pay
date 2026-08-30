package hu.bme.sch.kirpay.common

import org.springframework.dao.TransientDataAccessException
import org.springframework.resilience.annotation.Retryable

@Retryable(
  value = [TransientDataAccessException::class],
  maxRetries = 5,
  delay = 50,
  maxDelay = 250,
  multiplier = 1.5,
  jitter = 50
)
annotation class RetryTransaction
