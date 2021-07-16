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
import { useProjectPortfolios } from '../../data/project'
import { useMutation } from 'react-query'

const PortfolioTable = ({ projectId }) => {
    const { status, data: portfolios = [] } = useProjectPortfolios(projectId)
    const { mutate: deletePortfolio } = useMutation('deletePortfolio')

    const columns = ['Name', 'Scenarios', 'Metrics', 'Repeats']
    const rows =
        portfolios.length > 0
            ? portfolios.map((portfolio) => [
                  {
                      title: (
                          <Link href={`/projects/${portfolio.projectId}/portfolios/${portfolio._id}`}>
                              {portfolio.name}
                          </Link>
                      ),
                  },

                  portfolio.scenarioIds.length === 1 ? '1 scenario' : `${portfolio.scenarioIds.length} scenarios`,

                  portfolio.targets.enabledMetrics.length === 1
                      ? '1 metric'
                      : `${portfolio.targets.enabledMetrics.length} metrics`,
                  portfolio.targets.repeatsPerScenario === 1
                      ? '1 repeat'
                      : `${portfolio.targets.repeatsPerScenario} repeats`,
              ])
            : [
                  {
                      heightAuto: true,
                      cells: [
                          {
                              props: { colSpan: 4 },
                              title: (
                                  <TableEmptyState
                                      status={status}
                                      loadingTitle="Loading portfolios"
                                      emptyTitle="No portfolios"
                                      emptyText="You have not created any portfolio for this project yet. Click the New Portfolio button to create one."
                                  />
                              ),
                          },
                      ],
                  },
              ]

    const actions =
        portfolios.length > 0
            ? [
                  {
                      title: 'Delete Portfolio',
                      onClick: (_, rowId) => deletePortfolio(portfolios[rowId]._id),
                  },
              ]
            : []

    return (
        <Table aria-label="Portfolio List" variant="compact" cells={columns} rows={rows} actions={actions}>
            <TableHeader />
            <TableBody />
        </Table>
    )
}

PortfolioTable.propTypes = {
    projectId: PropTypes.string,
}

export default PortfolioTable
