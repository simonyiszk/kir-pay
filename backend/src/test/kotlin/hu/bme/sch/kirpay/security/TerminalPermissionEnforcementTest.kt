package hu.bme.sch.kirpay.security

import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.account.AccountRepository
import hu.bme.sch.kirpay.order.Item
import hu.bme.sch.kirpay.order.ItemRepository
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class TerminalPermissionEnforcementTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  @Autowired
  private lateinit var springSessionRepositoryFilter: SessionRepositoryFilter<*>

  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var accountRepository: AccountRepository

  @Autowired
  private lateinit var itemRepository: ItemRepository

  @Autowired
  private lateinit var principalRepository: PrincipalRepository

  private val encoder = BCryptPasswordEncoder()

  @BeforeEach
  fun setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
      .addFilter<DefaultMockMvcBuilder>(springSessionRepositoryFilter, "/*")
      .apply<DefaultMockMvcBuilder>(springSecurity())
      .build()
    accountRepository.deleteAll()
    itemRepository.deleteAll()
  }

  private fun saveTerminal(name: String, rawPassword: String, block: (Principal.() -> Principal)? = null): Principal {
    val base = Principal(
      id = null, name = name, secret = encoder.encode(rawPassword)!!,
      role = Role.TERMINAL, active = true,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis(), version = 0
    )
    return principalRepository.save(if (block != null) block(base) else base)
  }

  private fun login(username: String, password: String): Cookie =
    mockMvc.perform(
      post("/v1/api/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"username":"$username","password":"$password"}""")
    )
      .andExpect(status().isNoContent)
      .andReturn().response.getCookie("SESSION")!!

  @Test
  fun `anonymous gets 401 on terminal endpoint`() {
    mockMvc.perform(get("/v1/api/terminal/account-by-card/NOEXIST"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `anonymous gets 401 on admin endpoint`() {
    mockMvc.perform(get("/v1/api/admin/events?page=0&size=20"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `terminal gets 403 on admin endpoint`() {
    val terminal = saveTerminal("forbidden-terminal", "test-pw")

    mockMvc.perform(get("/v1/api/admin/events?page=0&size=20")
      .cookie(login(terminal.name, "test-pw"))
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `terminal without sell permission gets 403 on checkout`() {
    val rawPassword = "test-pw"
    val terminal = saveTerminal("no-sell", rawPassword) {
      copy(canSellItems = false)
    }
    val account = accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = "SEC01-CARD",
      balance = java.math.BigInteger.valueOf(1000),
      active = true))
    val item = itemRepository.save(Item(id = null,
      name = "Beer",
      alias = null,
      cost = java.math.BigInteger.valueOf(100),
      stock = 50,
      enabled = true))

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-CARD/checkout")
      .cookie(login(terminal.name, rawPassword))
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"orderLines":[{"itemId":${item.id},"itemCount":1,"usedVoucher":false}],"idempotencyKey":"00000000-0000-0000-0000-000000000001"}""")
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `terminal without redeem vouchers gets 403`() {
    val rawPassword = "test-pw"
    val terminal = saveTerminal("no-voucher", rawPassword) {
      copy(canRedeemVouchers = false)
    }
    val account = accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = "SEC01-VCH",
      balance = java.math.BigInteger.valueOf(1000),
      active = true))
    val item = itemRepository.save(Item(id = null,
      name = "Item",
      alias = null,
      cost = java.math.BigInteger.valueOf(100),
      stock = 50,
      enabled = true))

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-VCH/checkout")
      .cookie(login(terminal.name, rawPassword))
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"orderLines":[{"itemId":${item.id},"itemCount":1,"usedVoucher":true}],"idempotencyKey":"00000000-0000-0000-0000-000000000002"}""")
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `terminal without upload permission gets 403`() {
    val rawPassword = "test-pw"
    val terminal = saveTerminal("no-upload", rawPassword) {
      copy(canUpload = false)
    }
    accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = "SEC01-UP",
      balance = java.math.BigInteger.valueOf(1000),
      active = true))

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-UP/upload")
      .cookie(login(terminal.name, rawPassword))
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":100,"idempotencyKey":"00000000-0000-0000-0000-000000000003"}""")
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `terminal without transfer permission gets 403`() {
    val rawPassword = "test-pw"
    val terminal = saveTerminal("no-transfer", rawPassword) {
      copy(canTransfer = false)
    }
    accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = "SEC01-TX",
      balance = java.math.BigInteger.valueOf(1000),
      active = true))
    accountRepository.save(Account(id = null,
      name = "Recipient",
      email = null,
      phone = null,
      card = "SEC01-RX",
      balance = java.math.BigInteger.valueOf(100),
      active = true))

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-TX/transfer")
      .cookie(login(terminal.name, rawPassword))
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"recipientCard":"SEC01-RX","amount":100,"idempotencyKey":"00000000-0000-0000-0000-000000000004"}""")
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `terminal without assign cards permission gets 403`() {
    val rawPassword = "test-pw"
    val terminal = saveTerminal("no-assign", rawPassword) {
      copy(canAssignCards = false)
    }
    val account = accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = null,
      balance = java.math.BigInteger.valueOf(1000),
      active = true))

    mockMvc.perform(post("/v1/api/terminal/accounts/${account.id}/card")
      .cookie(login(terminal.name, rawPassword))
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"card":"NEW-CARD"}""")
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `terminal with all permissions gets 200 on all endpoints`() {
    val rawPassword = "test-pw"
    val terminal = saveTerminal("all-perms", rawPassword)
    accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = "SEC01-ALL",
      balance = java.math.BigInteger.valueOf(10000),
      active = true))
    accountRepository.save(Account(id = null,
      name = "Recipient",
      email = null,
      phone = null,
      card = "SEC01-RECV",
      balance = java.math.BigInteger.valueOf(100),
      active = true))
    val item = itemRepository.save(Item(id = null,
      name = "Item",
      alias = null,
      cost = java.math.BigInteger.valueOf(100),
      stock = 50,
      enabled = true))

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-ALL/checkout")
      .cookie(login(terminal.name, rawPassword))
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"orderLines":[{"itemId":${item.id},"itemCount":1,"usedVoucher":false}],"idempotencyKey":"00000000-0000-0000-0000-000000000009"}""")
    ).andExpect(status().isOk)

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-ALL/upload")
      .cookie(login(terminal.name, rawPassword))
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":100,"idempotencyKey":"00000000-0000-0000-0000-000000000005"}""")
    ).andExpect(status().isOk)

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-RECV/transfer")
      .cookie(login(terminal.name, rawPassword))
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"recipientCard":"SEC01-ALL","amount":10,"idempotencyKey":"00000000-0000-0000-0000-000000000006"}""")
    ).andExpect(status().isOk)

    val newAccount = accountRepository.save(Account(id = null,
      name = "New",
      email = null,
      phone = null,
      card = null,
      balance = java.math.BigInteger.valueOf(0),
      active = true))
    mockMvc.perform(post("/v1/api/terminal/accounts/${newAccount.id}/card")
      .cookie(login(terminal.name, rawPassword))
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"card":"SEC01-NEWCARD"}""")
    ).andExpect(status().isOk)

    mockMvc.perform(get("/v1/api/terminal/account-by-card/SEC01-ALL")
      .cookie(login(terminal.name, rawPassword))
    ).andExpect(status().isOk)
  }
}
