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

import java.io.Serializable

/**
 * An actor path represents the unique path to a specific actor instance within an [ActorSystem].
 */
sealed class ActorPath : Comparable<ActorPath>, Serializable {
    /**
     * The name of the actor that this path refers to.
     */
    abstract val name: String

    /**
     * The path for the parent actor.
     */
    abstract val parent: ActorPath

    /**
     * Walk up the tree to obtain and return the [ActorPath.Root].
     */
    abstract val root: Root

    /**
     * Create a new child actor path.
     */
    fun child(name: String): ActorPath = Child(this, name)

    /**
     * Recursively create a descendant’s path by appending all child names.
     */
    fun descendant(children: Iterable<String>): ActorPath = children.fold(this) { parent, name ->
        if (name.isNotBlank()) child(name) else parent
    }

    /**
     * Root of the hierarchy of [ActorPath]s. There is exactly root per [ActorSystem].
     */
    data class Root(override val name: String = "/") : ActorPath() {
        init {
            require(name.length == 1 || name.indexOf('/', 1) == -1) {
                "/ may only exist at the beginning of the root actors name"
            }
            require(name.indexOf('#') == -1) { "# may not exist in a path component"}
        }

        override val parent: ActorPath = this

        override val root: Root = this

        /**
         * Compare the [specified][other] path with this root node for order. If both paths are roots, compare their
         * name, otherwise the root is ordered higher.
         *
         * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater
         * than the specified path.
         */
        override fun compareTo(other: ActorPath): Int = if (other is Root) name.compareTo(other.name) else 1

        /**
         * Create a string representation of this root node which prints its own [name].
         *
         * @return A string representation of this node.
         */
        override fun toString(): String = name
    }

    /**
     * A child in the hierarchy of [ActorPath]s.
     */
    data class Child(override val parent: ActorPath, override val name: String) : ActorPath() {
        init {
            require(name.indexOf('/') == -1) { "/ may not exist in a path component" }
            require(name.indexOf('#') == -1) { "# may not exist in a path component"}
        }

        override val root: Root by lazy {
            when (parent) {
                is Root -> parent
                else -> parent.root
            }
        }

        /**
         * Compare the [specified][other] path with this child node for order.
         *
         * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater
         * than the specified path.
         */
        override fun compareTo(other: ActorPath): Int {
            tailrec fun rec(left: ActorPath, right: ActorPath): Int = when {
                left == right -> 0
                left is Root -> left.compareTo(right)
                right is Root -> -(right.compareTo(left))
                else -> {
                    val x = left.name.compareTo(right.name)
                    if (x == 0)
                        rec(left.parent, right.parent)
                    else
                        x
                }
            }
            return rec(this, other)
        }

        /**
         * Create a string representation of this child node which prints the name of [parent] and its own [name].
         *
         * @return A string representation of this node.
         */
        override fun toString(): String = "$parent/$name"
    }
}
