package hu.bme.sch.kirpay.common

import org.springframework.resilience.annotation.Retryable
import java.sql.SQLException

@Retryable(
    value = [SQLException::class],
    maxRetries = 5,
    delay = 200,
    maxDelay = 750,
    multiplier = 1.5,
    jitter = 100
)
annotation class RetryTransaction
