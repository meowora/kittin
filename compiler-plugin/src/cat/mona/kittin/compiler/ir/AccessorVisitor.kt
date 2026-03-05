package cat.mona.kittin.compiler.ir

import cat.mona.kittin.compiler.KittinConstants.invokerAnnotation
import cat.mona.kittin.compiler.KittinConstants.kittinAccessor
import cat.mona.kittin.compiler.KittinConstants.not
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrMutableAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

fun <T> T.accessorOrInvoker() where T : IrMutableAnnotationContainer, T : IrDeclarationWithName = getAnnotation(!kittinAccessor) ?: getAnnotation(!invokerAnnotation)

fun <T> T.getAccessorName(): String where T : IrMutableAnnotationContainer, T : IrDeclarationWithName {
    return (accessorOrInvoker()?.getArgumentsWithIr()?.find { (key) ->
        key.name.asString() == "value"
    }?.second as IrConst?)?.value?.toString() ?: this.name.asString()
}

fun <T> T.isRemapped(): Boolean where T : IrMutableAnnotationContainer, T : IrDeclarationWithName {
    return ((accessorOrInvoker()?.getArgumentsWithIr()?.find { (key) ->
        key.name.asString() == "remap"
    }?.second as IrConst?)?.value as Boolean?) ?: true
}

fun IrSimpleFunction.accessorType(pluginContext: IrPluginContext): AccessorType {
    return if (hasAnnotation(kittinAccessor)) {
        if (returnType == pluginContext.irBuiltIns.unitType) {
            AccessorType.SETTER
        } else {
            AccessorType.GETTER
        }
    } else AccessorType.INVOKER
}

class AccessorVisitor(val collector: AccessorCollector, val pluginContext: IrPluginContext) : IrElementTransformerVoid() {

    private fun IrAnnotationContainer.isAccessorOrInvoker() = this.hasAnnotation(kittinAccessor) || this.hasAnnotation(invokerAnnotation)

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (!declaration.isAccessorOrInvoker()) return super.visitSimpleFunction(declaration)

        val classId = declaration.parameters.find { it.kind == IrParameterKind.ExtensionReceiver }!!.type
        val accessorOrInvoker = declaration.getAnnotation(!kittinAccessor) ?: declaration.getAnnotation(!invokerAnnotation)

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
                declaration.accessorType(pluginContext),
            ),
        )

        return super.visitSimpleFunction(declaration)
    }

}
