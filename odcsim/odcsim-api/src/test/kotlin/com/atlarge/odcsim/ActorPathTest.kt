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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * A test suite for the [ActorPath] class.
 */
@DisplayName("ActorPath")
class ActorPathTest {
    /**
     * Test whether an [ActorPath.Root] may only start with a slash.
     */
    @Test
    fun `root node may only start with a slash`() {
        ActorPath.Root() // Assert slash at start
        assertThrows<IllegalArgumentException> { ActorPath.Root("abc/") }
    }

    /**
     * Test whether an [ActorPath.Child] disallows names with a slash.
     */
    @Test
    fun `child node should not allow name with a slash`() {
        assertThrows<IllegalArgumentException> { ActorPath.Child(ActorPath.Root(), "/") }
    }

    /**
     * Test whether a root node can have a custom name.
     */
    @Test
    fun `root node can have a custom name`() {
        val name = "user"
        assertEquals(name, ActorPath.Root(name).name)
    }

    /**
     * Test whether a child node can be created on a root.
     */
    @Test
    fun `child node can be created on a root`() {
        val root = ActorPath.Root(name = "/user")
        val child = root.child("child")

        assertEquals(root, child.parent)
        assertEquals("child", child.name)
    }

    /**
     * Test whether a child node can be created on a child.
     */
    @Test
    fun `child node can be created on a child`() {
        val root = ActorPath.Root(name = "/user").child("child")
        val child = root.child("child")

        assertEquals(root, child.parent)
        assertEquals("child", child.name)
    }
}
