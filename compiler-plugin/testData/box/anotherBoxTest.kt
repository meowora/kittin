package org.spongepowered.asm.mixin.gen

annotation class Accessor

@Accessor
external fun String.box(): String

class B

interface Mrow {
    fun test()
}

fun B.a() = (this as Mrow).test()

fun box() = "OK"
