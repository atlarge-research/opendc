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

package com.atlarge.odcsim.internal

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.Duration
import com.atlarge.odcsim.Signal
import com.atlarge.odcsim.TimerScheduler

/**
 * Implementation of [TimerScheduler] that uses the actor's [ActorContext] to provide timer functionality.
 *
 * @property ctx The actor context to use.
 */
internal class TimerSchedulerImpl<T : Any>(private val ctx: ActorContext<T>) : TimerScheduler<T> {
    private val timers = mutableMapOf<Any, Timer<T>>()

    override fun cancel(key: Any) {
        val timer = timers[key] ?: return
        ctx.log.debug("Cancel timer [{}] with generation [{}]", timer.key, timer.generation)
        timers -= timer.key
    }

    override fun cancelAll() {
        ctx.log.debug("Cancel all timers")
        timers.clear()
    }

    override fun isTimerActive(key: Any): Boolean = timers.containsKey(key)

    override fun startPeriodicTimer(key: Any, msg: T, interval: Duration) {
        startTimer(key, msg, interval, true)
    }

    override fun startSingleTimer(key: Any, msg: T, delay: Duration) {
        startTimer(key, msg, delay, false)
    }

    override fun every(key: Any, interval: Duration, block: () -> Unit) {
        @Suppress("UNCHECKED_CAST")
        startTimer(key, Block(block) as T, interval, true)
    }

    override fun after(key: Any, delay: Duration, block: () -> Unit) {
        @Suppress("UNCHECKED_CAST")
        startTimer(key, Block(block) as T, delay, false)
    }

    private fun startTimer(key: Any, msg: T, duration: Duration, repeat: Boolean) {
        val timer = timers.getOrPut(key) { Timer(key) }
        timer.duration = duration
        timer.generation += 1
        timer.msg = msg
        timer.repeat = repeat
        ctx.sendSignal(ctx.self, TimerSignal(key, timer.generation), duration)
        ctx.log.debug("Start timer [{}] with generation [{}]", key, timer.generation)
    }

    fun interceptTimerSignal(signal: TimerSignal): T? {
        val timer = timers[signal.key]

        if (timer == null) {
            // Message was from canceled timer that was already enqueued
            ctx.log.debug("Received timer [{}] that has been removed, discarding", signal.key)
            return null
        } else if (signal.generation != timer.generation) {
            // Message was from an old timer that was enqueued before canceled
            ctx.log.debug("Received timer [{}] from old generation [{}], expected generation [{}], discarding",
                signal.key, signal.generation, timer.generation)
        }

        if (!timer.repeat) {
            timers -= timer.key
        } else {
            ctx.sendSignal(ctx.self, signal, timer.duration)
        }

        val msg = timer.msg

        if (msg is Block) {
            msg()
            return null
        }

        return msg
    }

    data class Timer<T : Any>(val key: Any) {
        var duration: Duration = 0.0
        var repeat: Boolean = false
        var generation: Int = 0
        lateinit var msg: T
    }

    data class TimerSignal(val key: Any, val generation: Int) : Signal

    data class Block(val block: () -> Unit) {
        operator fun invoke() = block()
    }
}
