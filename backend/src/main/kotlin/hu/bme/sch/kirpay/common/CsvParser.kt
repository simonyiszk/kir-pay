package hu.bme.sch.kirpay.common

import org.springframework.stereotype.Component
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.module.SimpleModule
import tools.jackson.dataformat.csv.CsvMapper
import tools.jackson.dataformat.csv.CsvReadFeature
import tools.jackson.dataformat.csv.CsvSchema
import tools.jackson.module.kotlin.KotlinModule
import java.io.StringWriter
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

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

  fun fromCsv(csv: String): List<T> = reader.readValues<T>(csv).readAll()

}
