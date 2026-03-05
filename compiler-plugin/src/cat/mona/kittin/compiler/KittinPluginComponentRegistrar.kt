package cat.mona.kittin.compiler

import cat.mona.kittin.BuildConfig
import cat.mona.kittin.compiler.ir.SimpleIrGenerationExtension
import cat.mona.kittin.compiler.jvm.ClassBuilderExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import java.nio.file.Path

data class CompilerConfig(
    val path: Path,

    val required: Boolean,
    val minVersion: String,
    val compatibilityLevel: String,
    val mixinPlugin: String?,
    val mixinExtrasVersion: String?,
    val injectorsRequired: Int,
)

class KittinPluginComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String
        get() = BuildConfig.KOTLIN_PLUGIN_ID
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val modId = configuration[KittinCommandLineProcessor.modId]!!
        val mixinPackage = configuration[KittinCommandLineProcessor.mixinPackage]!!
        val mixinJson = configuration[KittinCommandLineProcessor.mixinFile]!!
        val compilerConfig = CompilerConfig(
            mixinJson,
            configuration[KittinCommandLineProcessor.required] ?: true,
            configuration[KittinCommandLineProcessor.minVersion] ?: MIN_MIXIN_VERSION,
            configuration[KittinCommandLineProcessor.compatibilityLevel] ?: COMPATIBILITY_VERSION,
            configuration[KittinCommandLineProcessor.mixinPlugin],
            configuration[KittinCommandLineProcessor.mixinExtrasVersion],
            configuration[KittinCommandLineProcessor.injectorsRequired] ?: DEFAULT_INJECTORS,
        )

        FirExtensionRegistrarAdapter.registerExtension(KittinPluginRegistrar())
        IrGenerationExtension.registerExtension(SimpleIrGenerationExtension(modId, mixinPackage, compilerConfig))
        ClassGeneratorExtension.registerExtension(ClassBuilderExtension())

    }
}
