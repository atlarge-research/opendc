import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import { useRouter } from 'next/router'
import PortfolioListComponent from '../../../../components/app/sidebars/project/PortfolioListComponent'
import NewPortfolioModalComponent from '../../../../components/modals/custom-components/NewPortfolioModalComponent'
import { usePortfolios, useProject } from '../../../../data/project'
import { useMutation, useQueryClient } from 'react-query'
import { addPortfolio, deletePortfolio } from '../../../../api/portfolios'
import { useAuth } from '../../../../auth'

const PortfolioListContainer = () => {
    const router = useRouter()
    const { project: currentProjectId, portfolio: currentPortfolioId } = router.query
    const { data: currentProject } = useProject(currentProjectId)
    const portfolios = usePortfolios(currentProject?.portfolioIds ?? [])
        .filter((res) => res.data)
        .map((res) => res.data)

    const auth = useAuth()
    const queryClient = useQueryClient()
    const addMutation = useMutation((data) => addPortfolio(auth, data), {
        onSuccess: async (result) => {
            await queryClient.invalidateQueries(['projects', currentProjectId])
        },
    })
    const deleteMutation = useMutation((id) => deletePortfolio(auth, id), {
        onSuccess: async (result) => {
            queryClient.setQueryData(['projects', currentProjectId], (old) => ({
                ...old,
                portfolioIds: old.portfolioIds.filter((id) => id !== result._id),
            }))
            queryClient.removeQueries(['portfolios', result._id])
        },
    })

    const dispatch = useDispatch()
    const [isVisible, setVisible] = useState(false)
    const actions = {
        onNewPortfolio: () => setVisible(true),
        onChoosePortfolio: async (portfolioId) => {
            await router.push(`/projects/${currentProjectId}/portfolios/${portfolioId}`)
        },
        onDeletePortfolio: async (id) => {
            if (id) {
                await deleteMutation.mutateAsync(id)
                await router.push(`/projects/${currentProjectId}`)
            }
        },
    }
    const callback = (name, targets) => {
        if (name) {
            addMutation.mutate({ projectId: currentProjectId, name, targets })
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
