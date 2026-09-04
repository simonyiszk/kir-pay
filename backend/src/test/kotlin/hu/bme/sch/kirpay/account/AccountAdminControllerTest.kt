package hu.bme.sch.kirpay.account

import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.session.web.http.SessionRepositoryFilter
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.math.BigInteger
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class AccountAdminControllerTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  @Autowired
  private lateinit var springSessionRepositoryFilter: SessionRepositoryFilter<*>

  @Autowired
  private lateinit var accountRepository: AccountRepository

  private lateinit var mockMvc: MockMvc

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

  private fun seedAccount(name: String, card: String? = null, active: Boolean = true): Account =
    accountRepository.save(
      Account(
        id = null,
        name = name,
        email = null,
        phone = null,
        card = card,
        balance = BigInteger.ZERO,
        active = active,
        version = 0
      )
    )

  private fun importAccounts(csv: String) =
    mockMvc.perform(
      post("/v1/api/admin/import/accounts?idempotencyKey=${UUID.randomUUID()}")
        .cookie(adminAuth())
        .contentType(MediaType.TEXT_PLAIN_VALUE)
        .content(csv)
    )

  @Test
  fun `get all accounts returns disabled accounts too`() {
    seedAccount(name = "Active One", active = true)
    seedAccount(name = "Disabled One", active = false)

    mockMvc.perform(get("/v1/api/admin/accounts").cookie(adminAuth()))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$[?(@.name == 'Active One')].active", hasItem(true)))
      .andExpect(jsonPath("$[?(@.name == 'Disabled One')].active", hasItem(false)))
  }

  @Test
  fun `get account by id returns a disabled account`() {
    val disabled = seedAccount(name = "Disabled One", active = false)

    mockMvc.perform(get("/v1/api/admin/accounts/${disabled.id}").cookie(adminAuth()))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.name").value("Disabled One"))
      .andExpect(jsonPath("$.active").value(false))
  }

  @Test
  fun `get account by unknown id returns 404`() {
    mockMvc.perform(get("/v1/api/admin/accounts/999999").cookie(adminAuth()))
      .andExpect(status().isNotFound)
  }

  @Test
  fun `terminal accounts endpoint still filters disabled accounts`() {
    seedAccount(name = "Active One", active = true)
    seedAccount(name = "Disabled One", active = false)

    mockMvc.perform(get("/v1/api/terminal/accounts").cookie(adminAuth()))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$[?(@.name == 'Active One')].active", hasItem(true)))
      .andExpect(jsonPath("$[?(@.name == 'Disabled One')]").isEmpty)
  }

  @Test
  fun `import accounts valid csv returns 201 and persists`() {
    importAccounts("name,email,phone,card,balance,active\nJohn,,,CARD-1,100,true")
      .andExpect(status().isCreated)
    assertEquals(1, accountRepository.count())
  }

  @Test
  fun `import accounts with blank name returns 400 and imports nothing`() {
    importAccounts("name,email,phone,card,balance,active\nJohn,,,CARD-1,100,true\n,,,CARD-2,200,true")
      .andExpect(status().isBadRequest)

    TestTransaction.end()
    try {
      assertEquals(0, accountRepository.count())
    } finally {
      TestTransaction.start()
    }
  }

  @Test
  fun `import accounts with duplicate card returns 400`() {
    importAccounts("name,email,phone,card,balance,active\nJohn,,,CARD-1,100,true\nJane,,,CARD-1,200,true")
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `anonymous gets 401 on admin accounts`() {
    mockMvc.perform(get("/v1/api/admin/accounts"))
      .andExpect(status().isUnauthorized)
  }

}
