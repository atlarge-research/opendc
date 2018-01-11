package com.atlarge.opendc.model.odc

import com.atlarge.opendc.model.odc.integration.jpa.schema.Experiment
import com.atlarge.opendc.model.topology.MutableTopology

/**
 * Implementation of the [OdcModel] using a JPA backend.
 *
 * @property experiment The experiment that is simulated.
 * @property topology The topology the simulation runs on.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
data class JpaModel(val experiment: Experiment, val topology: MutableTopology): OdcModel, MutableTopology by topology

