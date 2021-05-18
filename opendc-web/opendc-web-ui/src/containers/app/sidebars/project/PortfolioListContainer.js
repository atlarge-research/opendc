import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import { useRouter } from 'next/router'
import PortfolioListComponent from '../../../../components/app/sidebars/project/PortfolioListComponent'
import { addPortfolio, deletePortfolio, setCurrentPortfolio } from '../../../../redux/actions/portfolios'
import { getState } from '../../../../util/state-utils'
import { setCurrentTopology } from '../../../../redux/actions/topology/building'
import NewPortfolioModalComponent from '../../../../components/modals/custom-components/NewPortfolioModalComponent'
import { useActivePortfolio, useActiveProject, usePortfolios } from '../../../../data/project'

const PortfolioListContainer = () => {
    const currentProjectId = useActiveProject()?._id
    const currentPortfolioId = useActivePortfolio()?._id
    const portfolios = usePortfolios(currentProjectId)

    const dispatch = useDispatch()
    const [isVisible, setVisible] = useState(false)
    const router = useRouter()
    const actions = {
        onNewPortfolio: () => setVisible(true),
        onChoosePortfolio: (portfolioId) => {
            dispatch(setCurrentPortfolio(portfolioId))
        },
        onDeletePortfolio: async (id) => {
            if (id) {
                const state = await getState(dispatch)
                dispatch(deletePortfolio(id))
                dispatch(setCurrentTopology(state.objects.project[state.currentProjectId].topologyIds[0]))
                router.push(`/projects/${state.currentProjectId}`)
            }
        },
    }
    const callback = (name, targets) => {
        if (name) {
            dispatch(
                addPortfolio({
                    name,
                    targets,
                })
            )
        }
        setVisible(false)
    }
    return (
        <>
            <PortfolioListComponent
                currentProjectId={currentProjectId}
                currentPortfolioId={currentPortfolioId}
                portfolios={portfolios}
                {...actions}
            />
            <NewPortfolioModalComponent callback={callback} show={isVisible} />
        </>
    )
}

export default PortfolioListContainer
