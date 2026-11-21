/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.web.server.execution

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.event.TransactionPhase
import org.opendc.web.dispatcher.Dispatcher
import org.opendc.web.server.storage.ObjectStore
import org.opendc.web.server.storage.experimentKey
import org.slf4j.LoggerFactory
import java.util.UUID

/** The platform is to stop these executions, once the decision to stop them is committed. */
data class StopRequested(val executionIds: List<UUID>)

/** These experiments' objects are to go, once their rows are gone. */
data class DiscardRequested(val experimentIds: List<UUID>)

/**
 * What a cancel or a delete asks of things outside the database, done only after its transaction has
 * committed: inside it, a platform call would outlive a rollback and hold rows while it waits.
 */
@ApplicationScoped
class Teardown(
    private val dispatcher: Dispatcher,
    private val store: ObjectStore,
) {
    fun stop(
        @Observes(during = TransactionPhase.AFTER_SUCCESS) event: StopRequested,
    ) {
        for (executionId in event.executionIds) {
            try {
                dispatcher.cancel(executionId)
            } catch (e: Exception) {
                // The units are already cancelled; the platform or reconciliation reports the end.
                LOG.warn("Could not ask the platform to stop execution {}", executionId, e)
            }
        }
    }

    fun discard(
        @Observes(during = TransactionPhase.AFTER_SUCCESS) event: DiscardRequested,
    ) {
        for (experimentId in event.experimentIds) {
            try {
                store.deletePrefix(experimentKey(experimentId))
            } catch (e: Exception) {
                LOG.warn("Could not remove the objects of experiment {}", experimentId, e)
            }
        }
    }

    private companion object {
        val LOG = LoggerFactory.getLogger(Teardown::class.java)
    }
}
