package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.testPrincipalDto
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.session.web.http.SessionRepositoryFilter
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.time.Clock
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class PrincipalAdminControllerTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  @Autowired
  private lateinit var springSessionRepositoryFilter: SessionRepositoryFilter<*>

  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var principalRepository: PrincipalRepository

  private val encoder = BCryptPasswordEncoder()
  private val clock = Clock.systemUTC()

  @BeforeEach
  fun setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
      .addFilter<DefaultMockMvcBuilder>(springSessionRepositoryFilter, "/*")
      .apply<DefaultMockMvcBuilder>(springSecurity())
      .build()
  }

  private fun adminAuth(): Cookie = login("admin", "admin")

  private fun login(username: String, password: String): Cookie =
    mockMvc.perform(
      post("/v1/api/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"username":"$username","password":"$password"}""")
    )
      .andExpect(status().isNoContent)
      .andReturn().response.getCookie("SESSION")!!

  @Test
  fun `anonymous gets 401 on admin endpoint`() {
    mockMvc.perform(get("/v1/api/admin/principals"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `terminal gets 403 on admin principals`() {
    val terminal = principalRepository.save(Principal(
      id = null, name = "term-403-test", secret = encoder.encode("term-pw")!!,
      role = Role.TERMINAL, active = true,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis(), version = 0
    ))

    mockMvc.perform(get("/v1/api/admin/principals")
      .cookie(login(terminal.name, "term-pw"))
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `create valid terminal principal returns success`() {
    mockMvc.perform(post("/v1/api/admin/principals")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"new-terminal","password":"pw123","role":"TERMINAL","active":true,"canUpload":true,"canTransfer":true,"canSellItems":true,"canRedeemVouchers":true,"canAssignCards":true,"idempotencyKey":"${UUID.randomUUID()}"}""")
    ).andExpect(status().is2xxSuccessful)
      .andExpect(jsonPath("$.name").value("new-terminal"))
  }

  @Test
  fun `create duplicate principal name returns 400`() {
    val dto = testPrincipalDto(name = "dup-terminal", password = "pw")
    principalRepository.save(dto.toPrincipal(encoder, clock))

    mockMvc.perform(post("/v1/api/admin/principals")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"dup-terminal","password":"pw123","role":"TERMINAL","active":true,"canUpload":true,"canTransfer":true,"canSellItems":true,"canRedeemVouchers":true,"canAssignCards":true,"idempotencyKey":"${UUID.randomUUID()}"}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `disable terminal sets active to false`() {
    val dto = testPrincipalDto(name = "to-disable", password = "pw")
    val terminal = principalRepository.save(dto.toPrincipal(encoder, clock))

    mockMvc.perform(post("/v1/api/admin/principals/${terminal.id}/disable")
      .cookie(adminAuth())
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.active").value(false))
  }

  @Test
  fun `delete admin returns 400`() {
    val admin = principalRepository.findByName("admin")!!

    mockMvc.perform(delete("/v1/api/admin/principals/${admin.id}")
      .cookie(adminAuth())
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `enable previously disabled terminal`() {
    val dto = testPrincipalDto(name = "to-enable", password = "pw", active = false)
    val terminal = principalRepository.save(dto.toPrincipal(encoder, clock).copy(active = false))

    mockMvc.perform(post("/v1/api/admin/principals/${terminal.id}/enable")
      .cookie(adminAuth())
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.active").value(true))
  }
}
