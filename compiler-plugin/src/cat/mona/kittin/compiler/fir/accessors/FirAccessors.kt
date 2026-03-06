package cat.mona.kittin.compiler.fir.accessors

import cat.mona.kittin.compiler.KittinConstants.invokerAnnotation
import cat.mona.kittin.compiler.KittinConstants.kittinAccessor
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotationSafe

object FirAccessors {

    fun FirDeclaration.isAccessorLike(session: FirSession): Boolean {
        return this.hasAnnotationSafe(kittinAccessor, session) || this.hasAnnotationSafe(invokerAnnotation, session)
    }

}
