import PropTypes from 'prop-types'
import React from 'react'
import { Portfolio } from '../../../../shapes'
import Link from 'next/link'
import ScenarioListContainer from '../../../../containers/app/sidebars/project/ScenarioListContainer'
import { Button, Col, Row } from 'reactstrap'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus, faPlay, faTrash } from '@fortawesome/free-solid-svg-icons'

function PortfolioListComponent({
    portfolios,
    currentProjectId,
    currentPortfolioId,
    onNewPortfolio,
    onChoosePortfolio,
    onDeletePortfolio,
}) {
    return (
        <div className="pb-3">
            <h2>
                Portfolios
                <Button color="primary" outline className="float-right" onClick={(e) => onNewPortfolio(e)}>
                    <FontAwesomeIcon icon={faPlus} />
                </Button>
            </h2>

            {portfolios.map((portfolio, idx) => (
                <div key={portfolio._id}>
                    <Row className="row mb-1">
                        <Col
                            xs="7"
                            className={classNames('align-self-center', {
                                'font-weight-bold': portfolio._id === currentPortfolioId,
                            })}
                        >
                            {portfolio.name}
                        </Col>
                        <Col xs="5" className="text-right">
                            <Link passHref href={`/projects/${currentProjectId}/portfolios/${portfolio._id}`}>
                                <Button
                                    color="primary"
                                    outline
                                    className="mr-1"
                                    onClick={() => onChoosePortfolio(portfolio._id)}
                                >
                                    <FontAwesomeIcon icon={faPlay} />
                                </Button>
                            </Link>
                            <Button color="danger" outline onClick={() => onDeletePortfolio(portfolio._id)}>
                                <FontAwesomeIcon icon={faTrash} />
                            </Button>
                        </Col>
                    </Row>
                    <ScenarioListContainer portfolioId={portfolio._id} />
                </div>
            ))}
        </div>
    )
}

PortfolioListComponent.propTypes = {
    portfolios: PropTypes.arrayOf(Portfolio),
    currentProjectId: PropTypes.string.isRequired,
    currentPortfolioId: PropTypes.string,
    onNewPortfolio: PropTypes.func.isRequired,
    onChoosePortfolio: PropTypes.func.isRequired,
    onDeletePortfolio: PropTypes.func.isRequired,
}

export default PortfolioListComponent
