/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.common.util;

import java.util.function.LongConsumer;
import org.opendc.common.Dispatcher;
import org.opendc.common.DispatcherHandle;

/**
 * Helper class to pace the incoming scheduling requests.
 */
public final class Pacer {
    private final Dispatcher dispatcher;
    private final long quantumMs;
    private final LongConsumer process;

    /**
     * The current {@link DispatcherHandle} representing the pending scheduling cycle.
     */
    private DispatcherHandle handle;

    /**
     * Construct a {@link Pacer} instance.
     *
     * @param dispatcher The {@link Dispatcher} to schedule future invocations.
     * @param quantumMs The scheduling quantum in milliseconds.
     * @param process The process to invoke for the incoming requests.
     */
    public Pacer(Dispatcher dispatcher, long quantumMs, LongConsumer process) {
        this.dispatcher = dispatcher;
        this.quantumMs = quantumMs;
        this.process = process;
    }

    /**
     * Determine whether a scheduling cycle is pending.
     */
    public boolean isPending() {
        return handle != null;
    }

    /**
     * Enqueue a new scheduling cycle.
     */
    public void enqueue() {
        if (handle != null) {
            return;
        }

        final Dispatcher dispatcher = this.dispatcher;
        long quantumMs = this.quantumMs;
        long now = dispatcher.getTimeSource().millis();

        // We assume that the scheduler runs at a fixed slot every time quantum (e.g t=0, t=60, t=120).
        // We calculate here the delay until the next scheduling slot.
        long timeUntilNextSlot = quantumMs - (now % quantumMs);

        handle = dispatcher.scheduleCancellable(timeUntilNextSlot, () -> {
            process.accept(now + timeUntilNextSlot);
            handle = null;
        });
    }

    /**
     * Cancel the currently pending scheduling cycle.
     */
    public void cancel() {
        final DispatcherHandle handle = this.handle;
        if (handle != null) {
            this.handle = null;
            handle.cancel();
        }
    }
}
