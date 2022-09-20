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
import { Tr, Th, Thead, Td, ActionsColumn, Tbody, TableComposable } from '@patternfly/react-table'
import React from 'react'
import TableEmptyState from '../util/TableEmptyState'
import { parseAndFormatDateTime } from '../../util/date-time'
import { useTopologies, useDeleteTopology } from '../../data/topology'

function TopologyTable({ projectId }) {
    const { status, data: topologies = [] } = useTopologies(projectId)
    const { mutate: deleteTopology } = useDeleteTopology()

    const actions = ({ number }) => [
        {
            title: 'Delete Topology',
            onClick: () => deleteTopology({ projectId, number }),
            isDisabled: number === 0,
        },
    ]

    return (
        <TableComposable aria-label="Topology List" variant="compact">
            <Thead>
                <Tr>
                    <Th>Name</Th>
                    <Th>Rooms</Th>
                    <Th>Last Edited</Th>
                </Tr>
            </Thead>
            <Tbody>
                {topologies.map((topology) => (
                    <Tr key={topology.id}>
                        <Td dataLabel="Name">
                            <Link href={`/projects/${projectId}/topologies/${topology.number}`}>{topology.name}</Link>
                        </Td>
                        <Td dataLabel="Rooms">
                            {topology.rooms.length === 1 ? '1 room' : `${topology.rooms.length} rooms`}
                        </Td>
                        <Td dataLabel="Last Edited">{parseAndFormatDateTime(topology.updatedAt)}</Td>
                        <Td isActionCell>
                            <ActionsColumn items={actions(topology)} />
                        </Td>
                    </Tr>
                ))}
                {topologies.length === 0 && (
                    <Tr>
                        <Td colSpan={3}>
                            <Bullseye>
                                <TableEmptyState
                                    status={status}
                                    loadingTitle="Loading topologies"
                                    emptyTitle="No topologies"
                                    emptyText="You have not created any topology for this project yet. Click the New Topology button to create one."
                                />
                            </Bullseye>
                        </Td>
                    </Tr>
                )}
            </Tbody>
        </TableComposable>
    )
}

TopologyTable.propTypes = {
    projectId: PropTypes.number,
}

export default TopologyTable
