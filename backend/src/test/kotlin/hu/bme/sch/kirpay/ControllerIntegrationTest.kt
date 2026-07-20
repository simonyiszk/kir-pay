package hu.bme.sch.kirpay

import hu.bme.sch.kirpay.account.*
import hu.bme.sch.kirpay.common.CsvParserFactory
import hu.bme.sch.kirpay.event.Event
import hu.bme.sch.kirpay.event.EventRepository
import hu.bme.sch.kirpay.event.EventService
import hu.bme.sch.kirpay.order.*
import hu.bme.sch.kirpay.principal.PrincipalDto
import hu.bme.sch.kirpay.principal.PrincipalService
import hu.bme.sch.kirpay.principal.Role
import hu.bme.sch.kirpay.transaction.Transaction
import hu.bme.sch.kirpay.transaction.TransactionRepository
import hu.bme.sch.kirpay.transaction.TransactionService
import hu.bme.sch.kirpay.transaction.TransactionType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Controller integration tests — tests controllers through their service layer.
 * Uses real services with PostgreSQL via TestContainers for full coverage.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ControllerIntegrationTest {

  private val encoder = BCryptPasswordEncoder()

  @BeforeEach
  fun setUpAuth() {
    val principal = hu.bme.sch.kirpay.principal.Principal(
      id = null, name = "test-terminal", secret = encoder.encode("test-pw")!!,
      role = hu.bme.sch.kirpay.principal.Role.TERMINAL, active = true,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis()
    )
    val authorities = mutableListOf<SimpleGrantedAuthority>()
    authorities.add(SimpleGrantedAuthority("ROLE_${principal.role.name}"))
    hu.bme.sch.kirpay.principal.Permission.entries.forEach { perm ->
      authorities.add(SimpleGrantedAuthority(perm.name))
    }
    val auth = UsernamePasswordAuthenticationToken(principal, "test-pw", authorities)
    SecurityContextHolder.getContext().authentication = auth
  }

  @AfterEach
  fun clearAuth() {
    SecurityContextHolder.clearContext()
  }

  @Autowired
  lateinit var accountService: AccountService
  @Autowired
  lateinit var balanceService: AccountBalanceService
  @Autowired
  lateinit var orderService: OrderService
  @Autowired
  lateinit var itemService: ItemService
  @Autowired
  lateinit var voucherService: VoucherService
  @Autowired
  lateinit var orderLineService: OrderLineService
  @Autowired
  lateinit var eventService: EventService
  @Autowired
  lateinit var transactionService: TransactionService
  @Autowired
  lateinit var principalService: PrincipalService
  @Autowired
  lateinit var accountRepository: AccountRepository
  @Autowired
  lateinit var itemRepository: ItemRepository
  @Autowired
  lateinit var transactionRepository: TransactionRepository
  @Autowired
  lateinit var eventRepository: EventRepository
  @Autowired
  lateinit var parserFactory: CsvParserFactory

  // === CSV Import/Export ===

  @Test
  fun `Account CSV export roundtrip`() {
    accountService.create(AccountCreateDto(id = null,
      name = "Alice",
      email = "a@b.com",
      phone = null,
      card = "CSV-1",
      balance = 100,
      active = true))
    accountService.create(AccountCreateDto(id = null,
      name = "Bob",
      email = null,
      phone = null,
      card = "CSV-2",
      balance = 200,
      active = false))

    val all = accountService.findAll()
    val parser = parserFactory.getParserForType(Account::class)
    val csv = parser.toCsv(all)

    assertTrue(csv.contains("Alice"))
    assertTrue(csv.contains("Bob"))
    assertTrue(csv.contains("CSV-1"))

    // Roundtrip
    val reparsed = parser.fromCsv(csv)
    assertTrue(reparsed.size >= 2)
  }

  @Test
  fun `Account CSV import`() {
    val csv = "id,name,email,phone,card,balance,active\n,Carol,c@b.com,,CSV-3,300,true\n"
    val parser = parserFactory.getParserForType(Account::class)
    val accounts = parser.fromCsv(csv)

    accountService.importAccounts(accounts)

    val all = accountService.findAll()
    assertTrue(all.any { it.name == "Carol" && it.card == "CSV-3" })
  }

  @Test
  fun `OrderLine CSV export`() {
    val account = accountService.create(AccountCreateDto(id = null,
      name = "Exp",
      email = null,
      phone = null,
      card = "EXP-1",
      balance = 500,
      active = true))
    val item = itemRepository.save(Item(id = null,
      name = "ExportItem",
      alias = null,
      cost = 50,
      stock = 100,
      enabled = true,
      showOnLeaderboard = false))

    val dto = OrderTerminalController.CheckoutDto(orderLines = listOf(
      OrderTerminalController.OrderLineDto(itemId = item.id,
        itemCount = 3,
        usedVoucher = false,
        message = "CSV test",
        paidAmount = null)
    ))
    orderService.checkout("EXP-1", dto)

    val lines = orderLineService.findAll()
    val parser = parserFactory.getParserForType(OrderLine::class)
    val csv = parser.toCsv(lines)

    assertTrue(csv.contains("CSV test"))
    assertTrue(lines.any { it.paidAmount == 150L })
  }

  @Test
  fun `Event CSV export`() {
    eventService.create("TEST_EXPORT", "export message", "tester", System.currentTimeMillis())
    eventService.create("TEST_EXPORT2", "another", "tester", System.currentTimeMillis())

    val all = eventService.findAll()
    val parser = parserFactory.getParserForType(Event::class)
    val csv = parser.toCsv(all)

    assertTrue(csv.contains("TEST_EXPORT"))
    assertTrue(csv.contains("export message"))
  }

  @Test
  fun `Transaction CSV export`() {
    val account = accountService.create(AccountCreateDto(id = null,
      name = "Tx",
      email = null,
      phone = null,
      card = "TX-EXP",
      balance = 1000,
      active = true))
    transactionRepository.save(Transaction(id = null,
      type = TransactionType.CHARGE,
      senderId = account.id,
      recipientId = null,
      amount = 500,
      message = "Test tx",
      timestamp = System.currentTimeMillis()))

    val all = transactionService.findAll()
    val parser = parserFactory.getParserForType(Transaction::class)
    val csv = parser.toCsv(all)

    assertTrue(csv.contains("CHARGE"))
    assertTrue(csv.contains("500"))
  }

  @Test
  fun `VoucherDto CSV roundtrip`() {
    val account = accountService.create(AccountCreateDto(id = null,
      name = "V",
      email = null,
      phone = null,
      card = "V-EXP",
      balance = 0,
      active = true))
    val item = itemRepository.save(Item(id = null,
      name = "VItem",
      alias = null,
      cost = 10,
      stock = 100,
      enabled = true,
      showOnLeaderboard = false))

    voucherService.saveVoucher(OrderAdminController.VoucherDto(accountId = account.id, itemId = item.id!!, count = 5))

    val vouchers = voucherService.findAll()
    val parser = parserFactory.getParserForType(OrderAdminController.VoucherDto::class)
    val dtos =
      vouchers.map { OrderAdminController.VoucherDto(accountId = it.accountId, itemId = it.itemId, count = it.count) }
    val csv = parser.toCsv(dtos)

    assertTrue(csv.contains("5"))

    val reparsed = parser.fromCsv(csv)
    assertEquals(1, reparsed.size)
  }

  @Test
  fun `PrincipalDto CSV roundtrip`() {
    principalService.createPrincipal(
      PrincipalDto(name = "csv-principal", password = "pw", role = Role.TERMINAL,
        canUpload = true, canTransfer = false, canSellItems = true,
        canRedeemVouchers = false, canAssignCards = false, active = true)
    )

    val parser = parserFactory.getParserForType(PrincipalDto::class)
    val dtos = listOf(PrincipalDto(name = "csv-p2", password = "pw2", role = Role.ADMIN,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true, active = true))
    val csv = parser.toCsv(dtos)
    val reparsed = parser.fromCsv(csv)

    assertEquals(1, reparsed.size)
    assertEquals("csv-p2", reparsed[0].name)
    assertEquals(Role.ADMIN, reparsed[0].role)
  }

  // === Analytics ===

  @Test
  fun `analytics returns correct counts`() {
    accountService.create(AccountCreateDto(id = null,
      name = "A1",
      email = null,
      phone = null,
      card = "ANA-1",
      balance = 100,
      active = true))
    accountService.create(AccountCreateDto(id = null,
      name = "A2",
      email = null,
      phone = null,
      card = "ANA-2",
      balance = 200,
      active = true))

    assertEquals(2, accountService.countAll())
    assertEquals(300, accountService.getAllActiveBalance())
  }

  @Test
  fun `transaction analytics work`() {
    val account = accountService.create(AccountCreateDto(id = null,
      name = "TA",
      email = null,
      phone = null,
      card = "TA-CARD",
      balance = 1000,
      active = true))
    transactionRepository.save(Transaction(id = null,
      type = TransactionType.TOP_UP,
      senderId = null,
      recipientId = account.id,
      amount = 500,
      message = null,
      timestamp = System.currentTimeMillis()))
    transactionRepository.save(Transaction(id = null,
      type = TransactionType.CHARGE,
      senderId = account.id,
      recipientId = null,
      amount = 200,
      message = null,
      timestamp = System.currentTimeMillis()))

    assertEquals(2, transactionService.countAll())
    assertEquals(200, transactionService.getIncome())
    assertEquals(500, transactionService.getAllUploads())
    assertEquals(700, transactionService.getTransactionVolume())
  }

  // === Pagination ===

  @Test
  fun `events pagination works`() {
    // Delete any pre-existing events from other tests in this class
    val existingCount = eventService.findAll().size
    val ts = System.currentTimeMillis()
    repeat(5) { eventService.create("PAGE_UNIQUE", "msg$it", "user", ts + it) }

    val page0 = eventService.findPaginated(0, 2)
    val page1 = eventService.findPaginated(1, 2)

    assertEquals(2, page0.size)
    assertEquals(2, page1.size)
    // Verify no duplicates
    val page0Ids = page0.map { it.id }.toSet()
    val page1Ids = page1.map { it.id }.toSet()
    assertTrue(page0Ids.intersect(page1Ids).isEmpty(), "Pages should not overlap")
  }

  @Test
  fun `transactions pagination works`() {
    val account = accountService.create(AccountCreateDto(id = null,
      name = "P",
      email = null,
      phone = null,
      card = "P-CARD",
      balance = 5000,
      active = true))
    repeat(5) { i ->
      transactionRepository.save(Transaction(id = null,
        type = TransactionType.CHARGE,
        senderId = account.id,
        recipientId = null,
        amount = (i + 1) * 10L,
        message = null,
        timestamp = System.currentTimeMillis() + i))
    }

    val page = transactionService.findPaginated(0, 3)
    assertEquals(3, page.size)
  }
}
