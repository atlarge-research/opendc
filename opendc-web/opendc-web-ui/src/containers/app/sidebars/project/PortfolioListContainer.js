import React, { useState } from 'react'
import { useRouter } from 'next/router'
import PortfolioListComponent from '../../../../components/app/sidebars/project/PortfolioListComponent'
import NewPortfolioModalComponent from '../../../../components/modals/custom-components/NewPortfolioModalComponent'
import { useProjectPortfolios } from '../../../../data/project'
import { useMutation } from 'react-query'

const PortfolioListContainer = () => {
    const router = useRouter()
    const { project: currentProjectId, portfolio: currentPortfolioId } = router.query
    const portfolios = useProjectPortfolios(currentProjectId).data ?? []

    const { mutate: addPortfolio } = useMutation('addPortfolio')
    const { mutateAsync: deletePortfolio } = useMutation('deletePortfolio')

    const [isVisible, setVisible] = useState(false)
    const actions = {
        onNewPortfolio: () => setVisible(true),
        onChoosePortfolio: async (portfolioId) => {
            await router.push(`/projects/${currentProjectId}/portfolios/${portfolioId}`)
        },
        onDeletePortfolio: async (id) => {
            if (id) {
                await deletePortfolio(id)
                await router.push(`/projects/${currentProjectId}`)
            }
        },
    }
    const callback = (name, targets) => {
        if (name) {
            addPortfolio({ projectId: currentProjectId, name, targets })
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
