package hu.bme.sch.kirpay.common

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Clock


@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
class SessionMetadataFilter(
  private val sessionRepository: SessionDataRepository,
  private val clock: Clock,
) : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: jakarta.servlet.FilterChain,
  ) {
    recordMetadata(request)
    filterChain.doFilter(request, response)
  }

  private fun recordMetadata(request: HttpServletRequest) {
    val sessionId = request.getSession(false)?.id ?: return
    try {
      sessionRepository.insertIfAbsent(
        sessionId,
        request.remoteAddr,
        request.getHeader("User-Agent"),
        clock.millis(),
      )
    } catch (e: DataIntegrityViolationException) {
      logger.debug("Session $sessionId was removed between lookup and metadata insert", e)
    }
  }
}
