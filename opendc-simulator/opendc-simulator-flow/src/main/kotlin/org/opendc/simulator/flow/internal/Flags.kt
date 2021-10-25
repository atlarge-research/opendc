/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.simulator.flow.internal

/**
 * States of the flow connection.
 */
internal const val ConnPending = 0 // Connection is pending and the consumer is waiting to consume the source
internal const val ConnActive = 1 // Connection is active and the source is currently being consumed
internal const val ConnClosed = 2 // Connection is closed and source cannot be consumed through this connection anymore
internal const val ConnState = 0b11 // Mask for accessing the state of the flow connection

/**
 * Flags of the flow connection
 */
internal const val ConnPulled = 1 shl 2 // The source should be pulled
internal const val ConnPushed = 1 shl 3 // The source has pushed a value
internal const val ConnClose = 1 shl 4 // The connection should be closed
internal const val ConnUpdateActive = 1 shl 5 // An update for the connection is active
internal const val ConnUpdatePending = 1 shl 6 // An (immediate) update of the connection is pending
internal const val ConnConvergePending = 1 shl 7 // Indication that a convergence is already pending
internal const val ConnConvergeSource = 1 shl 8 // Enable convergence of the source
internal const val ConnConvergeConsumer = 1 shl 9 // Enable convergence of the consumer
internal const val ConnDisableTimers = 1 shl 10 // Disable timers for the source
