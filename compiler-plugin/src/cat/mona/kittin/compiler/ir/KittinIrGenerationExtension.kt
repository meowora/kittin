package cat.mona.kittin.compiler.ir

import cat.mona.kittin.compiler.CompilerConfig
import cat.mona.kittin.compiler.KittinConstants
import cat.mona.kittin.compiler.KittinConstants.accessorFieldName
import cat.mona.kittin.compiler.KittinConstants.accessorRemapped
import cat.mona.kittin.compiler.KittinConstants.accessorType
import cat.mona.kittin.compiler.KittinConstants.not
import cat.mona.kittin.compiler.KittinConstants.plus
import cat.mona.kittin.compiler.KittinGenerated
import cat.mona.kittin.compiler.utils.JsonArray
import cat.mona.kittin.compiler.utils.json
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.descriptors.FirPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addFile
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.referenceClassifier
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

enum class AccessorType(val annotation: ClassId) {
    SETTER(KittinConstants.accessorAnnotation),
    GETTER(KittinConstants.accessorAnnotation),
    INVOKER(KittinConstants.invokerAnnotation),
}

data class IrValueParameterWrapper(val parameter: IrValueParameter) {
    val type = parameter.type


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IrValueParameterWrapper) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }
}

data class AccessorMetadata(val name: String, val remap: Boolean, val returnType: IrType, val parameters: Array<IrValueParameterWrapper>, val type: AccessorType) {
    companion object {
        fun of(name: String, remap: Boolean, returnType: IrType, parameters: List<IrValueParameter>, type: AccessorType) = AccessorMetadata(name, remap, returnType, parameters.map(::IrValueParameterWrapper).toTypedArray(), type)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccessorMetadata) return false

        if (remap != other.remap) return false
        if (name != other.name) return false
        if (returnType != other.returnType) return false
        if (!parameters.contentEquals(other.parameters)) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = remap.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + returnType.hashCode()
        result = 31 * result + parameters.contentHashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}
typealias AccessorCollector = MutableMap<IrType?, MutableSet<AccessorMetadata>>

fun AccessorCollector.register(id: IrType?, meta: AccessorMetadata) {
    this.getOrPut(id) { mutableSetOf() }.add(meta)
}

class SimpleIrGenerationExtension(val modId: String, val rawMixinPackage: String, val compilerConfig: CompilerConfig) : IrGenerationExtension {
    val mixinPackage = FqName.fromSegments(rawMixinPackage.split("."))

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        pluginContext.messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, "mrow")
        val accessors: AccessorCollector = mutableMapOf()
        moduleFragment.transformChildrenVoid(AccessorVisitor(accessors, pluginContext))

        val irLookup = mutableMapOf<IrType, IrType>()

        val mixins = mutableSetOf<String>()

        val irBuiltIns = pluginContext.irBuiltIns
        accessors.forEach { (type, accessors) ->
            val name = !(type!!.classFqName!!.shortNameOrSpecial().asString() + "Accessor")
            mixins.add("accessors." + name.asString())

            pluginContext.messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, ((mixinPackage + "accessors") + (name.asString())).toString())
            moduleFragment.addFile(
                IrFileImpl(
                    fileEntry = NaiveSourceBasedFileEntryImpl(name = name.asString()),
                    packageFragmentDescriptor = FirPackageFragmentDescriptor(mixinPackage + "accessors", moduleFragment.descriptor),
                ).apply {
                    attributeOwnerId = this
                    val accessor = pluginContext.irFactory.buildClass {
                        visibility = DescriptorVisibilities.PUBLIC
                        this.name = name
                        this.modality = Modality.ABSTRACT
                        kind = ClassKind.INTERFACE
                        this.origin = KittinGenerated
                    }.apply {
                        createThisReceiverParameter()
                    }
                    irLookup[type] = accessor.defaultType

                    val classType = pluginContext.referenceClass(KittinConstants.mixinAnnotation)!!
                    accessor.annotations += IrConstructorCallImpl.fromSymbolOwner(classType.typeWith(), classType.constructors.single()).apply {
                        arguments[0] = IrClassReferenceImpl(-1, -1, type, type.classOrFail, type)
                    }

                    accessors.forEach { (name, remap, returnType, parameters, type) ->
                        pluginContext.messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, $$"$$modId$$$name$$${type.name.lowercase()}")
                        val accessorFunction = pluginContext.irFactory.addFunction(accessor) {
                            this.name = Name.identifier($$"$$modId$$$name$${if (remap) $$"$remapped" else ""}$$${type.name.lowercase()}")
                            this.returnType = returnType
                            this.modality = Modality.ABSTRACT
                            this.visibility = DescriptorVisibilities.PUBLIC
                            this.origin = KittinGenerated
                        }

                        accessorFunction.parameters += accessorFunction.buildReceiverParameter {
                            this.type = accessor.typeWith()
                            this.origin = KittinGenerated
                        }

                        val classType = pluginContext.referenceClass(type.annotation)!!
                        accessorFunction.annotations += IrConstructorCallImpl.fromSymbolOwner(classType.typeWith(), classType.constructors.single()).apply {
                            arguments[0] = IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.stringType, name)
                            arguments[1] = IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.booleanType, remap)
                        }

                        accessorFunction.accessorType = type
                        accessorFunction.accessorFieldName = name
                        accessorFunction.accessorRemapped = remap

                        for ((parameter) in parameters) {
                            if (parameter.kind == IrParameterKind.ExtensionReceiver) continue
                            accessorFunction.addValueParameter {
                                this.name = parameter.name
                                this.updateFrom(parameter)
                                this.origin = KittinGenerated
                            }
                        }
                    }

                    addChild(accessor)
                },
            )
        }

        val path = compilerConfig.path
        path.createParentDirectories()
        path.writeText(
            json {
                "required"(compilerConfig.required)
                "minVersion"(compilerConfig.minVersion)
                "package"(rawMixinPackage)
                "compatibilityLevel"(compilerConfig.compatibilityLevel)
                compilerConfig.mixinExtrasVersion?.let {
                    "mixinextras" {
                        "minVersion"(it)
                    }
                }
                compilerConfig.mixinPlugin?.let {
                    "plugin"(it)
                }
                "injectors" {
                    "defaultRequire"(compilerConfig.injectorsRequired)
                }
                "overwrites" {
                    "requireAnnotations"(true)
                }
                "mixins"(JsonArray(mixins))
            }.dump(),
        )

        moduleFragment.transformChildrenVoid(AccessorGenerator(mixinPackage, pluginContext, irLookup))
    }
}
