package cat.mona.kittin.compiler.ir

import cat.mona.kittin.compiler.KittinConstants
import cat.mona.kittin.compiler.KittinConstants.accessorFieldName
import cat.mona.kittin.compiler.KittinConstants.accessorRemapped
import cat.mona.kittin.compiler.KittinConstants.accessorType
import cat.mona.kittin.compiler.KittinConstants.not
import cat.mona.kittin.compiler.KittinConstants.plus
import cat.mona.kittin.compiler.KittinGenerated
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.descriptors.FirPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.addFile
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

enum class AccessorType(val annotation: ClassId) {
    ACCESSOR(KittinConstants.accessorAnnotation),
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

data class AccessorMetadata(val name: String, val remap: Boolean, val returnType: IrType, val parameters: List<IrValueParameterWrapper>, val type: AccessorType) {
    companion object {
        fun of(name: String, remap: Boolean, returnType: IrType, parameters: List<IrValueParameter>, type: AccessorType) = AccessorMetadata(name, remap, returnType, parameters.map(::IrValueParameterWrapper), type)
    }
}
typealias AccessorCollector = MutableMap<IrType?, MutableSet<AccessorMetadata>>

fun AccessorCollector.register(id: IrType?, meta: AccessorMetadata) {
    this.getOrPut(id) { mutableSetOf() }.add(meta)
}

class SimpleIrGenerationExtension(val modId: String, mixinPackage: String) : IrGenerationExtension {
    val mixinPackage = FqName.fromSegments(mixinPackage.split("."))

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        pluginContext.messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, "mrow")
        val accessors: AccessorCollector = mutableMapOf()
        moduleFragment.transformChildrenVoid(AccessorVisitor(accessors, pluginContext))

        val irLookup = mutableMapOf<IrType, IrType>()

        accessors.forEach { (type, accessors) ->
            val name = !(type!!.classFqName!!.shortNameOrSpecial().asString() + "Accessor")
            pluginContext.messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, mixinPackage.plus(name.asString() + "Accessor").toString())
            moduleFragment.addFile(
                IrFileImpl(
                    fileEntry = NaiveSourceBasedFileEntryImpl(name = name.asString()),
                    packageFragmentDescriptor = FirPackageFragmentDescriptor(mixinPackage, moduleFragment.descriptor),
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

                        accessorFunction.accessorType = type
                        accessorFunction.accessorFieldName = name
                        accessorFunction.accessorRemapped = remap

                        for ((parameter) in parameters.listIterator(1)) {
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

        moduleFragment.transformChildrenVoid(AccessorGenerator(mixinPackage, pluginContext, irLookup))
    }
}
