package com.atlarge.opendc.simulator

import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn

/**
 * Try to find the [Context] instance associated with the [Process] in the call chain which has (indirectly) invoked the
 * caller of this method.
 *
 * Note however that this method does not guarantee type-safety as this method allows the user to cast to a context
 * with different generic type arguments.
 *
 * @return The context that has been found or `null` if this method is not called in a simulation context.
 */
suspend fun <S, M> contextOrNull(): Context<S, M>? = suspendCoroutineOrReturn { it.context[Context] }

/**
 * Find the [Context] instance associated with the [Process] in the call chain which has (indirectly) invoked the
 * caller of this method.
 *
 * Note however that this method does not guarantee type-safety as this method allows the user to cast to a context
 * with different generic type arguments.
 *
 * @throws IllegalStateException if the context cannot be found.
 * @return The context that has been found.
 */
suspend fun <S, M> context(): Context<S, M> =
    contextOrNull() ?: throw IllegalStateException("The suspending call does not have an associated process context")

/**
 * Try to find the untyped [Context] instance associated with the [Process] in the call chain which has (indirectly)
 * invoked the caller of this method.
 *
 * @return The untyped context that has been found or `null` if this method is not called in a simulation context.
 */
suspend fun untypedContextOrNull(): Context<*, *>? = contextOrNull<Any?, Any?>()

/**
 * Find the [Context] instance associated with the [Process] in the call chain which has (indirectly) invoked the
 * caller of this method.
 *
 * @throws IllegalStateException if the context cannot be found.
 * @return The untyped context that has been found.
 */
suspend fun untypedContext(): Context<*, *> = context<Any?, Any?>()
