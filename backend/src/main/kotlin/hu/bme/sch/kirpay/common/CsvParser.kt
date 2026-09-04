package hu.bme.sch.kirpay.common

import org.springframework.stereotype.Component
import tools.jackson.core.JacksonException
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.exc.InvalidNullException
import tools.jackson.databind.exc.MismatchedInputException
import tools.jackson.databind.exc.PropertyBindingException
import tools.jackson.databind.module.SimpleModule
import tools.jackson.dataformat.csv.CsvMapper
import tools.jackson.dataformat.csv.CsvReadFeature
import tools.jackson.dataformat.csv.CsvSchema
import tools.jackson.module.kotlin.KotlinModule
import java.io.StringWriter
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

private const val MAX_REPORTED_ERRORS = 10
private const val BOM = "﻿"

@Component
class CsvParserFactory {
  fun <T : Any> getParserForType(type: KClass<T>): CsvParser<T> = CsvParser(type)
}

class CsvParser<T : Any>(private val type: KClass<T>) {
  private val mapper = CsvMapper.builder()
    .disable(CsvReadFeature.FAIL_ON_MISSING_HEADER_COLUMNS)
    .disable(CsvReadFeature.FAIL_ON_MISSING_COLUMNS)
    .addModule(KotlinModule.Builder().build())
    .addModule(SimpleModule().addDeserializer(String::class.java, AppJsonComponent.BlankToNullStringDeserializer()))
    .build()

  private val schema: CsvSchema by lazy {
    val schemaBuilder = CsvSchema.builder()
    val constructor = type.primaryConstructor
      ?: throw IllegalArgumentException("Only types with primary constructors can be read and written to CSV")

    constructor.parameters.forEach { schemaBuilder.addColumn(it.name) }
    schemaBuilder.setStrictHeaders(false)
    schemaBuilder.setUseHeader(true)
    schemaBuilder.setReorderColumns(true)

    return@lazy schemaBuilder.build()
      .withColumnSeparator(',')
      .withEscapeChar('\\')
  }

  private val writer by lazy { mapper.writerFor(object : TypeReference<List<T>>() {}).with(schema) }

  private val reader by lazy { mapper.readerFor(type.java).with(schema) }

  fun toCsv(data: List<T>): String = StringWriter().also { writer.writeValue(it, data) }.toString()

  fun fromCsv(csv: String): List<T> {
    if (csv.isBlank()) return emptyList()
    val iterator = reader.readValues<T>(csv.removePrefix(BOM))
    val result = mutableListOf<T>()
    val errors = mutableListOf<String>()
    var row = 0
    while (true) {
      row++
      val hasNext = try {
        iterator.hasNext()
      } catch (e: JacksonException) {
        errors.add("Sor $row: ${describeRowError(e)}")
        break
      }
      if (!hasNext) break
      try {
        result.add(iterator.next())
      } catch (e: JacksonException) {
        errors.add("Sor $row: ${describeRowError(e)}")
      }
    }
    if (errors.isNotEmpty()) {
      val suffix = if (errors.size > MAX_REPORTED_ERRORS)
        "\n… és további ${errors.size - MAX_REPORTED_ERRORS} hiba." else ""
      throw BadRequestException(
        "A CSV fájl érvénytelen:\n" + errors.take(MAX_REPORTED_ERRORS).joinToString("\n") + suffix
      )
    }
    return result
  }

  private fun describeRowError(e: JacksonException): String {
    val property = when (e) {
      is InvalidNullException -> e.propertyName?.simpleName
      is MismatchedInputException -> propertyFromPath(e)
      is PropertyBindingException -> e.propertyName
      else -> null
    }
    val problem = when (e) {
      is InvalidNullException -> "üresen maradt egy kötelező mező"
      is MismatchedInputException -> "érvénytelen értéket tartalmaz"
      else -> "hibás sor"
    }
    return if (property != null) "a(z) \"$property\" mező $problem" else problem
  }

  private fun propertyFromPath(e: JacksonException): String? {
    val match = Regex("""\["([^"]+)"\]\s*\)?\s*$""").find(e.message ?: return null)
    return match?.groupValues?.get(1)
  }

}
