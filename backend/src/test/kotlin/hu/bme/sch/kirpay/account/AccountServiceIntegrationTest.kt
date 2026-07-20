package hu.bme.sch.kirpay.account

import hu.bme.sch.kirpay.BaseIntegrationTest
import hu.bme.sch.kirpay.common.NotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.*

class AccountServiceIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var accountService: AccountService

  // --- create ---

  @Test
  fun `create account persists correctly`() {
    val dto = AccountCreateDto(id = null,
      name = "Alice",
      email = "alice@test.com",
      phone = null,
      card = "CARD-A",
      balance = 100,
      active = true)
    val created = accountService.create(dto)

    assertNotNull(created.id)
    assertEquals("Alice", created.name)
    assertEquals(100L, created.balance)

    val found = accountService.find(created.id!!)
    assertEquals("Alice", found.name)
  }

  @Test
  fun `create account with client-supplied id creates new account with different id`() {
    val first = accountService.create(AccountCreateDto(id = null,
      name = "First",
      email = null,
      phone = null,
      card = "CARD-1",
      balance = 500,
      active = true))
    val firstId = first.id!!

    // Create another with the same id — should NOT overwrite first
    val second = accountService.create(AccountCreateDto(id = firstId,
      name = "Second",
      email = null,
      phone = null,
      card = "CARD-2",
      balance = 300,
      active = true))

    // Reload first
    val firstReloaded = accountService.find(firstId)
    assertEquals("First", firstReloaded.name, "BUG-01: First account should NOT be overwritten")
    assertEquals(500L, firstReloaded.balance, "BUG-01: First account balance preserved")

    // Second should have a different id
    assertNotEquals(firstId, second.id, "Second account should have a new id")
  }

  // --- findActiveByCard ---

  @Test
  fun `findActiveByCard returns account`() {
    createAccount(card = "CARD-FIND", name = "Finder")
    val found = accountService.findActiveByCard("CARD-FIND")
    assertEquals("Finder", found.name)
  }

  @Test
  fun `findActiveByCard throws for unknown card`() {
    assertThrows<NotFoundException> { accountService.findActiveByCard("NONEXISTENT") }
  }

  // --- assignCard ---

  @Test
  fun `assignCard moves card from previous holder`() {
    val acc1 = createAccount(card = "SHARED", name = "Owner1")
    val acc2 = createAccount(card = null, name = "Owner2")

    val result = accountService.assignCard(acc2.id!!, "SHARED")

    assertEquals("SHARED", result.card)

    // Previous holder should have card=null
    val prev = accountService.find(acc1.id!!)
    assertNull(prev.card, "Previous holder card should be cleared")
  }

  // --- delete ---

  @Test
  fun `delete account with zero balance succeeds`() {
    val acc = createAccount(balance = 0, card = null)
    accountService.deleteAccount(acc.id!!)
    assertThrows<NotFoundException> { accountService.find(acc.id!!) }
  }

  // --- update ---

  @Test
  fun `update account changes name`() {
    val acc = createAccount(card = "CARD-UPDATE", name = "Old Name")
    val dto = AccountUpdateDto(name = "New Name", email = null, phone = null, card = null, active = true)

    val updated = accountService.update(acc.id!!, dto)

    assertEquals("New Name", updated.name)
    assertEquals(5000L, updated.balance)
  }

  @Test
  fun `setEnabled disables account`() {
    val acc = createAccount(card = "CARD-DISABLE")
    val result = accountService.setEnabled(acc.id!!, false)
    assertFalse(result.active)
  }

  // --- findAll ---

  @Test
  fun `findAll returns all accounts ordered by name`() {
    createAccount(card = "CARD-Z", name = "Zebra")
    createAccount(card = "CARD-A", name = "Apple")
    createAccount(card = "CARD-M", name = "Mango")

    val all = accountService.findAll()
    assertEquals("Apple", all[0].name)
    assertEquals("Mango", all[1].name)
    assertEquals("Zebra", all[2].name)
  }

  // --- count + balance ---

  @Test
  fun `countAll and getAllActiveBalance work`() {
    createAccount(card = "CARD-C1", balance = 100)
    createAccount(card = "CARD-C2", balance = 200)

    assertEquals(2, accountService.countAll())
    assertEquals(300, accountService.getAllActiveBalance())
  }
}
