package cat.mona.kittin.compiler.ir

import cat.mona.kittin.compiler.KittinConstants
import cat.mona.kittin.compiler.KittinConstants.accessorFieldName
import cat.mona.kittin.compiler.KittinConstants.accessorRemapped
import cat.mona.kittin.compiler.KittinConstants.accessorType
import cat.mona.kittin.compiler.KittinGenerated
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName

class AccessorGenerator(val mixnPackage: FqName, val pluginContext: IrPluginContext, val typeLookup: Map<IrType, IrType>) : IrElementTransformerVoid() {

    private fun IrAnnotationContainer.isAccessorOrInvoker() = this.hasAnnotation(KittinConstants.kittinAccessor) || this.hasAnnotation(KittinConstants.invokerAnnotation)

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (!declaration.isAccessorOrInvoker() || declaration.origin == KittinGenerated) {
            return super.visitSimpleFunction(declaration)
        }
        val parent = declaration.parent
        if (parent !is IrFile) TODO()

        val receiver = declaration.parameters.first { it.kind == IrParameterKind.ExtensionReceiver }

        val factory = pluginContext.irFactory

        val type = typeLookup[receiver.type]!!
        val irClass = type.getClass()!!
        val casted = IrTypeOperatorCallImpl(
            -1, -1, type, IrTypeOperator.CAST, type,
            IrGetValueImpl(-1, -1, receiver.symbol)
        )

        val kind =  declaration.accessorType(pluginContext)

        val parameters = declaration.parameters.filterNot { it.kind == IrParameterKind.ExtensionReceiver }
        val function = irClass.functions.find {
            it.accessorType == kind &&
                it.accessorFieldName == declaration.getAccessorName()
                && it.accessorRemapped == declaration.isRemapped()
                && it.parameters.filterNot { it.kind == IrParameterKind.DispatchReceiver }.map { it.type }.toTypedArray().contentEquals(parameters.map { it.type }.toTypedArray())
        }!!

        declaration.body = factory.createExpressionBody(
            IrCallImpl(
                -1,
                -1,
                function.returnType,
                function.symbol,
            ).apply {
                for ((index, parameter) in parameters.withIndex()) {
                    arguments[1 + index] = IrGetValueImpl(-1, -1, parameter.symbol)
                }

                arguments[0] = casted
            },
        )

        declaration.annotations -= declaration.annotations.filter { it.type.classFqName == kind.annotation.asSingleFqName() }

        declaration.isExternal = false

        return super.visitSimpleFunction(declaration)
    }

}
