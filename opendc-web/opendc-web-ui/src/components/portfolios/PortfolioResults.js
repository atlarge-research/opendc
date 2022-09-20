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

import { mean, std } from 'mathjs'
import React, { useMemo } from 'react'
import PropTypes from 'prop-types'
import { VictoryErrorBar } from 'victory-errorbar'
import { METRIC_NAMES, METRIC_UNITS, AVAILABLE_METRICS } from '../../util/available-metrics'
import {
    Bullseye,
    Card,
    CardActions,
    CardBody,
    CardHeader,
    CardTitle,
    EmptyState,
    EmptyStateBody,
    EmptyStateIcon,
    Grid,
    GridItem,
    Spinner,
    Title,
} from '@patternfly/react-core'
import { Chart, ChartAxis, ChartBar, ChartTooltip } from '@patternfly/react-charts'
import { ErrorCircleOIcon, CubesIcon } from '@patternfly/react-icons'
import { usePortfolio } from '../../data/project'
import PortfolioResultInfo from './PortfolioResultInfo'
import NewScenario from './NewScenario'

function PortfolioResults({ projectId, portfolioId }) {
    const { status, data: portfolio } = usePortfolio(projectId, portfolioId)
    const scenarios = useMemo(() => portfolio?.scenarios ?? [], [portfolio])

    const label = ({ datum }) =>
        `${datum.x}: ${datum.y.toLocaleString()} ± ${datum.errorY.toLocaleString()} ${METRIC_UNITS[datum.metric]}`
    const selectedMetrics = new Set(portfolio?.targets?.metrics ?? [])
    const dataPerMetric = useMemo(() => {
        const dataPerMetric = {}
        AVAILABLE_METRICS.forEach((metric) => {
            dataPerMetric[metric] = scenarios
                .filter((scenario) => scenario.job?.results)
                .map((scenario) => ({
                    metric,
                    x: scenario.name,
                    y: mean(scenario.job.results[metric]),
                    errorY: std(scenario.job.results[metric]),
                    label,
                }))
        })
        return dataPerMetric
    }, [scenarios])

    const categories = useMemo(() => ({ x: scenarios.map((s) => s.name).reverse() }), [scenarios])

    if (status === 'loading') {
        return (
            <Bullseye>
                <EmptyState>
                    <EmptyStateIcon variant="container" component={Spinner} />
                    <Title size="lg" headingLevel="h4">
                        Loading Results
                    </Title>
                </EmptyState>
            </Bullseye>
        )
    } else if (status === 'error') {
        return (
            <Bullseye>
                <EmptyState>
                    <EmptyStateIcon variant="container" component={ErrorCircleOIcon} />
                    <Title size="lg" headingLevel="h4">
                        Unable to connect
                    </Title>
                    <EmptyStateBody>
                        There was an error retrieving data. Check your connection and try again.
                    </EmptyStateBody>
                </EmptyState>
            </Bullseye>
        )
    } else if (scenarios.length === 0) {
        return (
            <Bullseye>
                <EmptyState>
                    <EmptyStateIcon variant="container" component={CubesIcon} />
                    <Title size="lg" headingLevel="h4">
                        No results
                    </Title>
                    <EmptyStateBody>
                        No results are currently available for this portfolio. Run a scenario to obtain simulation
                        results.
                    </EmptyStateBody>
                    <NewScenario projectId={projectId} portfolioId={portfolioId} />
                </EmptyState>
            </Bullseye>
        )
    }

    return (
        <Grid hasGutter>
            {AVAILABLE_METRICS.map(
                (metric) =>
                    selectedMetrics.has(metric) && (
                        <GridItem xl={6} lg={12} key={metric}>
                            <Card>
                                <CardHeader>
                                    <CardActions>
                                        <PortfolioResultInfo metric={metric} />
                                    </CardActions>
                                    <CardTitle>{METRIC_NAMES[metric]}</CardTitle>
                                </CardHeader>
                                <CardBody>
                                    <Chart
                                        width={650}
                                        height={250}
                                        padding={{
                                            top: 10,
                                            bottom: 60,
                                            left: 130,
                                        }}
                                        domainPadding={25}
                                    >
                                        <ChartAxis />
                                        <ChartAxis
                                            dependentAxis
                                            showGrid
                                            label={METRIC_UNITS[metric]}
                                            fixLabelOverlap
                                        />
                                        <ChartBar
                                            categories={categories}
                                            data={dataPerMetric[metric]}
                                            labelComponent={<ChartTooltip constrainToVisibleArea />}
                                            barWidth={25}
                                            horizontal
                                        />
                                        <VictoryErrorBar
                                            categories={categories}
                                            data={dataPerMetric[metric]}
                                            errorY={(d) => d.errorY}
                                            labelComponent={<></>}
                                            horizontal
                                        />
                                    </Chart>
                                </CardBody>
                            </Card>
                        </GridItem>
                    )
            )}
        </Grid>
    )
}

PortfolioResults.propTypes = {
    projectId: PropTypes.number,
    portfolioId: PropTypes.number,
}

export default PortfolioResults
