package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.principal.PrincipalRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class ItemAdminControllerTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var itemRepository: ItemRepository

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

  private fun createItem(name: String, cost: Long, stock: Int, enabled: Boolean): Item =
    itemRepository.save(Item(id = null, name = name, alias = null, cost = cost, stock = stock, enabled = enabled))

  @Test
  fun `create valid item returns success`() {
    mockMvc.perform(post("/v1/api/admin/items")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"New Item","cost":100,"stock":10,"enabled":true,"showOnLeaderboard":false,"version":0}""")
    ).andExpect(status().is2xxSuccessful)
      .andExpect(jsonPath("$.name").value("New Item"))
      .andExpect(jsonPath("$.cost").value(100))
      .andExpect(jsonPath("$.stock").value(10))
  }

  @Test
  fun `update item preserves stock`() {
    val item = createItem(name = "Original", cost = 50, stock = 10, enabled = true)

    mockMvc.perform(post("/v1/api/admin/items/${item.id}")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"Updated","cost":75,"stock":999,"enabled":true,"showOnLeaderboard":false,"version":0}""")
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.name").value("Updated"))
      .andExpect(jsonPath("$.cost").value(75))
      .andExpect(jsonPath("$.stock").value(10))
  }

  @Test
  fun `disable item sets enabled to false`() {
    val item = createItem(name = "To Disable", cost = 100, stock = 10, enabled = true)

    mockMvc.perform(post("/v1/api/admin/items/${item.id}/disable"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.enabled").value(false))
  }

  @Test
  fun `enable item sets enabled to true`() {
    val item = createItem(name = "To Enable", cost = 100, stock = 10, enabled = false)

    mockMvc.perform(post("/v1/api/admin/items/${item.id}/enable"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.enabled").value(true))
  }

  @Test
  fun `update non-existent item returns 400`() {
    mockMvc.perform(post("/v1/api/admin/items/9999")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"Ghost","cost":50,"stock":10,"enabled":true,"showOnLeaderboard":false,"version":0}""")
    ).andExpect(status().isBadRequest)
  }
}
