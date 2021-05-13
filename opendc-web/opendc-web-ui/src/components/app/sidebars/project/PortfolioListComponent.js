import PropTypes from 'prop-types'
import React from 'react'
import { Portfolio } from '../../../../shapes'
import Link from 'next/link'
import FontAwesome from 'react-fontawesome'
import ScenarioListContainer from '../../../../containers/app/sidebars/project/ScenarioListContainer'

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
                <button className="btn btn-outline-primary float-right" onClick={(e) => onNewPortfolio(e)}>
                    <FontAwesome name="plus" />
                </button>
            </h2>

            {portfolios.map((portfolio, idx) => (
                <div key={portfolio._id}>
                    <div className="row mb-1">
                        <div
                            className={
                                'col-7 align-self-center ' +
                                (portfolio._id === currentPortfolioId ? 'font-weight-bold' : '')
                            }
                        >
                            {portfolio.name}
                        </div>
                        <div className="col-5 text-right">
                            <Link href={`/projects/${currentProjectId}/portfolios/${portfolio._id}`}>
                                <a
                                    className="btn btn-outline-primary mr-1 fa fa-play"
                                    onClick={() => onChoosePortfolio(portfolio._id)}
                                />
                            </Link>
                            <span
                                className="btn btn-outline-danger fa fa-trash"
                                onClick={() => onDeletePortfolio(portfolio._id)}
                            />
                        </div>
                    </div>
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
