// Create new file: app/src/main/java/com/hrms/jeejateamozy/core/network/StringToListAdapter.kt

package com.hrms.jeejateamozy.core.network

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Custom Gson TypeAdapter to handle fields that may come as:
 * - An actual JSON array: ["file1.pdf", "file2.pdf"]
 * - A JSON-encoded string: "[]" or "[\"file1.pdf\"]"
 * - null
 */
class StringToListAdapter : JsonDeserializer<List<String>> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<String> {
        if (json == null || json.isJsonNull) {
            return emptyList()
        }

        return try {
            when {
                // Case 1: It's already a proper JSON array
                json.isJsonArray -> {
                    val result = mutableListOf<String>()
                    json.asJsonArray.forEach { element ->
                        if (element.isJsonPrimitive) {
                            result.add(element.asString)
                        }
                    }
                    result
                }

                // Case 2: It's a string (possibly JSON-encoded array like "[]" or "[\"file.pdf\"]")
                json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                    val stringValue = json.asString
                    if (stringValue.isBlank() || stringValue == "[]") {
                        emptyList()
                    } else if (stringValue.startsWith("[")) {
                        // Try to parse the string as JSON array
                        val innerGson = Gson()
                        val listType = object : TypeToken<List<String>>() {}.type
                        innerGson.fromJson(stringValue, listType) ?: emptyList()
                    } else {
                        // Single value, wrap in list
                        listOf(stringValue)
                    }
                }

                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}