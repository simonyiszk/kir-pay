package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.common.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(ADMIN_API)
class ItemAdminController(
  parserFactory: CsvParserFactory,
  private val itemService: ItemService
) {
  private val itemParser = parserFactory.getParserForType(Item::class)


  @GetMapping("/items")
  fun getItemsPaginated(@RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?) =
    if (page == null && size == null)
      itemService.findAll()
    else
      itemService.findPaginated(page ?: DEFAULT_PAGE, size ?: DEFAULT_PAGE_SIZE)


  @GetMapping("/items/{itemId}")
  fun getItemsPaginated(@PathVariable itemId: Int) = itemService.find(itemId)


  @PostMapping("/items/{itemId}/disable")
  fun disableItem(@PathVariable itemId: Int) = itemService.setEnabled(itemId, false)


  @PostMapping("/items/{itemId}/enable")
  fun enableItem(@PathVariable itemId: Int) = itemService.setEnabled(itemId, true)


  @PostMapping("/items")
  fun createItem(@RequestBody item: Item) = itemService.createItem(item)


  @PostMapping("/items/{itemId}")
  fun updateItem(@PathVariable itemId: Int, @RequestBody item: Item) = itemService.updateItem(itemId, item)


  @DeleteMapping("/items/{itemId}")
  fun deleteItem(@PathVariable itemId: Int) = itemService.deleteItem(itemId)


  @GetMapping("/export/items", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun exportItems(): ResponseEntity<String> {
    val orderLines = itemService.findAll()
    return ResponseEntity.ok()
      .asFileAttachment("items.csv")
      .body(itemParser.toCsv(orderLines))
  }


  @PostMapping("/import/items", consumes = [MediaType.TEXT_PLAIN_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  fun importItems(@RequestBody csv: String) {
    val items = itemParser.fromCsv(csv)
    itemService.importItems(items)
  }


  @GetMapping("/template/items", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun itemExportTemplate(): ResponseEntity<String> {
    val items: List<Item> = listOf()
    return ResponseEntity.ok()
      .asFileAttachment("items-template.csv")
      .body(itemParser.toCsv(items))
  }

}
