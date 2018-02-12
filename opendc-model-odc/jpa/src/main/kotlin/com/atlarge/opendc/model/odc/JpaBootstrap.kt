package com.atlarge.opendc.model.odc

import com.atlarge.opendc.model.odc.integration.jpa.schema.Experiment
import com.atlarge.opendc.model.odc.integration.jpa.schema.Task
import com.atlarge.opendc.model.odc.topology.JpaTopologyFactory
import com.atlarge.opendc.model.topology.bootstrap
import com.atlarge.opendc.simulator.Bootstrap

/**
 * A [Bootstrap] procedure for experiments retrieved from a JPA data store.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class JpaBootstrap(val experiment: Experiment) : Bootstrap<JpaModel> {
    /**
     * Bootstrap a model `M` for a kernel in the given context.
     *
     * @param context The context to bootstrap to model in.
     * @return The initialised model for the simulation.
     */
    override fun bootstrap(context: Bootstrap.Context<JpaModel>): JpaModel {
        val section = experiment.path.sections.first()

        // TODO We should not modify parts of the experiment in a bootstrap as the bootstrap should be reproducible.
        // Important: initialise the scheduler of the datacenter
        section.datacenter.scheduler = experiment.scheduler

        val topology = JpaTopologyFactory(section)
            .create()
            .bootstrap()
            .bootstrap(context)
        val trace = experiment.trace
        val tasks = trace.jobs.flatMap { it.tasks }

        // Schedule all messages in the trace
        tasks.forEach { task ->
            if (task is Task) {
                context.schedule(task, section.datacenter, delay = task.startTime)
            }
        }

        return JpaModel(experiment, topology)
    }
}
