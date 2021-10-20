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

package org.opendc.experiments.radice.util

import io.jenetics.Gene
import io.jenetics.Genotype
import io.jenetics.Phenotype
import io.jenetics.engine.Evaluator
import io.jenetics.util.ISeq
import io.jenetics.util.Seq
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.stream.Stream

/**
 * An [Evaluator] that uses a [ForkJoinPool] to perform the evaluation.
 *
 * @param pool The [ForkJoinPool] to run the evaluations in.
 * @param fitness The fitness function to run.
 */
internal class RadiceConcurrentEvaluator<G : Gene<*, G>, C : Comparable<C>>(
    private val pool: ForkJoinPool,
    private val fitness: (Genotype<G>) -> C
) : Evaluator<G, C> {
    override fun eval(population: Seq<Phenotype<G, C>>): ISeq<Phenotype<G, C>> {
        return pool.submit(
            Callable {
                val remainder = population.stream()
                    .filter { it.isEvaluated }

                val evaluated = population.stream()
                    .filter { it.nonEvaluated() }
                    .parallel()
                    .map { pt -> pt.withFitness(fitness(pt.genotype())) }

                Stream.concat(evaluated, remainder).collect(ISeq.toISeq())
            }
        ).get()
    }
}
