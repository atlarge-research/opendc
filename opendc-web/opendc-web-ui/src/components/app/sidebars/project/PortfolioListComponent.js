import PropTypes from 'prop-types'
import React from 'react'
import Shapes from '../../../../shapes'
import Link from 'next/link'
import FontAwesome from 'react-fontawesome'
import ScenarioListContainer from '../../../../containers/app/sidebars/project/ScenarioListContainer'

class PortfolioListComponent extends React.Component {
    static propTypes = {
        portfolios: PropTypes.arrayOf(Shapes.Portfolio),
        currentProjectId: PropTypes.string.isRequired,
        currentPortfolioId: PropTypes.string,
        onNewPortfolio: PropTypes.func.isRequired,
        onChoosePortfolio: PropTypes.func.isRequired,
        onDeletePortfolio: PropTypes.func.isRequired,
    }

    onDelete(id) {
        this.props.onDeletePortfolio(id)
    }

    render() {
        return (
            <div className="pb-3">
                <h2>
                    Portfolios
                    <button
                        className="btn btn-outline-primary float-right"
                        onClick={this.props.onNewPortfolio.bind(this)}
                    >
                        <FontAwesome name="plus" />
                    </button>
                </h2>

                {this.props.portfolios.map((portfolio, idx) => (
                    <div key={portfolio._id}>
                        <div className="row mb-1">
                            <div
                                className={
                                    'col-7 align-self-center ' +
                                    (portfolio._id === this.props.currentPortfolioId ? 'font-weight-bold' : '')
                                }
                            >
                                {portfolio.name}
                            </div>
                            <div className="col-5 text-right">
                                <Link href={`/projects/${this.props.currentProjectId}/portfolios/${portfolio._id}`}>
                                    <a
                                        className="btn btn-outline-primary mr-1 fa fa-play"
                                        onClick={() => this.props.onChoosePortfolio(portfolio._id)}
                                    />
                                </Link>
                                <span
                                    className="btn btn-outline-danger fa fa-trash"
                                    onClick={() => this.onDelete(portfolio._id)}
                                />
                            </div>
                        </div>
                        <ScenarioListContainer portfolioId={portfolio._id} />
                    </div>
                ))}
            </div>
        )
    }
}

export default PortfolioListComponent
