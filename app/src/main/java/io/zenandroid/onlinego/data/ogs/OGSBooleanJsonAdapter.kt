package io.zenandroid.onlinego.data.ogs

import com.squareup.moshi.*

class OGSBooleanJsonAdapter : JsonAdapter<Boolean?>() {
    override fun fromJson(reader: JsonReader): Boolean? {
        return when (reader.peek()) {
            JsonReader.Token.NUMBER -> reader.nextInt() != 0
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.STRING -> reader.nextString().equals("true", true)
            JsonReader.Token.NULL -> reader.nextNull<Boolean>()
            else -> throw JsonEncodingException("Error trying to parse `${reader.peek()}` as boolean at ${reader.path}")
        }
    }

    override fun toJson(writer: JsonWriter, value: Boolean?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.toString())
        }
    }
}