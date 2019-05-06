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

package com.atlarge.odcsim.engine.tests

import com.atlarge.odcsim.ActorPath
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.ActorSystemFactory
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.Terminated
import com.atlarge.odcsim.coroutines.dsl.timeout
import com.atlarge.odcsim.coroutines.suspending
import com.atlarge.odcsim.empty
import com.atlarge.odcsim.ignore
import com.atlarge.odcsim.receiveMessage
import com.atlarge.odcsim.receiveSignal
import com.atlarge.odcsim.same
import com.atlarge.odcsim.setup
import com.atlarge.odcsim.stopped
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * A conformance test suite for implementors of the [ActorSystem] interface.
 */
abstract class ActorSystemContract {
    /**
     * An [ActorSystemFactory] provided by implementors to create the [ActorSystem] to be tested.
     */
    abstract val factory: ActorSystemFactory

    /**
     * Test whether the created [ActorSystem] has the correct name.
     */
    @Test
    fun `should have a name`() {
        val name = "test"
        val system = factory(empty<Unit>(), name)

        assertEquals(name, system.name)
        system.terminate()
    }

    /**
     * Test whether the created [ActorSystem]  has a path.
     */
    @Test
    fun `should have a path`() {
        val system = factory(empty<Unit>(), "test")

        assertTrue(system.path is ActorPath.Root)
        system.terminate()
    }

    /**
     * Test whether creating an [ActorSystem] sets the initial time at 0.
     */
    @Test
    fun `should start at t=0`() {
        val system = factory(empty<Unit>(), name = "test")

        assertEquals(.0, system.time, DELTA)
        system.terminate()
    }

    /**
     * Test whether an [ActorSystem] does not accept invalid points in time.
     */
    @Test
    fun `should not accept negative instants for running`() {
        val system = factory(empty<Unit>(), name = "test")
        assertThrows<IllegalArgumentException> { system.run(-10.0) }
        system.terminate()
    }

    /**
     * Test whether an [ActorSystem] will not jump backward in time when asking to run until a specified instant
     * that has already occurred.
     */
    @Test
    fun `should not jump backward in time`() {
        val until = 10.0
        val system = factory(empty<Unit>(), name = "test")

        system.run(until = until)
        system.run(until = until - 0.5)
        assertEquals(until, system.time, DELTA)
        system.terminate()
    }

    /**
     * Test whether an [ActorSystem] will jump forward in time when asking to run until a specified instant.
     */
    @Test
    fun `should jump forward in time`() {
        val until = 10.0
        val system = factory(empty<Unit>(), name = "test")

        system.run(until = until)
        assertEquals(until, system.time, DELTA)
        system.terminate()
    }

    /**
     * Test whether an [ActorSystem] will jump forward in time when asking to run until a specified instant.
     */
    @Test
    fun `should order messages at the instant by insertion time`() {
        val behavior = receiveMessage<Int> { msg ->
            assertEquals(1, msg)
            receiveMessage {
                assertEquals(2, it)
                ignore()
            }
        }
        val system = factory(behavior, name = "test")
        system.send(1, after = 1.0)
        system.send(2, after = 1.0)
        system.run(until = 10.0)
        system.terminate()
    }

    /**
     * Test whether an [ActorSystem] will not process messages in the queue after the deadline.
     */
    @Test
    fun `should not process messages after deadline`() {
        var counter = 0
        val behavior = receiveMessage<Unit> { _ ->
            counter++
            same()
        }
        val system = factory(behavior, name = "test")
        system.send(Unit, after = 3.0)
        system.send(Unit, after = 1.0)
        system.run(until = 2.0)
        assertEquals(1, counter)
        system.terminate()
    }

    /**
     * Test whether an [ActorSystem] will not initialize the root actor if the system has not been run yet.
     */
    @Test
    fun `should not initialize root actor if not run`() {
        val system = factory(setup<Unit> { TODO() }, name = "test")
        system.terminate()
    }

    @Nested
    @DisplayName("ActorRef")
    inner class ActorRefTest {
        /**
         * Test whether an [ActorSystem] disallows sending messages in the past.
         */
        @Test
        fun `should disallow messages in the past`() {
            val system = factory(empty<Unit>(), name = "test")
            assertThrows<IllegalArgumentException> { system.send(Unit, after = -1.0) }
            system.terminate()
        }
    }

    @Nested
    @DisplayName("Actor")
    inner class Actor {
        /**
         * Test whether the pre-start time of the root actor is at 0.
         */
        @Test
        fun `should pre-start at t=0 if root`() {
            val behavior = setup<Unit> { ctx ->
                assertEquals(.0, ctx.time, DELTA)
                ignore()
            }

            val system = factory(behavior, "test")
            system.run()
            system.terminate()
        }

        /**
         * Test whether a child actor can be created from an actor.
         */
        @Test
        fun `should allow spawning of child actors`() {
            var spawned = false
            val behavior = setup<Unit> { spawned = true; empty() }

            val system = factory(setup<Unit> { ctx ->
                val ref = ctx.spawn(behavior, "child")
                assertEquals("child", ref.path.name)
                ignore()
            }, name = "test")

            system.run(until = 10.0)
            assertTrue(spawned)
            system.terminate()
        }

        /**
         * Test whether a child actor can be stopped from an actor.
         */
        @Test
        fun `should allow stopping of child actors`() {
            val system = factory(setup<Unit> { ctx ->
                val ref = ctx.spawn(receiveMessage<Unit> { throw UnsupportedOperationException() }, "child")
                assertTrue(ctx.stop(ref))
                assertEquals("child", ref.path.name)

                ignore()
            }, name = "test")

            system.run(until = 10.0)
            system.terminate()
        }

        /**
         * Test whether only the parent of a child can terminate it.
         */
        @Test
        fun `should only be able to terminate child actors`() {
            val system = factory(setup<Unit> { ctx1 ->
                val child1 = ctx1.spawn(ignore<Unit>(), "child-1")
                ctx1.spawn(setup<Unit> { ctx2 ->
                    assertFalse(ctx2.stop(child1))
                    ignore()
                }, "child-2")

                ignore()
            }, name = "test")
            system.run()
            system.terminate()
        }

        /**
         * Test whether terminating an already terminated child fails.
         */
        @Test
        fun `should not be able to stop an already terminated child`() {
            val system = factory(setup<Unit> { ctx ->
                val child = ctx.spawn(ignore<Unit>(), "child")
                ctx.stop(child)
                assertFalse(ctx.stop(child))
                ignore()
            }, name = "test")
            system.run()
            system.terminate()
        }

        /**
         * Test whether termination of a child also results in termination of its children.
         */
        @Test
        fun `should terminate children of child when terminating it`() {
            val system = factory(setup<ActorRef<Unit>> { ctx ->
                val root = ctx.self
                val child = ctx.spawn(setup<Unit> {
                    val child = it.spawn(receiveMessage<Unit> {
                        throw IllegalStateException("DELIBERATE")
                    }, "child")
                    ctx.send(root, child)
                    ignore()
                }, "child")

                receiveMessage { msg ->
                    assertTrue(ctx.stop(child))
                    ctx.send(msg, Unit) // This actor should be stopped now and not receive the message anymore
                    stopped()
                }
            }, name = "test")

            system.run()
            system.terminate()
        }

        /**
         * Test whether [same] works correctly.
         */
        @Test
        fun `should keep same behavior on same`() {
            var counter = 0

            val behavior = setup<Unit> { ctx ->
                counter++
                ctx.send(ctx.self, Unit)
                receiveMessage {
                    counter++
                    same()
                }
            }

            val system = factory(behavior, "test")
            system.run()
            assertEquals(2, counter)
            system.terminate()
        }

        /**
         * Test whether the reference to the actor itself is valid.
         */
        @Test
        fun `should have reference to itself`() {
            var flag = false
            val behavior: Behavior<Unit> = setup { ctx ->
                ctx.send(ctx.self, Unit)
                receiveMessage { flag = true; same() }
            }

            val system = factory(behavior, "test")
            system.run()
            assertTrue(flag)
            system.terminate()
        }

        /**
         * Test whether we can start an actor with the [stopped] behavior.
         */
        @Test
        fun `should start with stopped behavior`() {
            val system = factory(stopped<Unit>(), "test")
            system.run()
            system.terminate()
        }

        /**
         * Test whether an actor that is crashed cannot receive more messages.
         */
        @Test
        fun `should stop if it crashes`() {
            var counter = 0
            val system = factory(receiveMessage<Unit> {
                counter++
                throw IllegalArgumentException("STAGED")
            }, "test")

            system.send(Unit)
            system.send(Unit)

            system.run()
            assertEquals(1, counter)
            system.terminate()
        }

        /**
         * Test whether an actor can watch for termination.
         */
        @Test
        fun `should watch for termination`() {
            var received = false
            val system = factory(setup<Nothing> { ctx ->
                val child = ctx.spawn(suspending<Nothing> {
                    it.timeout(50.0)
                    stopped()
                }, "child")
                ctx.watch(child)

                receiveSignal { _, signal ->
                    when (signal) {
                        is Terminated -> {
                            received = true
                            stopped()
                        }
                        else ->
                            same()
                    }
                }
            }, "test")

            system.run()
            system.terminate()
            assertTrue(received)
        }
    }

    companion object {
        private const val DELTA: Double = 0.0001
    }
}
