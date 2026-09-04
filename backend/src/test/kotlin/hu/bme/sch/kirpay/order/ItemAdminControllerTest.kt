package hu.bme.sch.kirpay.order

import jakarta.servlet.http.Cookie
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class ItemAdminControllerTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  @Autowired
  private lateinit var springSessionRepositoryFilter: SessionRepositoryFilter<*>

  @Autowired
  private lateinit var itemRepository: ItemRepository

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

  private fun importItems(csv: String) =
    mockMvc.perform(
      post("/v1/api/admin/import/items?idempotencyKey=${UUID.randomUUID()}")
        .cookie(adminAuth())
        .contentType(MediaType.TEXT_PLAIN_VALUE)
        .content(csv)
    )

  @Test
  fun `import items valid csv returns 201 and persists`() {
    importItems("name,alias,cost,stock,enabled\nBoard,,100,5,true")
      .andExpect(status().isCreated)
    assertEquals(1, itemRepository.count())
    assertEquals("Board", itemRepository.findByEnabledOrderByName(true)[0].name)
  }

  @Test
  fun `import items with blank name returns 400 and lists the row`() {
    // Response body is rendered by Tomcat's error dispatch (not simulated by MockMvc);
    // the message content is asserted in CsvParserTest and by the e2e csv specs.
    importItems("name,alias,cost,stock,enabled\nBoard,,100,5,true\n,,200,6,true")
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `import items with misplaced value returns 400`() {
    importItems("name,alias,cost,stock,enabled\nBoard,,100,5,abc")
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `import items violating db constraints returns 400 and imports nothing`() {
    importItems("name,alias,cost,stock,enabled\nBoard,,-100,5,true")
      .andExpect(status().isBadRequest)

    // End the test transaction so the failed batch is rolled back; verify against fresh DB state.
    TestTransaction.end()
    try {
      assertEquals(0, itemRepository.count())
    } finally {
      TestTransaction.start()
    }
  }

  @Test
  fun `anonymous gets 401 on import items`() {
    mockMvc.perform(
      post("/v1/api/admin/import/items?idempotencyKey=${UUID.randomUUID()}")
        .contentType(MediaType.TEXT_PLAIN_VALUE)
        .content("name,alias,cost,stock,enabled\nBoard,,100,5,true")
    ).andExpect(status().isUnauthorized)
  }

}
