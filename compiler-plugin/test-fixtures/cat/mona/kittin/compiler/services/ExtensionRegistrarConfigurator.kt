package cat.mona.kittin.compiler.services

import cat.mona.kittin.compiler.KittinCommandLineProcessor
import cat.mona.kittin.compiler.KittinPluginComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

fun TestConfigurationBuilder.configurePlugin() {
    useConfigurators(::ExtensionRegistrarConfigurator)
}

private class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    private val registrar = KittinPluginComponentRegistrar()
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {

        configuration.put(KittinCommandLineProcessor.modId, "kittin")
        configuration.put(KittinCommandLineProcessor.mixinPackage, "cat.mona.kittin.mixin_test")
        with(registrar) { registerExtensions(configuration) }
    }
}
