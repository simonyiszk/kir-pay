package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.principal.Principal
import hu.bme.sch.kirpay.principal.PrincipalRepository
import hu.bme.sch.kirpay.principal.Role
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class EventAdminControllerTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  @Autowired
  private lateinit var springSessionRepositoryFilter: SessionRepositoryFilter<*>

  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var principalRepository: PrincipalRepository

  private val encoder = BCryptPasswordEncoder()

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
  fun `anonymous gets 401`() {
    mockMvc.perform(get("/v1/api/admin/events?page=0&size=20"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `terminal gets 403 on admin events`() {
    val terminal = principalRepository.save(Principal(
      id = null, name = "terminal-403-test", secret = encoder.encode("terminal-pw")!!,
      role = Role.TERMINAL, active = true,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis(), version = 0
    ))

    mockMvc.perform(get("/v1/api/admin/events?page=0&size=20")
      .cookie(login(terminal.name, "terminal-pw"))
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `paginated events returns 200`() {
    mockMvc.perform(get("/v1/api/admin/events?page=0&size=20").cookie(adminAuth()))
      .andExpect(status().isOk)
  }

  @Test
  fun `negative page returns 400`() {
    mockMvc.perform(get("/v1/api/admin/events?page=-1&size=20").cookie(adminAuth()))
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `zero size returns 400`() {
    mockMvc.perform(get("/v1/api/admin/events?page=0&size=0").cookie(adminAuth()))
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `size exceeding max returns 400`() {
    mockMvc.perform(get("/v1/api/admin/events?page=0&size=501").cookie(adminAuth()))
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `export events CSV returns file`() {
    mockMvc.perform(get("/v1/api/admin/export/events").cookie(adminAuth()))
      .andExpect(status().isOk)
      .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("events.csv")))
  }
}
