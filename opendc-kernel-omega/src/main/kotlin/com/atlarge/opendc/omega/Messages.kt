package com.atlarge.opendc.omega

import com.atlarge.opendc.simulator.Context
import com.atlarge.opendc.simulator.Process

/**
 * An internal message used by the Omega simulation kernel to indicate to a suspended [Process], that it should wake up
 * and resume execution.
 *
 * This message is not guaranteed to work on other simulation kernels and [Context.interrupt] should be preferred to
 * wake up a process from another entity.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
object Resume

/**
 * An internal message used by the Omega simulation kernel to indicate to a suspended [Process], that a timeout has been
 * reached and that it should wake up and resume execution.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
object Timeout

/**
 * An internal message used by the Omega simulation kernel to launch a process.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
data class Launch<M>(val process: Process<*, M>)
