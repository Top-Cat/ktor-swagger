package de.nielsfalk.ktor.swagger

import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
annotation class DefaultValue(
    val value: String
)

@Target(AnnotationTarget.PROPERTY)
annotation class Description(
    val description: String
)

@Target(AnnotationTarget.PROPERTY)
annotation class Ignore

@Target(AnnotationTarget.PROPERTY)
annotation class ModelClass(
    val clazz: KClass<*>
)
