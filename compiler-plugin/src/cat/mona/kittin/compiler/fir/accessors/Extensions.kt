package cat.mona.kittin.compiler.fir.accessors

import cat.mona.kittin.compiler.fir.accessors.FirAccessors.isAccessorLike
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.transform
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

class FirKittinAccessorStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    override fun needTransformStatus(declaration: FirDeclaration): Boolean = declaration.isAccessorLike(session)

    override fun transformStatus(status: FirDeclarationStatus, property: FirProperty, containingClass: FirClassLikeSymbol<*>?, isLocal: Boolean): FirDeclarationStatus {
        property.getter?.let {
            it.replaceAnnotations(property.annotations)
            it.replaceStatus(it.status.transform {
                isExternal = false
            })
        }
        property.setter?.let {
            it.replaceAnnotations(property.annotations)
            it.replaceStatus(it.status.transform {
                isExternal = false
            })
        }

        return status.transform {
            isExternal = false
        }
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        function: FirSimpleFunction,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean,
    ): FirDeclarationStatus {
        return status.transform {
            isExternal = false
        }
    }
}
