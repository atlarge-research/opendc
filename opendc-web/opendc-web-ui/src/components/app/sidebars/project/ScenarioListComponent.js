import PropTypes from 'prop-types'
import React from 'react'
import { Scenario } from '../../../../shapes'
import { Button, Col, Row } from 'reactstrap'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus, faTrash } from '@fortawesome/free-solid-svg-icons'

function ScenarioListComponent({ scenarios, portfolioId, onNewScenario, onDeleteScenario }) {
    return (
        <>
            {scenarios.map((scenario, idx) => (
                <Row key={scenario._id} className="mb-1">
                    <Col xs="7" className="pl-5 align-self-center">
                        {scenario.name}
                    </Col>
                    <Col xs="5" className="text-right">
                        <Button
                            color="danger"
                            outline
                            disabled={idx === 0}
                            onClick={() => (idx !== 0 ? onDeleteScenario(scenario._id) : undefined)}
                        >
                            <FontAwesomeIcon icon={faTrash} />
                        </Button>
                    </Col>
                </Row>
            ))}
            <div className="pl-4 mb-2">
                <Button color="primary" outline onClick={() => onNewScenario(portfolioId)}>
                    <FontAwesomeIcon icon={faPlus} className="mr-1" />
                    New scenario
                </Button>
            </div>
        </>
    )
}

ScenarioListComponent.propTypes = {
    scenarios: PropTypes.arrayOf(Scenario),
    portfolioId: PropTypes.string,
    onNewScenario: PropTypes.func.isRequired,
    onDeleteScenario: PropTypes.func.isRequired,
}

export default ScenarioListComponent
