package cat.mona.kittin.compiler

import cat.mona.kittin.compiler.ir.SimpleIrGenerationExtension
import cat.mona.kittin.compiler.jvm.ClassBuilderExtension
import cat.mona.kittin.BuildConfig
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class KittinPluginComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String
        get() = BuildConfig.KOTLIN_PLUGIN_ID
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val modId = configuration[KittinCommandLineProcessor.modId]!!
        val mixinPackage = configuration[KittinCommandLineProcessor.mixinPackage]!!

        val messageCollector = configuration.messageCollector

        FirExtensionRegistrarAdapter.registerExtension(KittinPluginRegistrar())
        IrGenerationExtension.registerExtension(SimpleIrGenerationExtension(modId, mixinPackage))
        ClassGeneratorExtension.registerExtension(ClassBuilderExtension())

    }
}
