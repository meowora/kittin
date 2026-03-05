package cat.mona.kittin.compiler.fir

import cat.mona.kittin.compiler.KittinConstants.invokerAnnotation
import cat.mona.kittin.compiler.KittinConstants.kittinAccessor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotationSafe
import org.jetbrains.kotlin.name.ClassId

class KittinAdditionalCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers = object : DeclarationCheckers() {
        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> = setOf(KittinSimpleFunctionChecker(session))
    }

}

data class KittinSimpleFunctionChecker(val session: FirSession) : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    private fun FirDeclaration.isAccessorOrInvoker() = this.hasAnyAnnotation(kittinAccessor, invokerAnnotation)

    private fun FirDeclaration.hasAllAnnotations(vararg id: ClassId) = id.all { this.hasAnnotationSafe(it, session) }
    private fun FirDeclaration.hasAnyAnnotation(vararg id: ClassId) = id.any { this.hasAnnotationSafe(it, session) }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirSimpleFunction) {
        if (!declaration.isAccessorOrInvoker()) return

        if (declaration.hasAllAnnotations(kittinAccessor, invokerAnnotation)) {
            reporter.reportOn(declaration.source, KittinDiagnostics.KITTIN_ACCESOR_AND_INVOKER_PRESENT, declaration.symbol)
            return
        }

        val type = if (declaration.hasAnyAnnotation(kittinAccessor)) kittinAccessor else invokerAnnotation

        if (declaration.receiverParameter == null) {
            reporter.reportOn(declaration.source, KittinDiagnostics.KITTIN_ACCESSOR_WITHOUT_RECEIVER, type)
            return
        }
    }

}
