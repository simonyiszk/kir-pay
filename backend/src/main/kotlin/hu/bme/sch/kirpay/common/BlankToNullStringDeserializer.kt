package hu.bme.sch.kirpay.common

import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.deser.jdk.StringDeserializer

@JacksonComponent
class AppJsonComponent {
  class BlankToNullStringDeserializer : ValueDeserializer<String>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): String? {
      val result = StringDeserializer.instance.deserialize(parser, context)
      if (result.isNullOrBlank()) return null
      return result
    }

  }

}
