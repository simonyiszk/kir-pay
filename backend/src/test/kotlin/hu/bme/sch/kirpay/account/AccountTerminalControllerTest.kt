package hu.bme.sch.kirpay.account


import hu.bme.sch.kirpay.principal.PrincipalRepository
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.session.web.http.SessionRepositoryFilter
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional

class AccountTerminalControllerTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  @Autowired
  private lateinit var springSessionRepositoryFilter: SessionRepositoryFilter<*>

  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var accountRepository: AccountRepository

  @Autowired
  private lateinit var principalRepository: PrincipalRepository

  private lateinit var accountA: Account
  private lateinit var accountB: Account

  @BeforeEach
  fun setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
      .addFilter<DefaultMockMvcBuilder>(springSessionRepositoryFilter, "/*")
      .apply<DefaultMockMvcBuilder>(springSecurity())
      .build()

    accountA = accountRepository.save(Account(id = null,
      name = "Account A",
      email = "a@example.com",
      phone = null,
      card = "CARD-A",
      balance = java.math.BigInteger.valueOf(1000),
      active = true))
    accountB = accountRepository.save(Account(id = null,
      name = "Account B",
      email = "b@example.com",
      phone = null,
      card = "CARD-B",
      balance = java.math.BigInteger.valueOf(1000),
      active = true))
  }

  private fun adminAuth(): Cookie = login("admin", "admin")

  private fun idemKey() = UUID.randomUUID().toString()

  private fun login(username: String, password: String): Cookie =
    mockMvc.perform(
      post("/v1/api/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"username":"$username","password":"$password"}""")
    )
      .andExpect(status().isNoContent)
      .andReturn().response.getCookie("SESSION")!!

  @Test
  fun `upload valid amount increases balance`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/upload")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":200,"idempotencyKey":"${idemKey()}"}""")
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.balance").value(1200))
  }

  @Test
  fun `upload zero amount returns 400`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/upload")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":0,"idempotencyKey":"${idemKey()}"}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `upload negative amount returns 400`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/upload")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":-50,"idempotencyKey":"${idemKey()}"}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `upload without idempotency key returns 400`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/upload")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":200}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `pay without idempotency key returns 400`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/pay")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":200}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `transfer valid decreases sender and increases recipient`() {
    val key = idemKey()
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/transfer")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"recipientCard":"CARD-B","amount":300,"idempotencyKey":"$key"}""")
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.balance").value(700))
  }

  @Test
  fun `transfer to self returns 400`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/transfer")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"recipientCard":"CARD-A","amount":100,"idempotencyKey":"${idemKey()}"}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `transfer insufficient balance returns 400`() {
    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/transfer")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"recipientCard":"CARD-B","amount":9999,"idempotencyKey":"${idemKey()}"}""")
    ).andExpect(status().isBadRequest)
  }

  @Test
  fun `same upload key twice returns same balance on second call`() {
    val key = idemKey()
    val payload = """{"amount":200,"idempotencyKey":"$key"}"""

    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/upload")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content(payload)
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.balance").value(1200))

    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/upload")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content(payload)
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.balance").value(1200))
  }

  @Test
  fun `same key different amount returns 422`() {
    val key = idemKey()

    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/upload")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":200,"idempotencyKey":"$key"}""")
    ).andExpect(status().isOk)

    mockMvc.perform(post("/v1/api/terminal/account-by-card/CARD-A/upload")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":500,"idempotencyKey":"$key"}""")
    ).andExpect(status().isUnprocessableContent)
  }

  @Test
  fun `assign card sets new card`() {
    mockMvc.perform(post("/v1/api/terminal/accounts/${accountA.id}/card")
      .cookie(adminAuth())
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"card":"NEW-CARD"}""")
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.card").value("NEW-CARD"))
  }

  @Test
  fun `get balance by card returns account with vouchers`() {
    mockMvc.perform(get("/v1/api/terminal/account-by-card/CARD-A")
      .cookie(adminAuth())
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.account.name").value("Account A"))
      .andExpect(jsonPath("$.account.balance").value(1000))
      .andExpect(jsonPath("$.vouchers").isArray)
  }

  @Test
  fun `get balance by email returns account`() {
    mockMvc.perform(get("/v1/api/terminal/account-by-email/a@example.com")
      .cookie(adminAuth())
    ).andExpect(status().isOk)
      .andExpect(jsonPath("$.account.name").value("Account A"))
  }

  @Test
  fun `get balance by unknown card returns 404`() {
    mockMvc.perform(get("/v1/api/terminal/account-by-card/NOEXIST")
      .cookie(adminAuth())
    ).andExpect(status().isNotFound)
  }

  @Test
  fun `get balance by unknown email returns 404`() {
    mockMvc.perform(get("/v1/api/terminal/account-by-email/nobody@example.com")
      .cookie(adminAuth())
    ).andExpect(status().isNotFound)
  }
}
