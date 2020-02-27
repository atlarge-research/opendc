package com.atlarge.opendc.compute.virt.service.allocation

import com.atlarge.opendc.compute.metal.Node

/**
 * A policy for selecting the [Node] an image should be deployed to,
 */
interface AllocationPolicy {
    /**
     * Builds the logic of the policy.
     */
    suspend operator fun invoke(): Comparator<Node>
}
