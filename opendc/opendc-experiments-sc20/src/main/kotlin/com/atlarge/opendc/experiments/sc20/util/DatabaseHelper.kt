/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.experiments.sc20.util

import com.atlarge.opendc.experiments.sc20.Portfolio
import com.atlarge.opendc.experiments.sc20.Run
import com.atlarge.opendc.experiments.sc20.Scenario
import java.io.Closeable
import java.sql.Connection
import java.sql.Statement

/**
 * A helper class for writing to the database.
 */
class DatabaseHelper(val conn: Connection) : Closeable {
    /**
     * Prepared statement to create experiment.
     */
    private val createExperiment = conn.prepareStatement("INSERT INTO experiments DEFAULT VALUES", arrayOf("id"))

    /**
     * Prepared statement for creating a portfolio.
     */
    private val createPortfolio = conn.prepareStatement("INSERT INTO portfolios (experiment_id, name) VALUES (?, ?)", arrayOf("id"))

    /**
     * Prepared statement for creating a scenario
     */
    private val createScenario = conn.prepareStatement("INSERT INTO scenarios (portfolio_id, repetitions, topology, workload_name, workload_fraction, allocation_policy, failure_frequency, interference) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", arrayOf("id"))

    /**
     * Prepared statement for creating a run.
     */
    private val createRun = conn.prepareStatement("INSERT INTO runs (id, scenario_id, seed) VALUES (?, ?, ?)", arrayOf("id"))

    /**
     * Prepared statement for starting a run.
     */
    private val startRun = conn.prepareStatement("UPDATE runs SET state = 'active'::run_state,start_time = now() WHERE id = ? AND scenario_id = ?")

    /**
     * Prepared statement for finishing a run.
     */
    private val finishRun = conn.prepareStatement("UPDATE runs SET state = ?::run_state,end_time = now() WHERE id = ? AND scenario_id = ?")

    /**
     * Create a new experiment and return its id.
     */
    fun createExperiment(): Long {
        val affectedRows = createExperiment.executeUpdate()
        check(affectedRows != 0)
        return createExperiment.latestId
    }

    /**
     * Persist a [Portfolio] and return its id.
     */
    fun persist(portfolio: Portfolio, experimentId: Long): Long {
        createPortfolio.setLong(1, experimentId)
        createPortfolio.setString(2, portfolio.name)

        val affectedRows = createPortfolio.executeUpdate()
        check(affectedRows != 0)
        return createPortfolio.latestId
    }

    /**
     * Persist a [Scenario] and return its id.
     */
    fun persist(scenario: Scenario, portfolioId: Long): Long {
        createScenario.setLong(1, portfolioId)
        createScenario.setInt(2, scenario.repetitions)
        createScenario.setString(3, scenario.topology.name)
        createScenario.setString(4, scenario.workload.name)
        createScenario.setDouble(5, scenario.workload.fraction)
        createScenario.setString(6, scenario.allocationPolicy)
        createScenario.setDouble(7, scenario.failureFrequency)
        createScenario.setBoolean(8, scenario.hasInterference)

        val affectedRows = createScenario.executeUpdate()
        check(affectedRows != 0)
        return createScenario.latestId
    }

    /**
     * Persist a [Run] and return its id.
     */
    fun persist(run: Run, scenarioId: Long): Int {
        createRun.setInt(1, run.id)
        createRun.setLong(2, scenarioId)
        createRun.setInt(3, run.seed)

        val affectedRows = createRun.executeUpdate()
        check(affectedRows != 0)
        return createRun.latestId.toInt()
    }

    /**
     * Start run.
     */
    fun startRun(scenario: Long, run: Int) {
        startRun.setInt(1, run)
        startRun.setLong(2, scenario)

        val affectedRows = startRun.executeUpdate()
        check(affectedRows != 0)
    }

    /**
     * Finish a run.
     */
    fun finishRun(scenario: Long, run: Int, hasFailed: Boolean) {
        finishRun.setString(1, if (hasFailed) "fail" else "ok")
        finishRun.setInt(2, run)
        finishRun.setLong(3, scenario)

        val affectedRows = finishRun.executeUpdate()
        check(affectedRows != 0)
    }

    /**
     * Obtain the latest identifier of the specified statement.
     */
    private val Statement.latestId: Long
        get() {
            val rs = generatedKeys
            return try {
                check(rs.next())
                rs.getLong(1)
            } finally {
                rs.close()
            }
        }

    override fun close() {
        conn.close()
    }
}
