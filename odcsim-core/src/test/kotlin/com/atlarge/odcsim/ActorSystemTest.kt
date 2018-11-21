/*
 * MIT License
 *
 * Copyright (c) 2018 atlarge-research
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

import com.atlarge.odcsim.Behavior.Companion.same
import com.atlarge.odcsim.dsl.empty
import com.atlarge.odcsim.dsl.ignore
import com.atlarge.odcsim.dsl.receive
import com.atlarge.odcsim.dsl.receiveMessage
import com.atlarge.odcsim.dsl.setup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

const val DELTA: Double = 0.0001

/**
 * A conformance test suite for implementors of the [ActorSystem] interface.
 */
abstract class ActorSystemTest {
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
        val system = factory(Behavior.empty<Unit>(), name)

        assertEquals(name, system.name)
    }

    /**
     * Test whether the created [ActorSystem]  has a path.
     */
    @Test
    fun `should have a path`() {
        val system = factory(Behavior.empty<Unit>(), "test")

        assertTrue(system.path is ActorPath.Root)
    }

    /**
     * Test whether creating an [ActorSystem] sets the initial time at 0.
     */
    @Test
    fun `should start at t=0`() {
        val system = factory(Behavior.empty<Unit>(), name = "test")

        assertTrue(Math.abs(system.time) < DELTA)
    }

    /**
     * Test whether an [ActorSystem] does not accept invalid points in time.
     */
    @Test
    fun `should not accept negative instants for running`() {
        val system = factory(Behavior.empty<Unit>(), name = "test")
        assertThrows<IllegalArgumentException> { system.run(-10.0) }
    }

    /**
     * Test whether an [ActorSystem] will not jump backward in time when asking to run until a specified instant
     * that has already occurred.
     */
    @Test
    fun `should not jump backward in time`() {
        val until = 10.0
        val system = factory(Behavior.empty<Unit>(), name = "test")

        system.run(until = until)
        system.run(until = until - 0.5)
        assertTrue(Math.abs(system.time - until) < DELTA)
    }

    /**
     * Test whether an [ActorSystem] will jump forward in time when asking to run until a specified instant.
     */
    @Test
    fun `should jump forward in time`() {
        val until = 10.0
        val system = factory(Behavior.empty<Unit>(), name = "test")

        system.run(until = until)
        assertTrue(Math.abs(system.time - until) < DELTA)
    }

    /**
     * Test whether an [ActorSystem] will jump forward in time when asking to run until a specified instant.
     */
    @Test
    fun `should order messages at the instant by insertion time`() {
        val behavior = Behavior.receiveMessage<Int> { msg ->
            assertEquals(1, msg)

            Behavior.receiveMessage {
                assertEquals(2, it)
                Behavior.ignore()
            }
        }
        val system = factory(behavior, name = "test")
        system.send(1, after = 1.0)
        system.send(2, after = 1.0)
        system.run(until = 10.0)
    }

    /**
     * Test whether an [ActorSystem] will not process messages in the queue after the deadline.
     */
    @Test
    fun `should not process messages after deadline`() {
        var counter = 0
        val behavior = Behavior.receiveMessage<Unit> { _ ->
            counter++
            same()
        }
        val system = factory(behavior, name = "test")
        system.send(Unit, after = 3.0)
        system.send(Unit, after = 1.0)
        system.run(until = 2.0)
        assertEquals(1, counter)
    }

    /**
     * Test whether an [ActorSystem] will not initialize the root actor if the system has not been run yet.
     */
    @Test
    fun `should not initialize root actor if not run`() {
        factory(Behavior.setup<Unit> { TODO() }, name = "test")
    }

    @Nested
    @DisplayName("ActorRef")
    inner class ActorRefTest {
        /**
         * Test whether an [ActorSystem] disallows sending messages in the past.
         */
        @Test
        fun `should disallow messages in the past`() {
            val system = factory(Behavior.empty<Unit>(), name = "test")
            assertThrows<IllegalArgumentException> { system.send(Unit, after = -1.0) }
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
            val behavior = Behavior.setup<Unit> { ctx ->
                assertTrue(Math.abs(ctx.time) < DELTA)
                Behavior.ignore()
            }

            val system = factory(behavior, "test")
            system.run()
        }

        /**
         * Test whether a child actor can be created from an actor.
         */
        @Test
        fun `should allow spawning of child actors`() {
            var spawned = false
            val behavior = Behavior.setup<Unit> { spawned = true; Behavior.empty() }

            val system = factory(Behavior.setup<Unit> { ctx ->
                val ref = ctx.spawn(behavior, "child")
                assertEquals("child", ref.path.name)
                Behavior.ignore()
            }, name = "test")

            system.run(until = 10.0)
            assertTrue(spawned)
        }

        /**
         * Test whether a child actor can be stopped from an actor.
         */
        @Test
        fun `should allow stopping of child actors`() {
            val system = factory(Behavior.setup<Unit> { ctx ->
                val ref = ctx.spawn(Behavior.receiveMessage<Unit> { throw UnsupportedOperationException() }, "child")
                assertTrue(ctx.stop(ref))
                assertEquals("child", ref.path.name)

                Behavior.ignore()
            }, name = "test")

            system.run(until = 10.0)
        }

        /**
         * Test whether only the parent of a child can terminate it.
         */
        @Test
        fun `should only be able to terminate child actors`() {
            val system = factory(Behavior.setup<Unit> { ctx ->
                val child1 = ctx.spawn(Behavior.ignore<Unit>(), "child-1")

                ctx.spawn(Behavior.setup<Unit> {
                    assertFalse(it.stop(child1))
                    Behavior.ignore()
                }, "child-2")

                Behavior.ignore()
            }, name = "test")
            system.run()
        }

        /**
         * Test whether terminating an already terminated child fails.
         */
        @Test
        fun `should not be able to stop an already terminated child`() {
            val system = factory(Behavior.setup<Unit> { ctx ->
                val child = ctx.spawn(Behavior.ignore<Unit>(), "child")
                ctx.stop(child)
                assertFalse(ctx.stop(child))
                Behavior.ignore()
            }, name = "test")
            system.run()
        }

        /**
         * Test whether termination of a child also results in termination of its children.
         */
        @Test
        fun `should terminate children of child when terminating it`() {
            val system = factory(Behavior.setup<ActorRef<Unit>> { ctx1 ->
                val root = ctx1.self
                val child = ctx1.spawn(Behavior.setup<Unit> {
                    val child = ctx1.spawn(Behavior.receiveMessage<Unit> {
                        throw IllegalStateException("DELIBERATE")
                    }, "child")
                    root.send(child)
                    Behavior.ignore()
                }, "child")

                Behavior.receive { ctx2, msg ->
                    assertTrue(ctx2.stop(child))
                    msg.send(Unit) // This actor should be stopped now and not receive the message anymore
                    Behavior.stopped()
                }
            }, name = "test")

            system.run()
        }

        /**
         * Test whether [Behavior.Companion.same] works correctly.
         */
        @Test
        fun `should keep same behavior on same`() {
            var counter = 0

            val behavior = Behavior.setup<Unit> { ctx ->
                    counter++
                    ctx.self.send(Unit)

                Behavior.receiveMessage {
                    counter++
                    Behavior.same()
                }
            }

            val system = factory(behavior, "test")
            system.run()
            assertEquals(2, counter)
        }

        /**
         * Test whether the reference to the actor itself is valid.
         */
        @Test
        fun `should have reference to itself`() {
            var flag = false
            val behavior: Behavior<Unit> = Behavior.setup { ctx ->
                ctx.self.send(Unit)
                Behavior.receiveMessage { flag = true; same() }
            }

            val system = factory(behavior, "test")
            system.run()
            assertTrue(flag)
        }

        /**
         * Test whether we cannot start an actor with the [Behavior.Companion.same] behavior.
         */
        @Test
        fun `should not start with same behavior`() {
            val system = factory(Behavior.same<Unit>(), "test")
            assertThrows<IllegalArgumentException> { system.run() }
        }

        /**
         * Test whether we can start an actor with the [Behavior.Companion.stopped] behavior.
         */
        @Test
        fun `should start with stopped behavior`() {
            val system = factory(Behavior.stopped<Unit>(), "test")
            system.run()
        }


        /**
         * Test whether an actor that is crashed cannot receive more messages.
         */
        @Test
        fun `should stop if it crashes`() {
            var counter = 0
            val system = factory(Behavior.receiveMessage<Unit> {
                counter++
                throw IllegalArgumentException("STAGED")
            }, "test")

            system.send(Unit)
            system.send(Unit)

            system.run()
            assertEquals(1, counter)
        }
    }
}
