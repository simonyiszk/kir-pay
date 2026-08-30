package hu.bme.sch.kirpay.common

import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(ADMIN_API)
class SessionAdminController(
  private val sessionRepository: SessionDataRepository,
  private val jdbcSessionRepository: JdbcIndexedSessionRepository,
) {
  @GetMapping("/sessions")
  fun listSessions(
    @RequestParam(defaultValue = "$DEFAULT_PAGE") page: Int,
    @RequestParam(defaultValue = "$DEFAULT_PAGE_SIZE") size: Int,
  ): List<SessionDataRepository.SessionInfo> {
    requireValidPagination(page, size)

    return sessionRepository.findAllSessions(page, size)
  }

  @DeleteMapping("/sessions/{sessionId}")
  fun revokeSession(@PathVariable sessionId: String): Map<String, String> {
    jdbcSessionRepository.deleteById(sessionId)
    return mapOf("status" to "revoked", "sessionId" to sessionId)
  }
}
