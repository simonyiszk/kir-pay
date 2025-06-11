package hu.bme.sch.kirpay.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.stereotype.Component
import java.io.StringWriter
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor


@Component
class CsvParserFactory {

  fun <T : Any> getParserForType(type: KClass<T>): CsvParser<T> = CsvParser(type)

}


class CsvParser<T : Any>(private val type: KClass<T>) {
  private val mapper = CsvMapper()
    .disable(com.fasterxml.jackson.dataformat.csv.CsvParser.Feature.FAIL_ON_MISSING_HEADER_COLUMNS)
    .disable(com.fasterxml.jackson.dataformat.csv.CsvParser.Feature.FAIL_ON_MISSING_COLUMNS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .enable(JsonGenerator.Feature.IGNORE_UNKNOWN)
    .registerModule(SimpleModule().also {
      it.addDeserializer(
        String::class.java,
        AppJsonComponent.BlankToNullStringDeserializer()
      )
    })
    .registerKotlinModule()

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

  private val writer by lazy { mapper.writerFor(object : TypeReference<T>() {}).with(schema) }

  private val reader by lazy { mapper.readerFor(type.java).with(schema) }


  fun toCsv(data: List<T>): String = StringWriter().also { writer.writeValue(it, data) }.toString()


  fun fromCsv(csv: String): List<T> = reader.readValues<T>(csv).readAll()

}
