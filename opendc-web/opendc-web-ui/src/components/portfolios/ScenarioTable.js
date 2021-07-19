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
import ScenarioState from './ScenarioState'
import { usePortfolio, usePortfolioScenarios } from '../../data/project'
import { useProjectTopologies } from '../../data/topology'
import { useMutation } from 'react-query'

const ScenarioTable = ({ portfolioId }) => {
    const { data: portfolio } = usePortfolio(portfolioId)
    const { status, data: scenarios = [] } = usePortfolioScenarios(portfolioId)
    const { data: topologies } = useProjectTopologies(portfolio?.projectId, {
        select: (topologies) => new Map(topologies.map((topology) => [topology._id, topology])),
    })

    const { mutate: deleteScenario } = useMutation('deleteScenario')

    const columns = ['Name', 'Topology', 'Trace', 'State']
    const rows =
        scenarios.length > 0
            ? scenarios.map((scenario) => {
                  const topology = topologies?.get(scenario.topology.topologyId)

                  return [
                      scenario.name,
                      {
                          title: topology ? (
                              <Link href={`/projects/${topology.projectId}/topologies/${topology._id}`}>
                                  <a>{topology.name}</a>
                              </Link>
                          ) : (
                              'Unknown Topology'
                          ),
                      },
                      scenario.trace.traceId,
                      { title: <ScenarioState state={scenario.simulation.state} /> },
                  ]
              })
            : [
                  {
                      heightAuto: true,
                      cells: [
                          {
                              props: { colSpan: 4 },
                              title: (
                                  <TableEmptyState
                                      status={status}
                                      loadingTitle="Loading Scenarios"
                                      emptyTitle="No scenarios"
                                      emptyText="You have not created any scenario for this portfolio yet. Click the New Scenario button to create one."
                                  />
                              ),
                          },
                      ],
                  },
              ]

    const actionResolver = (_, { rowIndex }) => [
        {
            title: 'Delete Scenario',
            onClick: (_, rowId) => deleteScenario(scenarios[rowId]._id),
            isDisabled: rowIndex === 0,
        },
    ]

    return (
        <Table
            aria-label="Scenario List"
            variant="compact"
            cells={columns}
            rows={rows}
            actionResolver={scenarios.length > 0 ? actionResolver : undefined}
        >
            <TableHeader />
            <TableBody />
        </Table>
    )
}

ScenarioTable.propTypes = {
    portfolioId: PropTypes.string,
}

export default ScenarioTable
