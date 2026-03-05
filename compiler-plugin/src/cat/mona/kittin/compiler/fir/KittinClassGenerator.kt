package cat.mona.kittin.compiler.fir

import cat.mona.kittin.compiler.KittinConstants
import cat.mona.kittin.compiler.KittinConstants.kittinAccessor
import cat.mona.kittin.compiler.KittinFirGenerated
import cat.mona.kittin.compiler.KittinGenerated
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.utils.relfection.renderAsDataClassToString
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameterKind
import org.jetbrains.kotlin.fir.declarations.builder.FirDefaultSetterValueParameterBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirPropertyAccessorBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirValueParameterBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildDefaultSetterValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getBooleanArgument
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.hasAnnotationSafe
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirAnnotationBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirFunctionCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirReturnExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildNamedArgumentExpression
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.transform
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.references.builder.FirSimpleNamedReferenceBuilder
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.booleanLiteralValue
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.renderControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.typeResolver
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.ConstantValueKind
import kotlin.collections.plus

class KittinClassGenerator(session: FirSession) : FirStatusTransformerExtension(session) {
    private fun FirDeclaration.isAccessorOrInvoker() = this.hasAnnotationSafe(KittinConstants.kittinAccessor, session) || this.hasAnnotationSafe(KittinConstants.invokerAnnotation, session)

    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        return declaration.isAccessorOrInvoker()
    }

    override fun transformStatus(status: FirDeclarationStatus, property: FirProperty, containingClass: FirClassLikeSymbol<*>?, isLocal: Boolean): FirDeclarationStatus {
        val kittinAccessorAnnotation = property.getAnnotationByClassId(kittinAccessor, session) ?: return status
        val status = status.transform {
            isExternal = false
        }

        val fakeSource = property.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated)

        if (property.isVar) {
            buildPropertyAccessor {
                moduleData = property.moduleData
                origin = KittinFirGenerated
                this.status = status
                returnTypeRef = session.builtinTypes.unitType
                symbol = FirPropertyAccessorSymbol()
                source = fakeSource
                propertySymbol = property.symbol
                isGetter = false
                valueParameters += buildDefaultSetterValueParameter {
                    moduleData = property.moduleData
                    origin = KittinFirGenerated
                    symbol = FirValueParameterSymbol()
                    returnTypeRef = property.returnTypeRef
                    containingDeclarationSymbol = property.symbol
                }

                annotations += kittinAccessorAnnotation
            }.apply {
                attributes
                replaceBody(createTodoBody(this))
            }.also(property::replaceSetter)
        }
        buildPropertyAccessor {
            moduleData = property.moduleData
            origin = KittinFirGenerated
            source = fakeSource
            this.status = status
            returnTypeRef = property.returnTypeRef
            symbol = FirPropertyAccessorSymbol()
            propertySymbol = property.symbol
            isGetter = true

            annotations += kittinAccessorAnnotation
        }.apply {
            replaceBody(createTodoBody(this))
        }.also(property::replaceGetter)


        return status
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        function: FirSimpleFunction,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean,
    ): FirDeclarationStatus {
        function.replaceBody(createTodoBody(function))
        return status.transform {
            isExternal = false
        }
    }

    fun createTodoBody(element: FirFunction) = FirBlockBuilder().apply {
        statements += FirReturnExpressionBuilder().apply {
            target = FirFunctionTarget(null, false).apply {
                bind(element)
            }
            result = FirFunctionCallBuilder().apply {
                calleeReference = FirSimpleNamedReferenceBuilder().apply {
                    name = Name.identifier("TODO")
                }.build()
            }.build()
        }.build()
    }.build()


}
