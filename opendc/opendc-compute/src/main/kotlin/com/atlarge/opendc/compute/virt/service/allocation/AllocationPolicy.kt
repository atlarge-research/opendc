package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.metal.Node
import com.atlarge.opendc.compute.virt.service.HypervisorView
import com.atlarge.opendc.compute.virt.service.SimpleVirtProvisioningService

/**
 * A policy for selecting the [Node] an image should be deployed to,
 */
public interface AllocationPolicy {
    /**
     * The logic of the allocation policy.
     */
    public interface Logic {
        /**
         * Select the node on which the server should be scheduled.
         */
        public fun select(hypervisors: Set<HypervisorView>, image: SimpleVirtProvisioningService.ImageView): HypervisorView?
    }

    /**
     * Builds the logic of the policy.
     */
    operator fun invoke(): Logic
}
