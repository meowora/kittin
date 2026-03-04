package cat.mona.kittin.compiler

import cat.mona.kittin.compiler.fir.KittinAdditionalCheckers
import cat.mona.kittin.compiler.fir.KittinClassGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class KittinPluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::KittinAdditionalCheckers
        +::KittinClassGenerator
    }


}
