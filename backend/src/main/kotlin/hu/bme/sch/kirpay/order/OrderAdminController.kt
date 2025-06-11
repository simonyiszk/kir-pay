package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.common.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(ADMIN_API)
class OrderAdminController(
  parserFactory: CsvParserFactory,
  private val orderService: OrderService,
  private val orderLineService: OrderLineService,
  private val voucherService: VoucherService,
) {
  private val orderParser = parserFactory.getParserForType(Order::class)
  private val orderLineParser = parserFactory.getParserForType(OrderLine::class)
  private val orderWithOrderLineParser = parserFactory.getParserForType(OrderWithOrderLine::class)
  private val voucherParser = parserFactory.getParserForType(VoucherDto::class)


  @GetMapping("/orders")
  fun getOrdersPaginated(@RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?) =
    if (page == null && size == null)
      orderService.findAll()
    else
      orderService.findPaginated(page ?: DEFAULT_PAGE, size ?: DEFAULT_PAGE_SIZE)


  @GetMapping("/export/orders", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun exportOrders(): ResponseEntity<String> {
    val orders = orderService.findAll()
    return ResponseEntity.ok()
      .asFileAttachment("orders.csv")
      .body(orderParser.toCsv(orders))
  }


  @GetMapping("/orders-with-order-lines")
  fun getOrdersWithOrderLinesPaginated(
    @RequestParam(required = false) page: Int?,
    @RequestParam(required = false) size: Int?
  ) = if (page == null && size == null)
    orderService.findAllOrdersWithOrderLines()
  else
    orderService.findAllOrdersWithOrderLinesPaginated(page ?: DEFAULT_PAGE, size ?: DEFAULT_PAGE_SIZE)


  @GetMapping("/export/orders-with-order-lines", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun exportOrdersWithOrderLinesPaginated(): ResponseEntity<String> {
    val ordersWithOrderLines = orderService.findAllOrdersWithOrderLines()
    return ResponseEntity.ok()
      .asFileAttachment("orders-with-order-lines.csv")
      .body(orderWithOrderLineParser.toCsv(ordersWithOrderLines))
  }


  @GetMapping("/order_lines")
  fun getOrderLinesPaginated(@RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?) =
    if (page == null && size == null)
      orderLineService.findAll()
    else
      orderLineService.findPaginated(page ?: DEFAULT_PAGE, size ?: DEFAULT_PAGE_SIZE)


  @GetMapping("/export/order_lines", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun exportOrderLines(): ResponseEntity<String> {
    val orderLines = orderLineService.findAll()
    return ResponseEntity.ok()
      .asFileAttachment("order-lines.csv")
      .body(orderLineParser.toCsv(orderLines))
  }


  @GetMapping("/vouchers")
  fun getVouchersPaginated(@RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?) =
    if (page == null && size == null)
      voucherService.findAll()
    else
      voucherService.findPaginated(page ?: DEFAULT_PAGE, size ?: DEFAULT_PAGE_SIZE)


  @DeleteMapping("/vouchers/{voucherId}")
  fun deleteVoucher(@PathVariable voucherId: Int) = voucherService.delete(voucherId)


  // NotNull is only applied when parsing
  data class VoucherDto(@field:NotNull val accountId: Int?, val itemId: Int, @field:Min(0) val count: Int)

  @PostMapping("/vouchers")
  fun createVoucher(@Valid @RequestBody voucher: VoucherDto) = voucherService.saveVoucher(voucher)


  data class VoucherBatchDto(@field:Min(0) val count: Int, val accounts: List<Int>)

  @PostMapping("/items/{itemId}/voucher")
  fun createBatchVoucher(@Valid @RequestBody dto: VoucherBatchDto, @PathVariable itemId: Int) =
    voucherService.createBatchVoucher(itemId, dto)


  data class VoucherCountDto(@field:Min(0) val count: Int)

  @PostMapping("/vouchers/{voucherId}/count")
  fun updateVoucherCount(@PathVariable voucherId: Int, @Valid @RequestBody dto: VoucherCountDto) =
    voucherService.updateVoucherCount(voucherId, dto.count)


  @GetMapping("/export/vouchers", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun exportVouchers(): ResponseEntity<String> {
    val vouchers = voucherService.findAll()
    return ResponseEntity.ok()
      .asFileAttachment("vouchers.csv")
      .body(voucherParser.toCsv(vouchers.map {
        VoucherDto(accountId = it.accountId, itemId = it.itemId, count = it.count)
      }))
  }


  @PostMapping("/import/vouchers", consumes = [MediaType.TEXT_PLAIN_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  fun importVouchers(@RequestBody csv: String) {
    val vouchers = voucherParser.fromCsv(csv)
    voucherService.importVouchers(vouchers)
  }


  @GetMapping("/template/vouchers", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun voucherExportTemplate(): ResponseEntity<String> {
    val vouchers: List<VoucherDto> = listOf()
    return ResponseEntity.ok()
      .asFileAttachment("vouchers-template.csv")
      .body(voucherParser.toCsv(vouchers))
  }

}
