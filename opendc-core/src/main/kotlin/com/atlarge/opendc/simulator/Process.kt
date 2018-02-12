package com.atlarge.opendc.simulator

/**
 * A process is dynamic entity within a simulation, that interacts with the model environment by the interchange of
 * messages.
 *
 * @param S The shape of the observable state of the process.
 * @param M The shape of the model in which the process exists.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Process<S, in M> : Entity<S, M> {
    /**
     * This method is invoked to start the simulation a process.
     *
     * This method is assumed to be running during a simulation, but should hand back control to the simulator at
     * some point by suspending the process. This allows other processes to do work at the current point in time of the
     * simulation.
     * Suspending the process can be achieved by calling suspending method in the context:
     * 	- [Context.hold]	- Hold for `n` units of time before resuming execution.
     * 	- [Context.receive]	- Wait for a message to be received in the mailbox of the [Entity] before resuming
     * 	execution.
     *
     * If this method exits early, before the simulation has finished, the entity is assumed to be shutdown and its
     * simulation will not run any further.
     */
    suspend fun Context<S, M>.run()
}
