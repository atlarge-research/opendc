import PropTypes from 'prop-types'
import React from 'react'
import { Scenario } from '../../../../shapes'
import Link from 'next/link'
import FontAwesome from 'react-fontawesome'
import { Button, Col, Row } from 'reactstrap'
import classNames from 'classnames'

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
                <Row key={scenario._id} className="mb-1">
                    <Col
                        xs="7"
                        className={classNames('pl-5 align-self-center', {
                            'font-weight-bold': scenario._id === currentScenarioId,
                        })}
                    >
                        {scenario.name}
                    </Col>
                    <Col xs="5" className="text-right">
                        <Link
                            href={`/projects/${currentProjectId}/portfolios/${scenario.portfolioId}/scenarios/${scenario._id}`}
                        >
                            <Button
                                color="primary"
                                outline
                                disabled
                                className="mr-1 fa fa-play"
                                onClick={() => onChooseScenario(scenario.portfolioId, scenario._id)}
                            />
                        </Link>
                        <Button
                            color="danger"
                            outline
                            className="fa fa-trash"
                            disabled={idx === 0}
                            onClick={() => (idx !== 0 ? onDeleteScenario(scenario._id) : undefined)}
                        />
                    </Col>
                </Row>
            ))}
            <div className="pl-4 mb-2">
                <Button color="primary" outline onClick={() => onNewScenario(portfolioId)}>
                    <FontAwesome name="plus" className="mr-1" />
                    New scenario
                </Button>
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
