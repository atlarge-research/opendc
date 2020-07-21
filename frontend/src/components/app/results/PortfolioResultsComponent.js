import React from 'react'
import PropTypes from 'prop-types'
import { Bar, BarChart, CartesianGrid, ErrorBar, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { AVAILABLE_METRICS, METRIC_NAMES } from '../../../util/available-metrics'
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
            std: std(scenario.results[metric]),
        }))
    })

    return (
        <div>
            <h1>Portfolio: {portfolio.name}</h1>
            <p>Repeats per Scenario: {portfolio.targets.repeatsPerScenario}</p>
            <div className="row">
                {AVAILABLE_METRICS.map(metric => (
                    <div className="col-6" key={metric}>
                        <h4>{METRIC_NAMES[metric]}</h4>
                        <ResponsiveContainer aspect={16 / 9} width="100%">
                            <BarChart data={dataPerMetric[metric]}>
                                <CartesianGrid strokeDasharray="3 3"/>
                                <XAxis dataKey="name"/>
                                <YAxis tickFormatter={tick => approx(tick)}/>
                                <Tooltip/>
                                <Bar dataKey="value" fill="#8884d8"/>
                                <ErrorBar dataKey="std" width={4} strokeWidth={2} stroke="blue" direction="y"/>
                            </BarChart>
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
