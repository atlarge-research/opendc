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
        val system = factory(object : Behavior<Unit> {}, name)

        assertEquals(name, system.name)
    }

    /**
     * Test whether the created [ActorSystem]  has a path.
     */
    @Test
    fun `should have a path`() {
        val system = factory(object : Behavior<Unit> {}, "test")

        assertTrue(system.path is ActorPath.Root)
    }

    /**
     * Test whether creating an [ActorSystem] sets the initial time at 0.
     */
    @Test
    fun `should start at t=0`() {
        val system = factory(object : Behavior<Unit> {}, name = "test")

        assertTrue(Math.abs(system.time) < DELTA)
    }

    /**
     * Test whether an [ActorSystem] does not accept invalid points in time.
     */
    @Test
    fun `should not accept negative instants for running`() {
        val system = factory(object : Behavior<Unit> {}, name = "test")
        assertThrows<IllegalArgumentException> { system.run(-10.0) }
    }

    /**
     * Test whether an [ActorSystem] will not jump backward in time when asking to run until a specified instant
     * that has already occurred.
     */
    @Test
    fun `should not jump backward in time`() {
        val until = 10.0
        val system = factory(object : Behavior<Unit> {}, name = "test")

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
        val system = factory(object : Behavior<Unit> {}, name = "test")

        system.run(until = until)
        assertTrue(Math.abs(system.time - until) < DELTA)
    }

    /**
     * Test whether an [ActorSystem] will jump forward in time when asking to run until a specified instant.
     */
    @Test
    fun `should order messages at the instant by insertion time`() {
        val behavior = object : Behavior<Int> {
            override fun receive(ctx: ActorContext<Int>, msg: Int): Behavior<Int> {
                assertEquals(1, msg)
                return object : Behavior<Int> {
                    override fun receive(ctx: ActorContext<Int>, msg: Int): Behavior<Int> {
                        assertEquals(2, msg)
                        return this
                    }
                }
            }
        }
        val system = factory(behavior, name = "test")
        system.send(1, after = 1.0)
        system.send(2, after = 1.0)
        system.run(until = 10.0)
    }

    @Nested
    @DisplayName("ActorRef")
    inner class ActorRefTest {
        /**
         * Test whether an [ActorSystem] disallows sending messages in the past.
         */
        @Test
        fun `should disallow messages in the past`() {
            val system = factory(object : Behavior<Unit> {}, name = "test")
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
            val behavior = object : Behavior<Unit> {
                override fun receiveSignal(ctx: ActorContext<Unit>, signal: Signal): Behavior<Unit> {
                    assertTrue(Math.abs(ctx.time) < DELTA)
                    return this
                }
            }

            val system = factory(behavior, "test")
            system.run()
        }

        /**
         * Test whether a child actor can be created from an actor.
         */
        @Test
        fun `should allow spawning of child actors`() {
            val behavior = object : Behavior<Unit> {
                override fun receiveSignal(ctx: ActorContext<Unit>, signal: Signal): Behavior<Unit> {
                    throw UnsupportedOperationException("b")
                }
            }

            val system = factory(object : Behavior<Unit> {
                override fun receiveSignal(ctx: ActorContext<Unit>, signal: Signal): Behavior<Unit> {
                    if (signal is PreStart) {
                        val ref = ctx.spawn(behavior, "child")
                        assertEquals("child", ref.path.name)
                    }

                    return this
                }
            }, name = "test")

            assertThrows<UnsupportedOperationException> { system.run(until = 10.0) }
        }

        /**
         * Test whether a child actor can be stopped from an actor.
         */
        @Test
        fun `should allow stopping of child actors`() {
            val system = factory(object : Behavior<Unit> {
                override fun receiveSignal(ctx: ActorContext<Unit>, signal: Signal): Behavior<Unit> {
                    if (signal is PreStart) {
                        val ref = ctx.spawn(object : Behavior<Unit> {
                            override fun receive(ctx: ActorContext<Unit>, msg: Unit): Behavior<Unit> {
                                throw UnsupportedOperationException()
                            }
                        }, "child")
                        assertTrue(ctx.stop(ref))
                        assertEquals("child", ref.path.name)
                    }

                    return this
                }
            }, name = "test")

            system.run(until = 10.0)
        }

        /**
         * Test whether only the parent of a child can terminate it.
         */
        @Test
        fun `should only be able to terminate child actors`() {
            val system = factory(object : Behavior<Unit> {
                override fun receiveSignal(ctx: ActorContext<Unit>, signal: Signal): Behavior<Unit> {
                    val child1 = ctx.spawn(object : Behavior<Unit> {}, "child-1")
                    ctx.spawn(object : Behavior<Unit> {
                        override fun receiveSignal(ctx: ActorContext<Unit>, signal: Signal): Behavior<Unit> {
                            assertFalse(ctx.stop(child1))
                            return this
                        }
                    }, "child-2")
                    return this
                }
            }, name = "test")
            system.run()
        }

        /**
         * Test whether terminating an already terminated child fails.
         */
        @Test
        fun `should not be able to stop an already terminated child`() {
            val system = factory(object : Behavior<Unit> {
                override fun receiveSignal(ctx: ActorContext<Unit>, signal: Signal): Behavior<Unit> {
                    val child = ctx.spawn(object : Behavior<Unit> {}, "child")
                    ctx.stop(child)
                    assertFalse(ctx.stop(child))
                    return this
                }
            }, name = "test")
            system.run()
        }

        /**
         * Test whether termination of a child also results in termination of its children.
         */
        @Test
        fun `should terminate children of child when terminating it`() {
            val system = factory(object : Behavior<ActorRef<Unit>> {
                lateinit var child: ActorRef<Unit>

                override fun receive(ctx: ActorContext<ActorRef<Unit>>, msg: ActorRef<Unit>): Behavior<ActorRef<Unit>> {
                    assertTrue(ctx.stop(child))
                    msg.send(Unit) // This actor should be stopped now and not receive the message anymore
                    return this
                }

                override fun receiveSignal(ctx: ActorContext<ActorRef<Unit>>, signal: Signal): Behavior<ActorRef<Unit>> {
                    val root = ctx.self
                    child = ctx.spawn(object : Behavior<Unit> {
                        override fun receiveSignal(ctx: ActorContext<Unit>, signal: Signal): Behavior<Unit> {
                            if (signal is PreStart) {
                                val child = ctx.spawn(object : Behavior<Unit> {
                                    override fun receive(ctx: ActorContext<Unit>, msg: Unit): Behavior<Unit> {
                                        throw IllegalStateException()
                                    }
                                }, "child")
                                root.send(child)
                            }
                            return this
                        }
                    }, "child")
                    return this
                }
            }, name = "test")
            system.run()
        }

        /**
         * Test whether the reference to the actor itself is valid.
         */
        @Test
        fun `should have reference to itself`() {
            val behavior = object : Behavior<Unit> {
                override fun receive(ctx: ActorContext<Unit>, msg: Unit): Behavior<Unit> {
                    throw UnsupportedOperationException()
                }
                override fun receiveSignal(ctx: ActorContext<Unit>, signal: Signal): Behavior<Unit> {
                    ctx.self.send(Unit)
                    return this
                }
            }

            val system = factory(behavior, "test")
            assertThrows<UnsupportedOperationException> { system.run() }
        }
    }
}
