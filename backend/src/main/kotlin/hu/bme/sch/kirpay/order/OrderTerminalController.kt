package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.common.TERMINAL_API
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(TERMINAL_API)
class OrderTerminalController(
  private val itemService: ItemService,
  private val orderService: OrderService
) {

  @GetMapping("/items")
  fun getAllItems() = itemService.findAllActive()


  data class OrderLineDto(
    val itemId: Int?,
    @field:Min(0) val itemCount: Int,
    val usedVoucher: Boolean,
    val message: String?,
    val paidAmount: Long?
  )

  data class CheckoutDto(@field:Size(min = 1) val orderLines: List<@Valid @NotNull OrderLineDto>)

  @PostMapping("/account-by-card/{card}/checkout")
  fun checkout(
    @PathVariable card: String,
    @Valid @RequestBody dto: CheckoutDto
  ) = orderService.checkout(card, dto)

}
