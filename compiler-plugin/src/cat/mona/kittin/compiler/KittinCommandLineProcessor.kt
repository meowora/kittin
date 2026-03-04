package cat.mona.kittin.compiler

import cat.mona.kittin.BuildConfig
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@Suppress("unused") // Used via reflection.
class KittinCommandLineProcessor : CommandLineProcessor {

    private val modId = CliOption(
        "modId",
        "The id of the mod",
        "The id used for prefixes in accessors."
    )

    private val mixinPackage = CliOption(
        "mixinPackage",
        "The root mixin package",
        "The package that all synthetic mixins should be located in."
    )

    override val pluginId: String
        get() = BuildConfig.KOTLIN_PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(
        modId, mixinPackage
    )

    companion object {
        val modId = CompilerConfigurationKey<String>("modId")
        val mixinPackage  = CompilerConfigurationKey<String>("mixinPackage")
    }

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            modId -> configuration.put(KittinCommandLineProcessor.modId, value)
            mixinPackage -> configuration.put(KittinCommandLineProcessor.mixinPackage, value)
        }
    }
}
