import PropTypes from 'prop-types'
import React from 'react'
import { Scenario } from '../../../../shapes'
import Link from 'next/link'
import FontAwesome from 'react-fontawesome'

function ScenarioListComponent({
    scenarios,
    portfolioId,
    currentProjectId,
    currentScenarioId,
    onNewScenario,
    onChooseScenario,
    onDeleteScenario,
}) {
    return (
        <>
            {scenarios.map((scenario, idx) => (
                <div key={scenario._id} className="row mb-1">
                    <div
                        className={
                            'col-7 pl-5 align-self-center ' +
                            (scenario._id === currentScenarioId ? 'font-weight-bold' : '')
                        }
                    >
                        {scenario.name}
                    </div>
                    <div className="col-5 text-right">
                        <Link
                            href={`/projects/${currentProjectId}/portfolios/${scenario.portfolioId}/scenarios/${scenario._id}`}
                        >
                            <a
                                className="btn btn-outline-primary mr-1 fa fa-play disabled"
                                onClick={() => onChooseScenario(scenario.portfolioId, scenario._id)}
                            />
                        </Link>
                        <span
                            className={'btn btn-outline-danger fa fa-trash ' + (idx === 0 ? 'disabled' : '')}
                            onClick={() => (idx !== 0 ? onDeleteScenario(scenario._id) : undefined)}
                        />
                    </div>
                </div>
            ))}
            <div className="pl-4 mb-2">
                <div className="btn btn-outline-primary" onClick={() => onNewScenario(this.props.portfolioId)}>
                    <FontAwesome name="plus" className="mr-1" />
                    New scenario
                </div>
            </div>
        </>
    )
}

ScenarioListComponent.propTypes = {
    scenarios: PropTypes.arrayOf(Scenario),
    portfolioId: PropTypes.string,
    currentProjectId: PropTypes.string.isRequired,
    currentScenarioId: PropTypes.string,
    onNewScenario: PropTypes.func.isRequired,
    onChooseScenario: PropTypes.func.isRequired,
    onDeleteScenario: PropTypes.func.isRequired,
}

export default ScenarioListComponent
