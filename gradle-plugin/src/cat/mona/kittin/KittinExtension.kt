package cat.mona.kittin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class KittinExtension() {

    @get:Input abstract val modId: Property<String>
    @get:Input abstract val mixinPackage: Property<String>

    @get:Input abstract val required: Property<Boolean>
    @get:Input abstract val minVersion: Property<String>
    @get:Input abstract val compatibilityLevel: Property<String>
    @get:Input abstract val mixinPlugin: Property<String>
    @get:Input abstract val mixinExtrasVersion: Property<String>
    @get:Input abstract val injectorsRequired: Property<Int>

}
