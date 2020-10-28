import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useHistory } from 'react-router-dom'
import PortfolioListComponent from '../../../../components/app/sidebars/project/PortfolioListComponent'
import { deletePortfolio, setCurrentPortfolio } from '../../../../actions/portfolios'
import { openNewPortfolioModal } from '../../../../actions/modals/portfolios'
import { getState } from '../../../../util/state-utils'
import { setCurrentTopology } from '../../../../actions/topology/building'

const PortfolioListContainer = (props) => {
    const state = useSelector((state) => {
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
    })

    const dispatch = useDispatch()
    const history = useHistory()
    const actions = {
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
                history.push(`/projects/${state.currentProjectId}`)
            }
        },
    }
    return <PortfolioListComponent {...props} {...state} {...actions} />
}

export default PortfolioListContainer
