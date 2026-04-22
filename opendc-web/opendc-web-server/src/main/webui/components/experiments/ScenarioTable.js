/*
 * Copyright (c) 2021 AtLarge Research
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

import { Bullseye } from '@patternfly/react-core'
import Link from 'next/link'
import { TableComposable, Thead, Tr, Th, Tbody, Td, ActionsColumn } from '@patternfly/react-table'
import React, { useState } from 'react'
import { Experiment, Status } from '../../shapes'
import TableEmptyState from '../util/TableEmptyState'
import ScenarioState from './ScenarioState'
import { useDeleteScenario } from '../../data/project'
import JobReportModal from './JobReportModal'

function ScenarioTable({ experiment, status }) {
    const { mutate: deleteScenario } = useDeleteScenario()
    const projectId = experiment?.project?.id
    const scenarios = experiment?.scenarios ?? []
    const [reportJobId, setReportJobId] = useState(null)
    const [reportHasExports, setReportHasExports] = useState(false)

    const actions = (scenario) => {
        const latestJob = scenario.jobs[scenario.jobs.length - 1]
        const canViewReport = latestJob && (latestJob.state === 'FINISHED' || latestJob.state === 'FAILED')

        return [
            {
                title: 'View Report',
                onClick: () => {
                    setReportJobId(latestJob.id)
                    setReportHasExports(latestJob.hasExports === true)
                },
                isDisabled: !canViewReport,
            },
            {
                title: 'Delete Scenario',
                onClick: () => deleteScenario({ projectId: projectId, number: scenario.number }),
                isDisabled: scenario.number === 0,
            },
        ]
    }

    return (
        <TableComposable aria-label="Scenario List" variant="compact">
            <Thead>
                <Tr>
                    <Th>Name</Th>
                    <Th>Topology</Th>
                    <Th>Trace</Th>
                    <Th>Created</Th>
                    <Th>State</Th>
                </Tr>
            </Thead>
            <Tbody>
                {scenarios.map((scenario) => (
                    <Tr key={scenario.id}>
                        <Td dataLabel="Name">{scenario.name}</Td>
                        <Td dataLabel="Topology">
                            {scenario.topology ? (
                                <Link href={`/projects/${projectId}/topologies/${scenario.topology.number}`}>
                                    {scenario.topology.name}
                                </Link>
                            ) : (
                                'Unknown Topology'
                            )}
                        </Td>
                        <Td dataLabel="Workload">{`${scenario.workload.trace.name} (${
                            scenario.workload.samplingFraction * 100
                        }%)`}</Td>
                        <Td dataLabel="Created">
                            {scenario.jobs && scenario.jobs.length > 0
                                ? new Date(scenario.jobs[0].createdAt).toLocaleString()
                                : '-'}
                        </Td>
                        <Td dataLabel="State">
                            <ScenarioState state={scenario.jobs[scenario.jobs.length - 1].state} />
                        </Td>
                        <Td isActionCell>
                            <ActionsColumn items={actions(scenario)} />
                        </Td>
                    </Tr>
                ))}
                {scenarios.length === 0 && (
                    <Tr>
                        <Td colSpan={5}>
                            <Bullseye>
                                <TableEmptyState
                                    status={status}
                                    loadingTitle="Loading Scenarios"
                                    emptyTitle="No scenarios"
                                    emptyText="You have not created any scenario for this experiment yet. Click the New Scenario button to create one."
                                />
                            </Bullseye>
                        </Td>
                    </Tr>
                )}
            </Tbody>
            {reportJobId && (
                <JobReportModal
                    jobId={reportJobId}
                    isOpen={true}
                    onClose={() => {
                        setReportJobId(null)
                        setReportHasExports(false)
                    }}
                    hasExports={reportHasExports}
                />
            )}
        </TableComposable>
    )
}

ScenarioTable.propTypes = {
    experiment: Experiment,
    status: Status.isRequired,
}

export default ScenarioTable
