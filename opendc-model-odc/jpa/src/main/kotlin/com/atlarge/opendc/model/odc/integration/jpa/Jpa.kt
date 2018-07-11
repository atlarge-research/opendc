/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package com.atlarge.opendc.model.odc.integration.jpa

import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consume
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.RollbackException

/**
 * Run the given block in a transaction, committing on return of the block.
 *
 * @param block The block to execute in the transaction.
 */
inline fun EntityManager.transaction(block: () -> Unit) {
    transaction.begin()
    block()
    transaction.commit()
}

/**
 * Write the given channel in batch to the database.
 *
 * @param factory The [EntityManagerFactory] to use to create an [EntityManager] which can persist the entities.
 * @param batchSize The size of each batch.
 */
suspend fun <E> ReceiveChannel<E>.persist(factory: EntityManagerFactory, batchSize: Int = 1000) {
    val writer = factory.createEntityManager()

    this.consume {
        val transaction = writer.transaction
        var counter = 0
        try {
            transaction.begin()

            for (element in this) {
                // Commit batch every batch size
                if (counter > 0 && counter % batchSize == 0) {
                    writer.flush()
                    writer.clear()

                    transaction.commit()
                    transaction.begin()
                }

                writer.persist(element)
                counter++
            }

            transaction.commit()
        } catch(e: RollbackException) {
            // Rollback transaction if still active
            if (transaction.isActive) {
                transaction.rollback()
            }

            throw e
        } finally {
            writer.close()
        }
    }
}
