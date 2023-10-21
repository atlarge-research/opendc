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
import { usePortfolios, useDeletePortfolio } from '../../data/project'

function PortfolioTable({ projectId }) {
    const { status, data: portfolios = [] } = usePortfolios(projectId)
    const { mutate: deletePortfolio } = useDeletePortfolio()

    const actions = (portfolio) => [
        {
            title: 'Delete Portfolio',
            onClick: () => deletePortfolio({ projectId, number: portfolio.number }),
        },
    ]

    return (
        <TableComposable aria-label="Portfolio List" variant="compact">
            <Thead>
                <Tr>
                    <Th>Name</Th>
                    <Th>Scenarios</Th>
                    <Th>Metrics</Th>
                    <Th>Repeats</Th>
                </Tr>
            </Thead>
            <Tbody>
                {portfolios.map((portfolio) => (
                    <Tr key={portfolio.id}>
                        <Td dataLabel="Name">
                            <Link href={`/projects/${projectId}/portfolios/${portfolio.number}`}>{portfolio.name}</Link>
                        </Td>
                        <Td dataLabel="Scenarios">
                            {portfolio.scenarios.length === 1
                                ? '1 scenario'
                                : `${portfolio.scenarios.length} scenarios`}
                        </Td>
                        <Td dataLabel="Metrics">
                            {portfolio.targets.metrics.length === 1
                                ? '1 metric'
                                : `${portfolio.targets.metrics.length} metrics`}
                        </Td>
                        <Td dataLabel="Repeats">
                            {portfolio.targets.repeats === 1 ? '1 repeat' : `${portfolio.targets.repeats} repeats`}
                        </Td>
                        <Td isActionCell>
                            <ActionsColumn items={actions(portfolio)} />
                        </Td>
                    </Tr>
                ))}
                {portfolios.length === 0 && (
                    <Tr>
                        <Td colSpan={4}>
                            <Bullseye>
                                <TableEmptyState
                                    status={status}
                                    loadingTitle="Loading portfolios"
                                    emptyTitle="No portfolios"
                                    emptyText="You have not created any portfolio for this project yet. Click the New Portfolio button to create one."
                                />
                            </Bullseye>
                        </Td>
                    </Tr>
                )}
            </Tbody>
        </TableComposable>
    )
}

PortfolioTable.propTypes = {
    projectId: PropTypes.number,
}

export default PortfolioTable
