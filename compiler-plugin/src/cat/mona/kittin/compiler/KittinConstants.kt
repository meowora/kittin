package cat.mona.kittin.compiler

import cat.mona.kittin.compiler.ir.AccessorType
import org.jetbrains.kotlin.ir.IrAttribute
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object KittinConstants {

    operator fun ClassId.not() = this.asSingleFqName()
    operator fun String.unaryPlus() = FqName.fromSegments(this.split("."))
    operator fun String.not() = Name.identifier(this)
    operator fun FqName.plus(name: String) = this.child(!name)
    operator fun FqName.minus(name: String) = ClassId(this, !name)

    val mixinPackage = +"org.spongepowered.asm.mixin"
    val mixinGenPackage = mixinPackage + "gen"

    val mixinAnnotation = mixinPackage - "Mixin"
    val accessorAnnotation = mixinGenPackage - "Accessor"
    val invokerAnnotation = mixinGenPackage - "Invoker"

    var IrFunction.accessorType: AccessorType? by irAttribute(true)
    var IrFunction.accessorRemapped: Boolean? by irAttribute(true)
    var IrFunction.accessorFieldName: String? by irAttribute(true)
}

data object KittinGenerated : IrDeclarationOrigin {
    override val name: String = "cat.mona.kittin.generated"
    override val isSynthetic: Boolean = false

    override fun toString(): String = "KITTIN_GENERATED"
}

data object KittinEntrypoint : IrDeclarationOrigin {
    override val name: String = "cat.mona.kittin.entrypoint"
    override val isSynthetic: Boolean = true

    override fun toString(): String = "KITTIN_ENTRYPOINT"
}
