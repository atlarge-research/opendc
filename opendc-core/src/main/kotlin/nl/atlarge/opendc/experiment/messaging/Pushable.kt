package nl.atlarge.opendc.experiment.messaging

import nl.atlarge.opendc.topology.Entity

/**
 * A [Pushable] instance allows objects to send messages to it.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Pushable {
	/**
	 * Push one message to downstream.
	 *
	 * @param msg The message to send downstream.
	 * @param sender The sender of the message.
	 */
	fun push(msg: Any?, sender: Entity)
}
