/*
 * MIT License
 *
 * Copyright (c) 2017 tudelft-atlarge
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

package nl.tudelft.opendc.sampler

/**
 * A sampler generates data points (samples) of the simulation based on the events that occur to [Entity] instances that
 * are part of the simulation.
 *
 * <p>[Sampler]s work by observing [Entity] instances in the simulation and transforming this stream of events into a
 * stream of data points.
 *
 * <p>An example would be a sampler that tracks [Machine] occupation per time unit, which is achieved by observing the
 * [Entity]'s event stream and filtering for [JobAssignment] events.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Sampler
