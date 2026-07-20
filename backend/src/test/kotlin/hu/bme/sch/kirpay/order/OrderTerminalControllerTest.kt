package hu.bme.sch.kirpay.order

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class OrderTerminalControllerTest {

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
  fun `GET items returns only enabled items`() {
    createItem(name = "Enabled Item", cost = 100, stock = 10, enabled = true)
    createItem(name = "Disabled Item", cost = 200, stock = 5, enabled = false)

    mockMvc.perform(get("/v1/api/terminal/items"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$[?(@.name == 'Enabled Item')]").exists())
      .andExpect(jsonPath("$[?(@.name == 'Disabled Item')]").doesNotExist())
  }

  @Test
  fun `GET items does not expose disabled items`() {
    createItem(name = "Hidden", cost = 50, stock = 1, enabled = false)
    createItem(name = "Also Hidden", cost = 75, stock = 2, enabled = false)

    mockMvc.perform(get("/v1/api/terminal/items"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(0))
  }
}
