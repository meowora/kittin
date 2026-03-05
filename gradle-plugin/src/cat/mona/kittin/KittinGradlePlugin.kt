package cat.mona.kittin

import cat.mona.kittin.BuildConfig.ANNOTATIONS_LIBRARY_COORDINATES
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import kotlin.io.path.absolutePathString

@Suppress("unused") // Used via reflection.
class KittinGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        val extension = target.extensions.create("kittin", KittinExtension::class.java)
        extension.modId.convention(target.name.lowercase())
        target.dependencies.add("compileOnly", ANNOTATIONS_LIBRARY_COORDINATES)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = BuildConfig.KOTLIN_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
        artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
        version = BuildConfig.KOTLIN_PLUGIN_VERSION,
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        val fileName = "${project.name}.${kotlinCompilation.defaultSourceSet.name}.kittin.json"
        val directory = project.layout.buildDirectory.file("generated/kittin/${kotlinCompilation.disambiguatedName}/")
        kotlinCompilation.defaultSourceSet.resources.also { resources ->
            resources.srcDirs(directory)
        }

        return project.provider {
            val extension = project.extensions.getByType(KittinExtension::class.java)

            listOfNotNull(
                SubpluginOption(
                    "modId",
                    extension.modId.get()
                ),
                SubpluginOption(
                    "mixinPackage",
                    extension.mixinPackage.orNull ?: error("Mixin package is required!")
                ),
                SubpluginOption(
                    "mixinFile",
                    directory.get().asFile.toPath().resolve(fileName).absolutePathString()
                ),

                extension.required.map { SubpluginOption("required", it.toString()) }.orNull,
                extension.minVersion.map { SubpluginOption("minVersion", it) }.orNull,
                extension.compatibilityLevel.map { SubpluginOption("compatibilityLevel", it) }.orNull,
                extension.mixinPlugin.map { SubpluginOption("mixinPlugin", it) }.orNull,
                extension.mixinExtrasVersion.map { SubpluginOption("mixinExtrasVersion", it) }.orNull,
                extension.injectorsRequired.map { SubpluginOption("injectorsRequired", it.toString()) }.orNull,
            )
        }
    }
}
