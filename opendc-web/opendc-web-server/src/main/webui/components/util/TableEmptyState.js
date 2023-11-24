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
import { Bullseye, EmptyState, EmptyStateBody, EmptyStateIcon, Spinner, Title } from '@patternfly/react-core'
import { SearchIcon, CubesIcon } from '@patternfly/react-icons'
import { Status } from '../../shapes'

function TableEmptyState({
    status,
    isFiltering,
    loadingTitle = 'Loading',
    emptyTitle = 'No results found',
    emptyText = 'No results found of this type.',
    emptyAction = '',
}) {
    if (status === 'loading') {
        return (
            <Bullseye>
                <EmptyState variant="xs">
                    <EmptyStateIcon variant="container" component={Spinner} />
                    <Title headingLevel="h4" size="md">
                        {loadingTitle}
                    </Title>
                </EmptyState>
            </Bullseye>
        )
    } else if (status === 'error') {
        return (
            <EmptyState variant="xs">
                <Title headingLevel="h4" size="md">
                    Unable to connect
                </Title>
                <EmptyStateBody>
                    There was an error retrieving data. Check your connection and try again.
                </EmptyStateBody>
            </EmptyState>
        )
    } else if (status === 'idle') {
        return (
            <EmptyState variant="xs">
                <EmptyStateIcon icon={CubesIcon} />
                <Title headingLevel="h4" size="md">
                    {emptyTitle}
                </Title>
                <EmptyStateBody>No results available at this moment.</EmptyStateBody>
            </EmptyState>
        )
    } else if (isFiltering) {
        return (
            <EmptyState variant="xs">
                <EmptyStateIcon icon={SearchIcon} />
                <Title headingLevel="h4" size="md">
                    No results found
                </Title>
                <EmptyStateBody>
                    No results match this filter criteria. Remove all filters or clear all filters to show results.
                </EmptyStateBody>
            </EmptyState>
        )
    }

    return (
        <EmptyState variant="xs">
            <EmptyStateIcon icon={CubesIcon} />
            <Title headingLevel="h4" size="md">
                {emptyTitle}
            </Title>
            <EmptyStateBody>{emptyText}</EmptyStateBody>
            {emptyAction}
        </EmptyState>
    )
}

TableEmptyState.propTypes = {
    status: Status.isRequired,
    isFiltering: PropTypes.bool,
    loadingTitle: PropTypes.string,
    emptyTitle: PropTypes.string,
    emptyText: PropTypes.string,
    emptyAction: PropTypes.node,
}

export default TableEmptyState
