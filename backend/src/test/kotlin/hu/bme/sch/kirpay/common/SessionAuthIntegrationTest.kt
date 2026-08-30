package hu.bme.sch.kirpay.common

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.session.web.http.SessionRepositoryFilter
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class SessionAuthIntegrationTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  @Autowired
  private lateinit var springSessionRepositoryFilter: SessionRepositoryFilter<*>

  @Autowired
  private lateinit var sessionMetadataFilter: SessionMetadataFilter

  private lateinit var mockMvc: MockMvc

  @BeforeEach
  fun setUp() {
    SecurityContextHolder.clearContext()
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
      .addFilter<DefaultMockMvcBuilder>(springSessionRepositoryFilter, "/*")
      .addFilter<DefaultMockMvcBuilder>(sessionMetadataFilter, "/*")
      .apply<DefaultMockMvcBuilder>(springSecurity())
      .build()
  }

  @AfterEach
  fun tearDown() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `http basic credentials are ignored`() {
    mockMvc.perform(
      get("/v1/api/app")
        .with(httpBasic("admin", "admin"))
    )
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `unauthenticated request returns 401`() {
    mockMvc.perform(get("/v1/api/app"))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `wrong password returns 401`() {
    mockMvc.perform(
      post("/v1/api/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"username":"admin","password":"wrong"}""")
    )
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `logout invalidates session`() {
    val result = mockMvc.perform(
      post("/v1/api/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"username":"admin","password":"admin"}""")
    )
      .andExpect(status().isNoContent)
      .andReturn()

    val sessionCookie = result.response.getCookie("SESSION")!!

    mockMvc.perform(post("/v1/api/logout").cookie(sessionCookie))
      .andExpect(status().isOk)

    mockMvc.perform(get("/v1/api/app").cookie(sessionCookie))
      .andExpect(status().isUnauthorized)
  }

  @Test
  fun `json login creates session cookie and persists security context`() {
    val result = mockMvc.perform(
      post("/v1/api/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"username":"admin","password":"admin"}""")
    )
      .andExpect(status().isNoContent)
      .andExpect(cookie().exists("SESSION"))
      .andReturn()

    val sessionCookie = result.response.getCookie("SESSION")!!

    mockMvc.perform(
      get("/v1/api/app")
        .cookie(sessionCookie)
    )
      .andExpect(status().isOk)
  }

  @Test
  fun `admin can view sessions with metadata`() {
    val result = mockMvc.perform(
      post("/v1/api/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"username":"admin","password":"admin"}""")
    )
      .andExpect(status().isNoContent)
      .andReturn()

    val sessionCookie = result.response.getCookie("SESSION")!!

    mockMvc.perform(
      get("/v1/api/admin/sessions")
        .cookie(sessionCookie)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").isNumber)
      .andExpect(jsonPath("$[0].ipAddress").isNotEmpty)
  }

}
