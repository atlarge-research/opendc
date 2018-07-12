package com.atlarge.opendc.simulator.instrumentation

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.channels.toChannel
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.coroutineContext

/**
 * Transform each element in the channel into a [ReceiveChannel] of output elements that is then flattened into the
 * output stream by emitting elements from the channels as they become available.
 *
 * @param context The [CoroutineContext] to run the operation in.
 * @param transform The function to transform the elements into channels.
 * @return The flattened [ReceiveChannel] of merged elements.
 */
fun <E, R> ReceiveChannel<E>.flatMapMerge(context: CoroutineContext = Unconfined,
                                          transform: suspend (E) -> ReceiveChannel<R>): ReceiveChannel<R> =
    produce(context) {
        val job = launch(Unconfined) {
            consumeEach {
                launch(coroutineContext) {
                    transform(it).toChannel(this@produce)
                }
            }
        }
        job.join()
    }

/**
 * Merge this channel with the other channel into an output stream by emitting elements from the channels as they
 * become available.
 *
 * @param context The [CoroutineContext] to run the operation in.
 * @param other The other channel to merge with.
 * @return The [ReceiveChannel] of merged elements.
 */
fun <E, E1: E, E2: E> ReceiveChannel<E1>.merge(context: CoroutineContext = Unconfined,
                                               other: ReceiveChannel<E2>): ReceiveChannel<E> =
    produce(context) {
        val job = launch(Unconfined) {
            launch(coroutineContext) { toChannel(this@produce) }
            launch(coroutineContext) { other.toChannel(this@produce) }
        }
        job.join()
    }
