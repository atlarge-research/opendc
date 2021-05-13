import PropTypes from 'prop-types'
import React from 'react'
import { Portfolio } from '../../../../shapes'
import Link from 'next/link'
import FontAwesome from 'react-fontawesome'
import ScenarioListContainer from '../../../../containers/app/sidebars/project/ScenarioListContainer'
import { Button, Col, Row } from 'reactstrap'
import classNames from 'classnames'

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
                    <FontAwesome name="plus" />
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
                            <Link href={`/projects/${currentProjectId}/portfolios/${portfolio._id}`}>
                                <Button
                                    color="primary"
                                    outline
                                    className="mr-1 fa fa-play"
                                    onClick={() => onChoosePortfolio(portfolio._id)}
                                />
                            </Link>
                            <Button
                                color="danger"
                                outline
                                className="fa fa-trash"
                                onClick={() => onDeletePortfolio(portfolio._id)}
                            />
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
