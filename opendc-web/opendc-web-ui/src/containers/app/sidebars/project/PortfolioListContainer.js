import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import { useRouter } from 'next/router'
import PortfolioListComponent from '../../../../components/app/sidebars/project/PortfolioListComponent'
import { addPortfolio, deletePortfolio } from '../../../../redux/actions/portfolios'
import { getState } from '../../../../util/state-utils'
import { setCurrentTopology } from '../../../../redux/actions/topology/building'
import NewPortfolioModalComponent from '../../../../components/modals/custom-components/NewPortfolioModalComponent'
import { usePortfolios } from '../../../../data/project'

const PortfolioListContainer = () => {
    const router = useRouter()
    const { project: currentProjectId, portfolio: currentPortfolioId } = router.query
    const portfolios = usePortfolios(currentProjectId)

    const dispatch = useDispatch()
    const [isVisible, setVisible] = useState(false)
    const actions = {
        onNewPortfolio: () => setVisible(true),
        onChoosePortfolio: async (portfolioId) => {
            await router.push(`/projects/${currentProjectId}/portfolios/${portfolioId}`)
        },
        onDeletePortfolio: async (id) => {
            if (id) {
                const state = await getState(dispatch)
                dispatch(deletePortfolio(id))
                dispatch(setCurrentTopology(state.objects.project[currentProjectId].topologyIds[0]))
                await router.push(`/projects/${currentProjectId}`)
            }
        },
    }
    const callback = (name, targets) => {
        if (name) {
            dispatch(
                addPortfolio(currentProjectId, {
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
