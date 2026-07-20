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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class AccountTerminalControllerTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var accountRepository: AccountRepository

  @Autowired
  private lateinit var principalRepository: PrincipalRepository

  private lateinit var accountA: Account
  private lateinit var accountB: Account

  @BeforeEach
  fun setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    val admin = principalRepository.findByName("test-admin")!!
    val auth = UsernamePasswordAuthenticationToken(admin, "test-admin-pw", admin.authorities)
    SecurityContextHolder.getContext().authentication = auth

    accountA = accountRepository.save(Account(id = null,
      name = "Account A",
      email = "a@example.com",
      phone = null,
      card = "CARD-A",
      balance = 1000,
      active = true))
    accountB = accountRepository.save(Account(id = null,
      name = "Account B",
      email = "b@example.com",
      phone = null,
      card = "CARD-B",
      balance = 1000,
      active = true))
  }

  @AfterEach
  fun clearAuth() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `upload valid amount increases balance`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/upload")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":200}""")
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.balance").value(1200))
  }

  @Test
  fun `upload zero amount returns 400`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/upload")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":0}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `upload negative amount returns 400`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/upload")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":-50}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `transfer valid decreases sender and increases recipient`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/transfer")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"recipientCard":"CARD-B","amount":300}""")
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.balance").value(700))
  }

  @Test
  fun `transfer to self returns 400`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/transfer")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"recipientCard":"CARD-A","amount":100}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `transfer insufficient balance returns 400`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/transfer")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"recipientCard":"CARD-B","amount":9999}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `assign card sets new card`() {
    mockMvc.perform(post("/v1/api/terminal/accounts/${accountA.id}/card")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"card":"NEW-CARD"}""")
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.card").value("NEW-CARD"))
  }

  @Test
  fun `get balance by card returns account with vouchers`() {
    mockMvc.perform(get("/v1/api/terminal/account-by-card/CARD-A"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.account.name").value("Account A"))
      .andExpect(jsonPath("$.account.balance").value(1000))
      .andExpect(jsonPath("$.vouchers").isArray)
  }

  @Test
  fun `get balance by email returns account`() {
    mockMvc.perform(get("/v1/api/terminal/account-by-email/a@example.com"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.account.name").value("Account A"))
  }

  @Test
  fun `get balance by unknown card returns 404`() {
    mockMvc.perform(get("/v1/api/terminal/account-by-card/NOEXIST"))
      .andExpect(status().isNotFound)
  }

  @Test
  fun `get balance by unknown email returns 404`() {
    mockMvc.perform(get("/v1/api/terminal/account-by-email/nobody@example.com"))
      .andExpect(status().isNotFound)
  }
}
