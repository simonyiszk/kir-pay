package hu.bme.sch.kirpay.common


const val ADMIN_API = "/v1/api/admin"
const val TERMINAL_API = "/v1/api/terminal"
const val APP_ENDPOINT = "/v1/api/app"

const val DEFAULT_PAGE = 0
const val DEFAULT_PAGE_SIZE = 50
const val MAX_PAGE_SIZE = 500

fun requireValidPagination(page: Int, size: Int) {
    if (page < 0) throw BadRequestException("Az oldalszám nem lehet negatív!")
    if (size < 1) throw BadRequestException("A lapméret legalább 1 kell, hogy legyen!")
    if (size > MAX_PAGE_SIZE) throw BadRequestException("A lapméret legfeljebb $MAX_PAGE_SIZE lehet!")
}
