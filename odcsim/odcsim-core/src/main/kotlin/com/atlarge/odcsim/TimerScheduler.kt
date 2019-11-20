/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.atlarge.odcsim

/**
 * An interface to provide support for scheduled self messages in an actor. It is used with [withTimers].
 * Timers are bound to the lifecycle of the actor that owns it, and thus are cancelled automatically when it is
 * restarted or stopped.
 *
 * Please be aware that [TimerScheduler] is not thread-safe and must only be used within the actor that owns it.
 *
 * @param T The shape of the messages the owning actor of this scheduling accepts.
 */
interface TimerScheduler<T : Any> {
    /**
     * Cancel a timer with the given key.
     *
     * @param key The key of the timer.
     */
    fun cancel(key: Any)

    /**
     * Cancel all timers.
     */
    fun cancelAll()

    /**
     * Check if a timer with a given [key] is active.
     *
     * @param key The key to check if it is active.
     * @return `true` if a timer with the specified key is active, `false` otherwise.
     */
    fun isTimerActive(key: Any): Boolean

    /**
     * Start a periodic timer that will send [msg] to the `self` actor at a fixed [interval].
     *
     * @param key The key of the timer.
     * @param msg The message to send to the actor.
     * @param interval The interval of simulation time after which it should be sent.
     */
    fun startPeriodicTimer(key: Any, msg: T, interval: Duration)

    /**
     * Start a timer that will send [msg] once to the `self` actor after the given [delay].
     *
     * @param key The key of the timer.
     * @param msg The message to send to the actor.
     * @param delay The delay in simulation time after which it should be sent.
     */
    fun startSingleTimer(key: Any, msg: T, delay: Duration)

    /**
     * Run [block] periodically at a fixed [interval]
     *
     * @param key The key of the timer.
     * @param interval The delay of simulation time after which the block should run.
     * @param block The block to run.
     */
    fun every(key: Any, interval: Duration, block: () -> Unit)

    /**
     * Run [block] after the specified [delay].
     *
     * @param key The key of the timer.
     * @param delay The delay in simulation time after which the block should run.
     * @param block The block to run.
     */
    fun after(key: Any, delay: Duration, block: () -> Unit)
}
