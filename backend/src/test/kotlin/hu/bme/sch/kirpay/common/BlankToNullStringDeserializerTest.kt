package hu.bme.sch.kirpay.common

import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlankToNullStringDeserializerTest {

  private val mapper = JsonMapper.builder()
    .addModule(SimpleModule().apply {
      addDeserializer(String::class.java, AppJsonComponent.BlankToNullStringDeserializer())
    })
    .build()

  data class TestDto(val name: String?, val email: String?)

  @Test
  fun `blank strings are deserialized to null`() {
    val json = """{"name":"  ","email":"actual@email.com"}"""
    val result = mapper.readValue(json, TestDto::class.java)
    assertNull(result.name)
    assertEquals("actual@email.com", result.email)
  }

  @Test
  fun `empty strings are deserialized to null`() {
    val json = """{"name":"","email":"test@test.com"}"""
    val result = mapper.readValue(json, TestDto::class.java)
    assertNull(result.name)
    assertEquals("test@test.com", result.email)
  }

  @Test
  fun `non-blank strings are preserved`() {
    val json = """{"name":"John","email":"john@test.com"}"""
    val result = mapper.readValue(json, TestDto::class.java)
    assertEquals("John", result.name)
    assertEquals("john@test.com", result.email)
  }

  @Test
  fun `null values remain null`() {
    val json = """{"name":null,"email":"e@e.com"}"""
    val result = mapper.readValue(json, TestDto::class.java)
    assertNull(result.name)
    assertEquals("e@e.com", result.email)
  }

  @Test
  fun `whitespace-only strings become null`() {
    val json = """{"name":"\t\n ","email":"e@e.com"}"""
    val result = mapper.readValue(json, TestDto::class.java)
    assertNull(result.name)
  }
}
