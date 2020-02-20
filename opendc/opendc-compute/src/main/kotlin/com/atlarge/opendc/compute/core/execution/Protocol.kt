/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.compute.core.execution

import com.atlarge.odcsim.ReceivePort
import com.atlarge.odcsim.SendPort
import com.atlarge.odcsim.processContext
import com.atlarge.opendc.compute.core.Server
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Request that are accepted by a [ServerContext] instance.
 */
public sealed class ServerRequest {
    /**
     * Request the context to be initialized.
     */
    public object Initialize : ServerRequest()

    /**
     * Request for each core the specified amount of cpu time to run from the server.
     */
    public data class Run(public val req: LongArray, public val reqDuration: Long) : ServerRequest()

    /**
     * Terminate the execution of the server.
     */
    public data class Exit(public val cause: Throwable? = null) : ServerRequest()
}

/**
 * Messages sent in response to [ServerRequest] objects.
 */
public sealed class ServerResponse {
    /**
     * The server that sent this response.
     */
    public abstract val server: Server

    /**
     * The amount cpu time granted on this server.
     */
    public abstract val rec: LongArray?

    /**
     * Indicate that this request was processed successfully.
     */
    public data class Ok(
        public override val server: Server,
        public override val rec: LongArray? = null
    ) : ServerResponse()
}

/**
 * Serialize the specified [ServerManagementContext] instance in order to safely send this object across logical
 * processes.
 */
public suspend fun ServerManagementContext.serialize(): ServerManagementContext {
    val ctx = processContext
    val input = ctx.open<ServerRequest>()
    val output = ctx.open<ServerResponse>()

    ctx.launch {
        val outlet = processContext.connect(output.send)
        val inlet = processContext.listen(input.receive)

        while (isActive) {
            when (val msg = inlet.receive()) {
                is ServerRequest.Initialize -> {
                    init()
                    outlet.send(ServerResponse.Ok(server))
                }
                is ServerRequest.Run -> {
                    val rec = run(msg.req, msg.reqDuration)
                    outlet.send(ServerResponse.Ok(server, rec))
                }
                is ServerRequest.Exit -> {
                    exit(msg.cause)
                    outlet.send(ServerResponse.Ok(server))
                }
            }
        }
    }

    return object : ServerManagementContext {
        private lateinit var inlet: ReceivePort<ServerResponse>
        private lateinit var outlet: SendPort<ServerRequest>

        override var server: Server = this@serialize.server

        override suspend fun run(req: LongArray, reqDuration: Long): LongArray {
            outlet.send(ServerRequest.Run(req, reqDuration))

            when (val res = inlet.receive()) {
                is ServerResponse.Ok -> {
                    server = res.server
                    return res.rec ?: error("Received should be defined in this type of request.")
                }
            }
        }

        override suspend fun exit(cause: Throwable?) {
            outlet.send(ServerRequest.Exit(cause))

            when (val res = inlet.receive()) {
                is ServerResponse.Ok -> {
                    server = res.server
                }
            }
        }

        override suspend fun init() {
            if (!this::outlet.isInitialized) {
                outlet = processContext.connect(input.send)
            }

            if (!this::inlet.isInitialized) {
                inlet = processContext.listen(output.receive)
            }

            outlet.send(ServerRequest.Initialize)
            when (val res = inlet.receive()) {
                is ServerResponse.Ok -> {
                    server = res.server
                }
            }
        }
    }
}
