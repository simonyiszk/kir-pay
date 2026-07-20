package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.principal.PrincipalRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class EventAdminControllerTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var principalRepository: PrincipalRepository

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
  fun `paginated events returns 200`() {
    mockMvc.perform(get("/v1/api/admin/events?page=0&size=20"))
      .andExpect(status().isOk)
  }

  @Test
  fun `negative page returns 400`() {
    mockMvc.perform(get("/v1/api/admin/events?page=-1&size=20"))
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `zero size returns 400`() {
    mockMvc.perform(get("/v1/api/admin/events?page=0&size=0"))
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `size exceeding max returns 400`() {
    mockMvc.perform(get("/v1/api/admin/events?page=0&size=501"))
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `export events CSV returns file`() {
    mockMvc.perform(get("/v1/api/admin/export/events"))
      .andExpect(status().isOk)
      .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("events.csv")))
  }
}
