package hu.bme.sch.kirpay.account

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class AccountAdminControllerTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var accountRepository: AccountRepository

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

  private fun createAccount(name: String, balance: Long = 0): Account {
    return accountRepository.save(Account(id = null,
      name = name,
      email = null,
      phone = null,
      card = null,
      balance = balance,
      active = true))
  }

  @Test
  fun `create account with negative balance returns 400`() {
    mockMvc.perform(post("/v1/api/admin/accounts")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"Test","balance":-100,"active":true}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `create account with blank name returns 400`() {
    mockMvc.perform(post("/v1/api/admin/accounts")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"","balance":0,"active":true}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `create valid account returns success`() {
    mockMvc.perform(post("/v1/api/admin/accounts")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"Valid Account","balance":50,"active":true}""")
    ).andExpect(status().is2xxSuccessful)
      .andExpect(jsonPath("$.name").value("Valid Account"))
      .andExpect(jsonPath("$.balance").value(50))
  }

  @Test
  fun `update account name preserves balance`() {
    val account = createAccount(name = "Original", balance = 500)

    mockMvc.perform(post("/v1/api/admin/accounts/${account.id}")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"Renamed","active":true}""")
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.name").value("Renamed"))
      .andExpect(jsonPath("$.balance").value(500))
  }

  @Test
  fun `delete account with positive balance returns 400`() {
    val account = createAccount(name = "Rich", balance = 1000)

    mockMvc.perform(delete("/v1/api/admin/accounts/${account.id}"))
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `delete account with zero balance succeeds`() {
    val account = createAccount(name = "Poor", balance = 0)

    mockMvc.perform(delete("/v1/api/admin/accounts/${account.id}"))
      .andExpect(status().isOk)
  }

  @Test
  fun `export accounts CSV returns file`() {
    createAccount(name = "Exportable", balance = 100)

    mockMvc.perform(get("/v1/api/admin/export/accounts"))
      .andExpect(status().isOk)
      .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("accounts.csv")))
  }

  @Test
  fun `disable account sets active to false`() {
    val account = createAccount(name = "To Disable", balance = 0)

    mockMvc.perform(post("/v1/api/admin/accounts/${account.id}/disable"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.active").value(false))
  }

  @Test
  fun `enable account sets active to true`() {
    val account = accountRepository.save(
      Account(id = null, name = "To Enable", email = null, phone = null, card = null, balance = 0, active = false)
    )

    mockMvc.perform(post("/v1/api/admin/accounts/${account.id}/enable"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.active").value(true))
  }

  @Test
  fun `update non-existent account returns 400`() {
    mockMvc.perform(post("/v1/api/admin/accounts/9999")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"name":"Ghost","active":true}""")
    ).andExpect(status().isBadRequest)
  }
}
