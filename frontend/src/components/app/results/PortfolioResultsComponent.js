import React from 'react'
import PropTypes from 'prop-types'
import { Bar, CartesianGrid, ComposedChart, ErrorBar, ResponsiveContainer, Scatter, XAxis, YAxis } from 'recharts'
import { AVAILABLE_METRICS, METRIC_NAMES, METRIC_UNITS } from '../../../util/available-metrics'
import { mean, std } from 'mathjs'
import Shapes from '../../../shapes/index'
import approx from 'approximate-number'

const PortfolioResultsComponent = ({ portfolio, scenarios }) => {
    if (!portfolio) {
        return <div>Loading...</div>
    }

    const nonFinishedScenarios = scenarios.filter((s) => s.simulation.state !== 'FINISHED')

    if (nonFinishedScenarios.length > 0) {
        if (nonFinishedScenarios.every((s) => s.simulation.state === 'QUEUED' || s.simulation.state === 'RUNNING')) {
            return (
                <div>
                    <h1>Simulation running...</h1>
                    <p>{nonFinishedScenarios.length} of the scenarios are still being simulated</p>
                </div>
            )
        }
        if (nonFinishedScenarios.some((s) => s.simulation.state === 'FAILED')) {
            return (
                <div>
                    <h1>Simulation failed.</h1>
                    <p>
                        Try again by creating a new scenario. Please contact the OpenDC team for support, if issues
                        persist.
                    </p>
                </div>
            )
        }
    }

    const dataPerMetric = {}

    AVAILABLE_METRICS.forEach((metric) => {
        dataPerMetric[metric] = scenarios.map((scenario) => ({
            name: scenario.name,
            value: mean(scenario.results[metric]),
            errorX: std(scenario.results[metric]),
        }))
    })

    return (
        <div className="full-height" style={{ overflowY: 'scroll', overflowX: 'hidden' }}>
            <h2>Portfolio: {portfolio.name}</h2>
            <p>Repeats per Scenario: {portfolio.targets.repeatsPerScenario}</p>
            <div className="row">
                {AVAILABLE_METRICS.map((metric) => (
                    <div className="col-6 mb-2" key={metric}>
                        <h4>{METRIC_NAMES[metric]}</h4>
                        <ResponsiveContainer aspect={16 / 9} width="100%">
                            <ComposedChart data={dataPerMetric[metric]} margin={{ left: 20, bottom: 15 }} layout="vertical">
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
                    </div>
                ))}
            </div>
        </div>
    )
}

PortfolioResultsComponent.propTypes = {
    portfolio: Shapes.Portfolio,
    scenarios: PropTypes.arrayOf(Shapes.Scenario),
}

export default PortfolioResultsComponent
