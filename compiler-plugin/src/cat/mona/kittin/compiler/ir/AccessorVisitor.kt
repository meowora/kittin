package cat.mona.kittin.compiler.ir

import cat.mona.kittin.compiler.KittinConstants.accessorAnnotation
import cat.mona.kittin.compiler.KittinConstants.invokerAnnotation
import cat.mona.kittin.compiler.KittinConstants.not
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstantPrimitive
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

fun IrSimpleFunction.accessorOrInvoker() = getAnnotation(!accessorAnnotation) ?: getAnnotation(!invokerAnnotation)

fun IrSimpleFunction.getAccessorName(): String {
    return (accessorOrInvoker()?.getArgumentsWithIr()?.find { (key) ->
        key.name.asString() == "value"
    }?.second as IrConst?)?.value?.toString() ?: this.name.asString()
}

fun IrSimpleFunction.isRemapped(): Boolean {
    return ((accessorOrInvoker()?.getArgumentsWithIr()?.find { (key) ->
        key.name.asString() == "remap"
    }?.second as IrConst?)?.value as Boolean?) ?: true
}

class AccessorVisitor(val collector: AccessorCollector, val pluginContext: IrPluginContext) : IrElementTransformerVoid() {

    private fun IrAnnotationContainer.isAccessorOrInvoker() = this.hasAnnotation(accessorAnnotation) || this.hasAnnotation(invokerAnnotation)

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (!declaration.isAccessorOrInvoker()) return super.visitSimpleFunction(declaration)

        val classId = declaration.parameters.find { it.kind == IrParameterKind.ExtensionReceiver }!!.type
        val accessorOrInvoker = declaration.getAnnotation(!accessorAnnotation) ?: declaration.getAnnotation(!invokerAnnotation)

        accessorOrInvoker?.let {
            pluginContext.messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, it.dump())
        }

        val name = declaration.getAccessorName()
        val remap = declaration.isRemapped()

        collector.register(
            classId,
            AccessorMetadata.of(
                name,
                remap,
                declaration.returnType,
                declaration.parameters,
                if (declaration.hasAnnotation(accessorAnnotation)) AccessorType.ACCESSOR else AccessorType.INVOKER,
            ),
        )

        return super.visitSimpleFunction(declaration)
    }

}
