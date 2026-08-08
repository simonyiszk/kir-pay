package hu.bme.sch.kirpay.common

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

/**
 * JDBC access to the `spring_session` / `session_metadata` tables owned by spring-session-jdbc.
 * Deliberately JDBC-based rather than JPA: the tables are the library's internal schema, and the
 * metadata write relies on `ON CONFLICT DO NOTHING` semantics that JPA cannot express.
 */
@Repository
class SessionDataRepository(private val jdbcClient: JdbcClient) {

  data class SessionInfo(
    val sessionId: String,
    val principalName: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val creationTime: Long,
    val lastAccessTime: Long,
    val maxInactiveInterval: Int,
    val expiryTime: Long,
  )

  fun findAllSessions(page: Int, size: Int): List<SessionInfo> =
    jdbcClient
      .sql(
        """
          SELECT s.session_id, s.principal_name, s.creation_time, s.last_access_time,
                 s.max_inactive_interval, s.expiry_time,
                 m.ip_address, m.user_agent
          FROM spring_session s
          LEFT JOIN session_metadata m ON s.session_id = m.session_id
          ORDER BY s.last_access_time DESC
          LIMIT ? OFFSET ?
        """.trimIndent()
      )
      .params(size, page * size)
      .query { rs, _ ->
        SessionInfo(
          sessionId = rs.getString("session_id"),
          principalName = rs.getString("principal_name"),
          ipAddress = rs.getString("ip_address"),
          userAgent = rs.getString("user_agent"),
          creationTime = rs.getLong("creation_time"),
          lastAccessTime = rs.getLong("last_access_time"),
          maxInactiveInterval = rs.getInt("max_inactive_interval"),
          expiryTime = rs.getLong("expiry_time"),
        )
      }
      .list()

  fun insertIfAbsent(sessionId: String, ipAddress: String, userAgent: String?, createdAtMillis: Long) {
    jdbcClient
      .sql(
        "INSERT INTO session_metadata (session_id, ip_address, user_agent, created_at) VALUES (:sessionId, :ipAddress, :userAgent, :createdAtMillis) ON CONFLICT (session_id) DO NOTHING"
      )
      .param("sessionId", sessionId)
      .param("ipAddress", ipAddress)
      .param("userAgent", userAgent)
      .param("createdAtMillis", createdAtMillis)
      .update()
  }
}
