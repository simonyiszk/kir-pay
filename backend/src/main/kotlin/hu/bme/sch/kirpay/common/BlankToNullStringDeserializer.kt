package hu.bme.sch.kirpay.common

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.std.StringDeserializer
import org.springframework.boot.jackson.JsonComponent

@JsonComponent
class AppJsonComponent {

  class BlankToNullStringDeserializer : JsonDeserializer<String>() {

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): String? {
      val result = StringDeserializer.instance.deserialize(parser, context)
      if (result.isNullOrBlank()) return null
      return result
    }

  }

}
