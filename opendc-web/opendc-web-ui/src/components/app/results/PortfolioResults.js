import React from 'react'
import PropTypes from 'prop-types'
import { Bar, CartesianGrid, ComposedChart, ErrorBar, ResponsiveContainer, Scatter, XAxis, YAxis } from 'recharts'
import { AVAILABLE_METRICS, METRIC_NAMES, METRIC_NAMES_SHORT, METRIC_UNITS } from '../../../util/available-metrics'
import { mean, std } from 'mathjs'
import approx from 'approximate-number'
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
import { ErrorCircleOIcon, CubesIcon } from '@patternfly/react-icons'
import { usePortfolioScenarios } from '../../../data/project'
import NewScenario from '../../projects/NewScenario'
import PortfolioResultInfo from './PortfolioResultInfo'

const PortfolioResults = ({ portfolioId }) => {
    const { status, data: scenarios = [] } = usePortfolioScenarios(portfolioId)

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
                    <NewScenario portfolioId={portfolioId} />
                </EmptyState>
            </Bullseye>
        )
    }

    const dataPerMetric = {}

    AVAILABLE_METRICS.forEach((metric) => {
        dataPerMetric[metric] = scenarios
            .filter((scenario) => scenario.results)
            .map((scenario) => ({
                name: scenario.name,
                value: mean(scenario.results[metric]),
                errorX: std(scenario.results[metric]),
            }))
    })

    return (
        <Grid hasGutter>
            {AVAILABLE_METRICS.map((metric) => (
                <GridItem xl={6} lg={12} key={metric}>
                    <Card>
                        <CardHeader>
                            <CardActions>
                                <PortfolioResultInfo metric={metric} />
                            </CardActions>
                            <CardTitle>{METRIC_NAMES[metric]}</CardTitle>
                        </CardHeader>
                        <CardBody>
                            <ResponsiveContainer aspect={16 / 9} width="100%">
                                <ComposedChart
                                    data={dataPerMetric[metric]}
                                    margin={{ left: 35, bottom: 15 }}
                                    layout="vertical"
                                >
                                    <CartesianGrid strokeDasharray="3 3" />
                                    <XAxis
                                        tickFormatter={(tick) => approx(tick)}
                                        label={{ value: METRIC_UNITS[metric], position: 'bottom', offset: 0 }}
                                        type="number"
                                    />
                                    <YAxis dataKey="name" type="category" />
                                    <Bar dataKey="value" fill="#3399FF" isAnimationActive={false} />
                                    <Scatter dataKey="value" opacity={0} isAnimationActive={false}>
                                        <ErrorBar
                                            dataKey="errorX"
                                            width={10}
                                            strokeWidth={3}
                                            stroke="#FF6600"
                                            direction="x"
                                        />
                                    </Scatter>
                                </ComposedChart>
                            </ResponsiveContainer>
                        </CardBody>
                    </Card>
                </GridItem>
            ))}
        </Grid>
    )
}

PortfolioResults.propTypes = {
    portfolioId: PropTypes.string,
}

export default PortfolioResults
