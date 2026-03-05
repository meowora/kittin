package cat.mona.kittin

annotation class Accessor

@Accessor
external var String.box: String
var String.box2: String
    get() = TODO()
    set(value) = TODO()

fun box() = "OK"
