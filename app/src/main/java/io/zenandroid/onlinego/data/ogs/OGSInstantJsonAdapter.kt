package io.zenandroid.onlinego.data.ogs

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class OGSInstantJsonAdapter : JsonAdapter<Instant>() {
    override fun fromJson(reader: JsonReader): Instant? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull<Instant>()
        }
        val string = reader.nextString()
        return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(string))
    }

    override fun toJson(writer: JsonWriter, value: Instant?) {
        if (value == null) {
            writer.nullValue()
        } else {
            val string = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withLocale( Locale.US ).withZone( ZoneId.of("America/New_York") ).format(value)
            writer.value(string)
        }
    }
}