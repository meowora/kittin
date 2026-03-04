package cat.mona.kittin.compiler.jvm

import cat.mona.kittin.compiler.KittinGenerated
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGenerator
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

class ClassBuilderExtension : ClassGeneratorExtension {
    override fun generateClass(
        generator: ClassGenerator,
        declaration: IrClass?,
    ): ClassGenerator {
        val declaration = declaration ?: return generator
        if (declaration.origin == KittinGenerated) {
            return ClassBuilderWrapper(generator)
        }
        return generator
    }
}

private val metadataAnnotations = setOf("Lkotlin/Metadata;")
private fun String.isMetadataAnnotation() = this in metadataAnnotations

class MethodVisitorWrapper(methodVisitor: MethodVisitor) : MethodVisitor(Opcodes.API_VERSION, methodVisitor) {
    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        if (descriptor?.isMetadataAnnotation() == true) {
            return object : AnnotationVisitor(Opcodes.API_VERSION) {}
        }

        return super.visitAnnotation(descriptor, visible)
    }
}


class ClassBuilderWrapper(val classBuilder: ClassGenerator) : ClassGenerator by classBuilder {
    override fun newMethod(
        declaration: IrFunction?,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor = MethodVisitorWrapper(classBuilder.newMethod(declaration, access, name, desc, signature, exceptions))

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
        if (desc.isMetadataAnnotation()) {
            return object : AnnotationVisitor(Opcodes.API_VERSION) {}
        }

        return classBuilder.visitAnnotation(desc, visible)
    }
}
