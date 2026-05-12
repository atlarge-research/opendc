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
import PropTypes from 'prop-types'
import Link from 'next/link'
import { TableComposable, Thead, Tbody, Tr, Th, Td, ActionsColumn } from '@patternfly/react-table'
import React from 'react'
import TableEmptyState from '../util/TableEmptyState'
import { useExperiments, useDeleteExperiment } from '../../data/project'

function ExperimentTable({ projectId }) {
    const { status, data: experiments = [] } = useExperiments(projectId)
    const { mutate: deleteExperiment } = useDeleteExperiment()

    const actions = (experiment) => [
        {
            title: 'Delete Experiment',
            onClick: () => deleteExperiment({ projectId, number: experiment.number }),
        },
    ]

    return (
        <TableComposable aria-label="Experiment List" variant="compact">
            <Thead>
                <Tr>
                    <Th>Name</Th>
                    <Th>Scenarios</Th>
                    <Th>Metrics</Th>
                    <Th>Repeats</Th>
                </Tr>
            </Thead>
            <Tbody>
                {experiments.map((experiment) => (
                    <Tr key={experiment.id}>
                        <Td dataLabel="Name">
                            <Link href={`/projects/${projectId}/experiments/${experiment.number}`}>{experiment.name}</Link>
                        </Td>
                        <Td dataLabel="Scenarios">
                            {experiment.scenarios.length === 1
                                ? '1 scenario'
                                : `${experiment.scenarios.length} scenarios`}
                        </Td>
                        <Td dataLabel="Metrics">
                            {experiment.targets.metrics.length === 1
                                ? '1 metric'
                                : `${experiment.targets.metrics.length} metrics`}
                        </Td>
                        <Td dataLabel="Repeats">
                            {experiment.targets.repeats === 1 ? '1 repeat' : `${experiment.targets.repeats} repeats`}
                        </Td>
                        <Td isActionCell>
                            <ActionsColumn items={actions(experiment)} />
                        </Td>
                    </Tr>
                ))}
                {experiments.length === 0 && (
                    <Tr>
                        <Td colSpan={4}>
                            <Bullseye>
                                <TableEmptyState
                                    status={status}
                                    loadingTitle="Loading experiments"
                                    emptyTitle="No experiments"
                                    emptyText="You have not created any experiment for this project yet. Click the New Experiment button to create one."
                                />
                            </Bullseye>
                        </Td>
                    </Tr>
                )}
            </Tbody>
        </TableComposable>
    )
}

ExperimentTable.propTypes = {
    projectId: PropTypes.number,
}

export default ExperimentTable
