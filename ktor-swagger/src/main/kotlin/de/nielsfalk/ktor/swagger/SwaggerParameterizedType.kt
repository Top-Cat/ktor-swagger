package de.nielsfalk.ktor.swagger

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.javaType

/**
 * Create a parameterized type instance.
 * @param raw the raw class to create a parameterized type instance for
 * @param typeArguments the mapping used for parameterization
 * @return [ParameterizedType]
 */
@OptIn(ExperimentalStdlibApi::class)
internal fun parameterize(raw: Class<*>, rawArgs: List<KTypeParameter>, arguments: List<KTypeProjection>, vararg typeArguments: Type): ParameterizedType? {
    val useOwner: Type? = if (raw.enclosingClass == null) {
        // no owner allowed for top-level
        null
    } else {
        raw.enclosingClass
    }

    val argTest = arguments.mapNotNull { arg ->
        if (arg.type?.javaType is TypeVariable<*>) {
            val index = rawArgs.indexOfFirst { r -> r.name == (arg.type?.classifier as? KTypeParameter)?.name }
            typeArguments[index]
        } else {
            arg.type?.javaType
        }
    }

    return ParameterizedTypeImpl(raw, useOwner, argTest.toTypedArray())
}

/**
 * Implementation of [ParameterizedType].
 * @see parameterize
 */
internal data class ParameterizedTypeImpl
internal constructor(
    private val rawType: Type,
    private val ownerType: Type?,
    private val actualTypeArguments: Array<Type>
) : ParameterizedType {

    override fun getRawType(): Type = rawType

    override fun getOwnerType(): Type? = ownerType

    override fun getActualTypeArguments(): Array<Type> =
        actualTypeArguments.clone()

    override fun equals(other: Any?): Boolean =
        if (other is ParameterizedType) {
            rawType == other.rawType &&
                ownerType == other.ownerType &&
                    actualTypeArguments.contentEquals(other.actualTypeArguments)
        } else {
            false
        }
}
