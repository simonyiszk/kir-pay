package hu.bme.sch.kirpay.common

import org.springframework.dao.TransientDataAccessException
import org.springframework.resilience.annotation.Retryable

@Retryable(
    value = [TransientDataAccessException::class],
    maxRetries = 5,
    delay = 200,
    maxDelay = 750,
    multiplier = 1.5,
    jitter = 100
)
annotation class RetryTransaction
