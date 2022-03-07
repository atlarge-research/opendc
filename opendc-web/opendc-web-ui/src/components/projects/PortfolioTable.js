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
import { usePortfolios, useDeletePortfolio } from '../../data/project'

const PortfolioTable = ({ projectId }) => {
    const { status, data: portfolios = [] } = usePortfolios(projectId)
    const { mutate: deletePortfolio } = useDeletePortfolio()

    const columns = ['Name', 'Scenarios', 'Metrics', 'Repeats']
    const rows =
        portfolios.length > 0
            ? portfolios.map((portfolio) => [
                  {
                      title: (
                          <Link href={`/projects/${projectId}/portfolios/${portfolio.number}`}>{portfolio.name}</Link>
                      ),
                  },
                  portfolio.scenarios.length === 1 ? '1 scenario' : `${portfolio.scenarios.length} scenarios`,
                  portfolio.targets.metrics.length === 1 ? '1 metric' : `${portfolio.targets.metrics.length} metrics`,
                  portfolio.targets.repeats === 1 ? '1 repeat' : `${portfolio.targets.repeats} repeats`,
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
                      onClick: (_, rowId) => deletePortfolio({ projectId, number: portfolios[rowId].number }),
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
    projectId: PropTypes.number,
}

export default PortfolioTable
