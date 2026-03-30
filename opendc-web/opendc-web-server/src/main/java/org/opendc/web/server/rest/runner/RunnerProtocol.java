/*
 * Copyright (c) 2023 AtLarge Research
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

package org.opendc.web.server.rest.runner;

import org.opendc.web.server.model.Job;
import org.opendc.web.server.model.Portfolio;
import org.opendc.web.server.model.Scenario;
import org.opendc.web.server.model.Topology;
import org.opendc.web.server.rest.BaseProtocol;

/**
 * DTO-conversions for the runner protocol.
 */
public final class RunnerProtocol {
    /**
     * Private constructor to prevent instantiation of class.
     */
    private RunnerProtocol() {}

    /**
     * Convert a {@link Job} into a runner-facing DTO.
     */
    public static org.opendc.web.proto.runner.Job toDto(Job job) {
        return new org.opendc.web.proto.runner.Job(
                job.id, toDto(job.scenario), job.state, job.createdAt, job.updatedAt, job.runtime, job.results);
    }

    /**
     * Convert a {@link Scenario} into a runner-facing DTO.
     */
    public static org.opendc.web.proto.runner.Scenario toDto(Scenario scenario) {
        return new org.opendc.web.proto.runner.Scenario(
                scenario.id,
                scenario.number,
                toDto(scenario.portfolio),
                scenario.name,
                BaseProtocol.toDto(scenario.workload),
                toDto(scenario.topology),
                scenario.phenomena,
                scenario.schedulerName);
    }

    /**
     * Convert a {@link Portfolio} into a runner-facing DTO.
     */
    public static org.opendc.web.proto.runner.Portfolio toDto(Portfolio portfolio) {
        return new org.opendc.web.proto.runner.Portfolio(
                portfolio.id, portfolio.number, portfolio.name, portfolio.targets);
    }

    /**
     * Convert a {@link Topology} into a runner-facing DTO.
     */
    public static org.opendc.web.proto.runner.Topology toDto(Topology topology) {
        return new org.opendc.web.proto.runner.Topology(
                topology.id, topology.number, topology.name, topology.rooms, topology.createdAt, topology.updatedAt);
    }
}
