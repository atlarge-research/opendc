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
import React from 'react'
import { Portfolio, Status } from '../../shapes'
import TableEmptyState from '../util/TableEmptyState'
import ScenarioState from './ScenarioState'
import { useDeleteScenario } from '../../data/project'

function ScenarioTable({ portfolio, status }) {
    const { mutate: deleteScenario } = useDeleteScenario()
    const projectId = portfolio?.project?.id
    const scenarios = portfolio?.scenarios ?? []

    const actions = ({ number }) => [
        {
            title: 'Delete Scenario',
            onClick: () => deleteScenario({ projectId: projectId, number }),
            isDisabled: number === 0,
        },
    ]

    return (
        <TableComposable aria-label="Scenario List" variant="compact">
            <Thead>
                <Tr>
                    <Th>Name</Th>
                    <Th>Topology</Th>
                    <Th>Trace</Th>
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
                                    scenario.topology.name
                                </Link>
                            ) : (
                                'Unknown Topology'
                            )}
                        </Td>
                        <Td dataLabel="Workload">{`${scenario.workload.trace.name} (${
                            scenario.workload.samplingFraction * 100
                        }%)`}</Td>
                        <Td dataLabel="State">
                            <ScenarioState state={scenario.job.state} />
                        </Td>
                        <Td isActionCell>
                            <ActionsColumn items={actions(scenario)} />
                        </Td>
                    </Tr>
                ))}
                {scenarios.length === 0 && (
                    <Tr>
                        <Td colSpan={4}>
                            <Bullseye>
                                <TableEmptyState
                                    status={status}
                                    loadingTitle="Loading Scenarios"
                                    emptyTitle="No scenarios"
                                    emptyText="You have not created any scenario for this portfolio yet. Click the New Scenario button to create one."
                                />
                            </Bullseye>
                        </Td>
                    </Tr>
                )}
            </Tbody>
        </TableComposable>
    )
}

ScenarioTable.propTypes = {
    portfolio: Portfolio,
    status: Status.isRequired,
}

export default ScenarioTable
