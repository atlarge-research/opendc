package org.opendc.simulator.flow.interference

import org.opendc.simulator.flow.FlowSource

/**
 * An interference domain represents a system of flow stages where [flow sources][FlowSource] may incur
 * performance variability due to operating on the same resource and therefore causing interference.
 */
public interface InterferenceDomain {
    /**
     * Compute the performance score of a participant in this interference domain.
     *
     * @param key The participant to obtain the score of or `null` if the participant has no key.
     * @param load The overall load on the interference domain.
     * @return A score representing the performance score to be applied to the resource consumer, with 1
     * meaning no influence, <1 means that performance degrades, and >1 means that performance improves.
     */
    public fun apply(key: InterferenceKey?, load: Double): Double
}
