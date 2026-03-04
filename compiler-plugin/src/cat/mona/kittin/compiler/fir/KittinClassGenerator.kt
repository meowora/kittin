package cat.mona.kittin.compiler.fir

import cat.mona.kittin.compiler.KittinConstants
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotationSafe
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirFunctionCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirReturnExpressionBuilder
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.transform
import org.jetbrains.kotlin.fir.references.builder.FirSimpleNamedReferenceBuilder
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.Name

class KittinClassGenerator(session: FirSession) : FirStatusTransformerExtension(session) {
    private fun FirDeclaration.isAccessorOrInvoker() = this.hasAnnotationSafe(KittinConstants.accessorAnnotation, session) || this.hasAnnotationSafe(KittinConstants.invokerAnnotation, session)

    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        return declaration.isAccessorOrInvoker()
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        function: FirSimpleFunction,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        function.replaceBody(
            FirBlockBuilder().apply {
            statements += FirReturnExpressionBuilder().apply {
                target = FirFunctionTarget(null, false).apply {
                    bind(function)
                }
                result = FirFunctionCallBuilder().apply {
                    calleeReference = FirSimpleNamedReferenceBuilder().apply {
                        name = Name.identifier("TODO")
                    }.build()
                }.build()
            }.build()
        }.build())

        return status.transform {
            isExternal = false
        }
    }

}
