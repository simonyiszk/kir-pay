package hu.bme.sch.kirpay.common

import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.order.Item
import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CsvParserTest {

  private val accountParser = CsvParser(Account::class)
  private val itemParser = CsvParser(Item::class)

  @Test
  fun `valid account csv parses`() {
    val csv = "name,email,phone,card,balance,active\nJohn,,,CARD-1,100,true"
    val accounts = accountParser.fromCsv(csv)
    assertEquals(1, accounts.size)
    assertEquals("John", accounts[0].name)
    assertNull(accounts[0].email)
    assertNull(accounts[0].phone)
    assertEquals("CARD-1", accounts[0].card)
    assertEquals(BigInteger.valueOf(100), accounts[0].balance)
    assertTrue(accounts[0].active)
  }

  @Test
  fun `extra header and data columns are tolerated`() {
    val csv = "name,email,phone,card,balance,active,extra\nJohn,,,CARD-1,100,true,EXTRA"
    val accounts = accountParser.fromCsv(csv)
    assertEquals(1, accounts.size)
    assertEquals("John", accounts[0].name)
  }

  @Test
  fun `row with extra cell beyond header is rejected with a row error`() {
    val csv = "name,email,phone,card,balance,active\nJohn,,,CARD-1,100,true,EXTRA"
    val e = assertFailsWith<BadRequestException> { accountParser.fromCsv(csv) }
    assertContains(e.message!!, "Sor 1")
  }

  @Test
  fun `export template round-trips`() {
    val account = Account(
      id = 1,
      name = "John",
      email = null,
      phone = null,
      card = null,
      balance = BigInteger.ZERO,
      active = true,
      version = 0
    )
    val csv = accountParser.toCsv(listOf(account))
    assertEquals(listOf(account), accountParser.fromCsv(csv))
  }

  @Test
  fun `blank required cell reports the row and property`() {
    val csv = "name,email,phone,card,balance,active\nJohn,,,CARD-1,100,true\n,,,CARD-2,200,true"
    val e = assertFailsWith<BadRequestException> { accountParser.fromCsv(csv) }
    assertContains(e.message!!, "Sor 2")
    assertContains(e.message!!, "name")
  }

  @Test
  fun `iterator continues after a failed row`() {
    val csv =
      "name,email,phone,card,balance,active\n,,,CARD-1,100,true\nJane,,,CARD-2,200,true\nBob,,,CARD-3,notanumber,true"
    val e = assertFailsWith<BadRequestException> { accountParser.fromCsv(csv) }
    assertContains(e.message!!, "Sor 1")
    assertContains(e.message!!, "Sor 3")
  }

  @Test
  fun `wrong-typed cell reports the row and property`() {
    val csv = "name,alias,cost,stock,enabled\nBoard,,100,5,100000"
    val e = assertFailsWith<BadRequestException> { itemParser.fromCsv(csv) }
    assertContains(e.message!!, "Sor 1")
    assertContains(e.message!!, "enabled")
  }

  @Test
  fun `empty csv parses to empty list`() {
    assertEquals(emptyList(), accountParser.fromCsv(""))
  }

  @Test
  fun `header only csv parses to empty list`() {
    assertEquals(emptyList(), accountParser.fromCsv("name,email,phone,card,balance,active"))
  }

  @Test
  fun `trailing newline after last row is tolerated`() {
    val csv = "name,email,phone,card,balance,active\nJohn,,,CARD-1,100,true\n"
    assertEquals(1, accountParser.fromCsv(csv).size)
  }

  @Test
  fun `multiple bad rows are aggregated and capped`() {
    val rows = (1..12).joinToString("\n") { ",,,CARD-$it,100,true" }
    val e = assertFailsWith<BadRequestException> { accountParser.fromCsv("name,email,phone,card,balance,active\n$rows") }
    assertContains(e.message!!, "Sor 1")
    assertContains(e.message!!, "Sor 10")
    assertContains(e.message!!, "további 2 hiba")
    assertTrue(!e.message!!.contains("Sor 11"))
  }

  @Test
  fun `blank optional fields stay null`() {
    val csv = "name,email,phone,card,balance,active\nJohn, , ,,100,true"
    val accounts = accountParser.fromCsv(csv)
    assertEquals(1, accounts.size)
    assertNull(accounts[0].email)
    assertNull(accounts[0].phone)
    assertNull(accounts[0].card)
  }

  @Test
  fun `bom prefixed csv parses`() {
    val csv = "﻿name,email,phone,card,balance,active\nJohn,,,CARD-1,100,true"
    assertEquals("John", accountParser.fromCsv(csv)[0].name)
  }

}
