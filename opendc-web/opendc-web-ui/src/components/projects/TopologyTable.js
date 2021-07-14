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

import PropTypes from 'prop-types'
import Link from 'next/link'
import { Table, TableBody, TableHeader } from '@patternfly/react-table'
import React from 'react'
import TableEmptyState from '../util/TableEmptyState'
import { parseAndFormatDateTime } from '../../util/date-time'
import { useMutation } from 'react-query'
import { useProjectTopologies } from '../../data/topology'

const TopologyTable = ({ projectId }) => {
    const { status, data: topologies = [] } = useProjectTopologies(projectId)
    const { mutate: deleteTopology } = useMutation('deleteTopology')

    const columns = ['Name', 'Rooms', 'Last Edited']
    const rows =
        topologies.length > 0
            ? topologies.map((topology) => [
                  {
                      title: (
                          <Link href={`/projects/${topology.projectId}/topologies/${topology._id}`}>
                              {topology.name}
                          </Link>
                      ),
                  },
                  topology.rooms.length === 1 ? '1 room' : `${topology.rooms.length} rooms`,
                  parseAndFormatDateTime(topology.datetimeLastEdited),
              ])
            : [
                  {
                      heightAuto: true,
                      cells: [
                          {
                              props: { colSpan: 3 },
                              title: (
                                  <TableEmptyState
                                      status={status}
                                      loadingTitle="Loading topologies"
                                      emptyTitle="No topologies"
                                      emptyText="You have not created any topology for this project yet. Click the New Topology button to create one."
                                  />
                              ),
                          },
                      ],
                  },
              ]

    const actionResolver = (_, { rowIndex }) => [
        {
            title: 'Delete Topology',
            onClick: (_, rowId) => deleteTopology(topologies[rowId]._id),
            isDisabled: rowIndex === 0,
        },
    ]

    return (
        <Table
            aria-label="Topology List"
            variant="compact"
            cells={columns}
            rows={rows}
            actionResolver={topologies.length > 0 ? actionResolver : () => []}
        >
            <TableHeader />
            <TableBody />
        </Table>
    )
}

TopologyTable.propTypes = {
    projectId: PropTypes.string,
}

export default TopologyTable
