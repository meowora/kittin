package cat.mona.kittin.compiler.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.ClassId

object KittinDiagnostics {
    val KITTIN_ACCESSOR_WITHOUT_RECEIVER = KtDiagnosticFactory1<ClassId>(
        "KITTIN_ACCESSOR_WITHOUT_RECEIVER",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        Any::class,
        KittenErrorsDefaultMessages
    )
    val KITTIN_ACCESOR_AND_INVOKER_PRESENT = KtDiagnosticFactory1<FirBasedSymbol<*>>(
        "KITTIN_ACCESOR_AND_INVOKER_PRESENT",
        Severity.ERROR,
        SourceElementPositioningStrategies.DEFAULT,
        Any::class,
        KittenErrorsDefaultMessages
    )


    object KittenErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
        val SYMBOL = FirDiagnosticRenderers.SYMBOL

        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("KITTIN") { map ->
            map.put(KITTIN_ACCESSOR_WITHOUT_RECEIVER, "{0} requires a receiver type!", KtDiagnosticRenderers.CLASS_ID)
            map.put(KITTIN_ACCESOR_AND_INVOKER_PRESENT, "{0} has both @Accessor and @Invoker which is not allowed!", FirDiagnosticRenderers.SYMBOL)
        }

    }
}
