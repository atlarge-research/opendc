import { connect } from 'react-redux'
import { withRouter } from 'react-router-dom'
import PortfolioListComponent from '../../../../components/app/sidebars/project/PortfolioListComponent'
import { deletePortfolio, setCurrentPortfolio } from '../../../../actions/portfolios'
import { openNewPortfolioModal } from '../../../../actions/modals/portfolios'
import { getState } from '../../../../util/state-utils'
import { setCurrentTopology } from '../../../../actions/topology/building'

const mapStateToProps = (state) => {
    let portfolios = state.objects.project[state.currentProjectId]
        ? state.objects.project[state.currentProjectId].portfolioIds.map((t) => state.objects.portfolio[t])
        : []
    if (portfolios.filter((t) => !t).length > 0) {
        portfolios = []
    }

    return {
        currentProjectId: state.currentProjectId,
        currentPortfolioId: state.currentPortfolioId,
        portfolios,
    }
}

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onNewPortfolio: () => {
            dispatch(openNewPortfolioModal())
        },
        onChoosePortfolio: (portfolioId) => {
            dispatch(setCurrentPortfolio(portfolioId))
        },
        onDeletePortfolio: async (id) => {
            if (id) {
                const state = await getState(dispatch)
                dispatch(deletePortfolio(id))
                dispatch(setCurrentTopology(state.objects.project[state.currentProjectId].topologyIds[0]))
                ownProps.history.push(`/projects/${state.currentProjectId}`)
            }
        },
    }
}

const PortfolioListContainer = withRouter(connect(mapStateToProps, mapDispatchToProps)(PortfolioListComponent))

export default PortfolioListContainer
