package hu.bme.sch.kirpay.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConstantsTest {

  @Test
  fun `same inputs produce the same fingerprint`() {
    assertEquals(buildFingerprint("PAY", "1234", 500L), buildFingerprint("PAY", "1234", 500L))
    assertEquals(buildFingerprint("ACCOUNT_IMPORT", "alma\nkorte,1"),
      buildFingerprint("ACCOUNT_IMPORT", "alma\nkorte,1"))
  }

  @Test
  fun `null and empty string are distinct parts`() {
    assertNotEquals(buildFingerprint("PAY", "1234", null), buildFingerprint("PAY", "1234", ""))
  }

  @Test
  fun `null and the literal string null are distinct parts`() {
    assertNotEquals(buildFingerprint("PAY", "1234", null), buildFingerprint("PAY", "1234", "null"))
  }

  @Test
  fun `a pipe inside a value does not collide with a split tuple`() {
    assertNotEquals(buildFingerprint("PAY", "a|b"), buildFingerprint("PAY", "a", "b"))
    assertNotEquals(buildFingerprint("PAY", "a|b", "c"), buildFingerprint("PAY", "a", "b|c"))
  }

  @Test
  fun `quotes and backslashes inside values are escaped`() {
    assertNotEquals(buildFingerprint("PAY", "a\"b"), buildFingerprint("PAY", "a", "b"))
    assertNotEquals(buildFingerprint("PAY", "a\\b"), buildFingerprint("PAY", "a", "b"))
  }

  @Test
  fun `different operation types never collide on equal parts`() {
    assertNotEquals(buildFingerprint("PAY", "1234", 500L), buildFingerprint("UPLOAD", "1234", 500L))
  }

  @Test
  fun `different arity never collides`() {
    assertNotEquals(buildFingerprint("PAY", "1234"), buildFingerprint("PAY", "1234", "x"))
  }

  @Test
  fun `number and numeric string are distinct parts`() {
    assertNotEquals(buildFingerprint("PAY", 1), buildFingerprint("PAY", "1"))
  }
}
