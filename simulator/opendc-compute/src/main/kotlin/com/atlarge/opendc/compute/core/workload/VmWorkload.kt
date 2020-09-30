package com.atlarge.opendc.compute.core.workload

import com.atlarge.opendc.compute.core.image.VmImage
import com.atlarge.opendc.core.User
import com.atlarge.opendc.core.workload.Workload
import java.util.UUID

/**
 * A workload that represents a VM.
 *
 * @property uid A unique identified of this VM.
 * @property name The name of this VM.
 * @property owner The owner of the VM.
 * @property image The image of the VM.
 */
data class VmWorkload(
    override val uid: UUID,
    override val name: String,
    override val owner: User,
    val image: VmImage
) : Workload {
    override fun equals(other: Any?): Boolean = other is VmWorkload && uid == other.uid

    override fun hashCode(): Int = uid.hashCode()
}
