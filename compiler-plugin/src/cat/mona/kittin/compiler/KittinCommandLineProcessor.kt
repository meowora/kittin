package cat.mona.kittin.compiler

import cat.mona.kittin.BuildConfig
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.nio.file.Path
import kotlin.io.path.Path

const val MIN_MIXIN_VERSION = "0.8"
const val COMPATIBILITY_VERSION = "JAVA_21"
const val DEFAULT_INJECTORS = 1

@Suppress("unused") // Used via reflection.
class KittinCommandLineProcessor : CommandLineProcessor {

    private val modId = CliOption(
        "modId",
        "The id of the mod",
        "The id used for prefixes in accessors.",
    )

    private val mixinPackage = CliOption(
        "mixinPackage",
        "The root mixin package",
        "The package that all synthetic mixins should be located in.",
    )

    private val mixinFile = CliOption(
        "mixinFile",
        "The absolute path",
        "The absolute path for the mixin.json file.",
    )


    private val required = CliOption(
        "required",
        "true/false (default is true)",
        "Whether the generated mixin config is required",
        false,
    )

    private val minVersion = CliOption(
        "minVersion",
        "Minimum mixin version",
        "The minimum mixin version (defaults to $MIN_MIXIN_VERSION)",
        false,
    )

    private val compatibilityLevel = CliOption(
        "compatibilityLevel",
        "Compatibility level",
        "The compatibility version of the mixin system (defaults to $COMPATIBILITY_VERSION)",
        false,
    )

    private val mixinPlugin = CliOption(
        "mixinPlugin",
        "Mixin plugin fqName",
        "The mixin plugin to use for the config.",
        false,
    )

    private val mixinExtrasVersion = CliOption(
        "mixinExtrasVersion",
        "Minimum mixin extras version",
        "The minimum mixin extras version to use.",
        false,
    )

    private val injectorsRequired = CliOption(
        "requiredInjectors",
        "Int",
        "The amount of required injectors (defaults to $DEFAULT_INJECTORS)",
        false,
    )


    override val pluginId: String
        get() = BuildConfig.KOTLIN_PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf(modId, mixinPackage,mixinFile, required, minVersion, compatibilityLevel, mixinPlugin, mixinExtrasVersion, injectorsRequired,)

    companion object {
        val modId = CompilerConfigurationKey<String>("modId")
        val mixinPackage = CompilerConfigurationKey<String>("mixinPackage")
        val mixinFile = CompilerConfigurationKey<Path>("mixinFile")

        val required = CompilerConfigurationKey<Boolean>("required")
        val minVersion = CompilerConfigurationKey<String>("minVersion")
        val compatibilityLevel = CompilerConfigurationKey<String>("compatibilityLevel")
        val mixinPlugin = CompilerConfigurationKey<String>("mixinPlugin")
        val mixinExtrasVersion = CompilerConfigurationKey<String>("mixinExtrasVersion")
        val injectorsRequired = CompilerConfigurationKey<Int>("injectorsRequired")
    }

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            modId -> configuration.put(KittinCommandLineProcessor.modId, value)
            mixinPackage -> configuration.put(KittinCommandLineProcessor.mixinPackage, value)
            mixinFile -> configuration.put(KittinCommandLineProcessor.mixinFile, Path(value))

            required -> configuration.put(KittinCommandLineProcessor.required, "true".equals(value, true))
            minVersion -> configuration.put(KittinCommandLineProcessor.minVersion, value)
            compatibilityLevel -> configuration.put(KittinCommandLineProcessor.compatibilityLevel, value)
            mixinPlugin -> configuration.put(KittinCommandLineProcessor.mixinPlugin, value)
            mixinExtrasVersion -> configuration.put(KittinCommandLineProcessor.mixinExtrasVersion, value)
            injectorsRequired -> configuration.put(KittinCommandLineProcessor.injectorsRequired, value.toInt())
        }
    }
}
