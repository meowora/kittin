package cat.mona.kittin.compiler.utils

import kotlin.collections.component1
import kotlin.collections.component2

private fun StringBuilder.newLine() = append("\n")

interface JsonElement {
    fun _emit(builder: StringBuilder, depth: Int) = builder.emit(depth)
    fun StringBuilder.emit(depth: Int)

    fun dump() = StringBuilder().apply { emit(0) }.toString()
}

fun json(init: JsonObject.() -> Unit) = JsonObject().apply(init)

data class JsonObject(val map: MutableMap<String, JsonElement> = mutableMapOf()) : JsonElement {
    override fun StringBuilder.emit(depth: Int) {
        append("{").newLine()
        map.entries.forEachIndexed { index, (name, element) ->
            append("  ".repeat(depth + 1)).append('"').append(name).append('"').append(": ")
            element._emit(this, depth + 1)
            if (index + 1 < map.size) {
                append(",")
            }
            newLine()
        }
        append("  ".repeat(depth)).append("}")
    }

    operator fun String.invoke(init: JsonObject.() -> Unit) {
        map[this] = JsonObject().apply(init)
    }

    operator fun String.invoke(number: Number) {
        map[this] = JsonNumber(number)
    }

    operator fun String.invoke(boolean: Boolean) {
        map[this] = JsonBoolean(boolean)
    }

    operator fun String.invoke(string: String) {
        map[this] = JsonString(string)
    }

    operator fun String.invoke(element: JsonElement) {
        map[this] = element
    }
}

data class JsonArray(val elements: MutableList<JsonElement> = mutableListOf()) : JsonElement {
    constructor(elements: Collection<String>) : this(elements.map(::JsonString).toMutableList())

    override fun StringBuilder.emit(depth: Int) {
        append("[").newLine()
        elements.forEachIndexed { index, element ->
            append("  ".repeat(depth + 1))
            element._emit(this, depth + 1)
            if (index + 1 < elements.size) {
                append(",")
            }
            newLine()
        }
        append("  ".repeat(depth)).append("]")
    }
}

data object JsonNull : JsonElement {
    override fun StringBuilder.emit(depth: Int) {
        append("null")
    }
}


data class JsonString(val data: String) : JsonElement {
    override fun StringBuilder.emit(depth: Int) {
        append('"').append(data.replace("\\", "\\\\").replace("\"", "\\\"")).append('"')
    }
}

data class JsonNumber(val number: Number) : JsonElement {
    override fun StringBuilder.emit(depth: Int) {
        append(number)
    }
}

data class JsonBoolean(val value: Boolean) : JsonElement {
    override fun StringBuilder.emit(depth: Int) {
        append(value)
    }
}
