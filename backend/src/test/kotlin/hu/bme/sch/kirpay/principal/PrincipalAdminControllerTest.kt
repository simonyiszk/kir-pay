package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.testPrincipalDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.time.Clock

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class PrincipalAdminControllerTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var principalRepository: PrincipalRepository

  private val encoder = BCryptPasswordEncoder()
  private val clock = Clock.systemUTC()

  @BeforeEach
  fun setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    val admin = principalRepository.findByName("test-admin")!!
    val auth = UsernamePasswordAuthenticationToken(admin, "test-admin-pw", admin.authorities)
    SecurityContextHolder.getContext().authentication = auth
  }

  @AfterEach
  fun clearAuth() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `create valid terminal principal returns success`() {
    mockMvc.perform(post("/v1/api/admin/principals")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"new-terminal","password":"pw123","role":"TERMINAL","active":true,"canUpload":true,"canTransfer":true,"canSellItems":true,"canRedeemVouchers":true,"canAssignCards":true}""")
    ).andExpect(status().is2xxSuccessful)
      .andExpect(jsonPath("$.name").value("new-terminal"))
  }

  @Test
  fun `create duplicate principal name returns 400`() {
    val dto = testPrincipalDto(name = "dup-terminal", password = "pw")
    principalRepository.save(dto.toPrincipal(encoder, clock))

    mockMvc.perform(post("/v1/api/admin/principals")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"dup-terminal","password":"pw123","role":"TERMINAL","active":true,"canUpload":true,"canTransfer":true,"canSellItems":true,"canRedeemVouchers":true,"canAssignCards":true}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `disable terminal sets active to false`() {
    val dto = testPrincipalDto(name = "to-disable", password = "pw")
    val terminal = principalRepository.save(dto.toPrincipal(encoder, clock))

    mockMvc.perform(post("/v1/api/admin/principals/${terminal.id}/disable"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.active").value(false))
  }

  @Test
  fun `delete admin returns 400`() {
    val admin = principalRepository.findByName("test-admin")!!

    mockMvc.perform(delete("/v1/api/admin/principals/${admin.id}"))
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `enable previously disabled terminal`() {
    val dto = testPrincipalDto(name = "to-enable", password = "pw", active = false)
    val terminal = principalRepository.save(dto.toPrincipal(encoder, clock).copy(active = false))

    mockMvc.perform(post("/v1/api/admin/principals/${terminal.id}/enable"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.active").value(true))
  }
}
