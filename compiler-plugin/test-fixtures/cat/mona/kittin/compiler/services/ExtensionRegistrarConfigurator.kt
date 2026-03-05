package cat.mona.kittin.compiler.services

import cat.mona.kittin.compiler.KittinCommandLineProcessor
import cat.mona.kittin.compiler.KittinPluginComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists

fun TestConfigurationBuilder.configurePlugin() {
    useConfigurators(::ExtensionRegistrarConfigurator)
}

private class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    private val registrar = KittinPluginComponentRegistrar()
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {

        val path = Path("mrow")
        configuration.put(KittinCommandLineProcessor.modId, "kittin")
        configuration.put(KittinCommandLineProcessor.mixinPackage, "cat.mona.kittin.mixin_test")
        configuration.put(KittinCommandLineProcessor.mixinFile, path)
        with(registrar) { registerExtensions(configuration) }
        path.deleteIfExists()
    }
}
