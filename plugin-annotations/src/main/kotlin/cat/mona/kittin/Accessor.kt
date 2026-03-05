package cat.mona.kittin

/**
 * See [org.spongepowered.asm.mixin.gen.Accessor]
 */
@Retention(AnnotationRetention.RUNTIME)
public annotation class Accessor(

    val value: String,


    val remap: Boolean = true

)
