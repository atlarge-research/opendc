import React from 'react'
import PortfolioResultsComponent from '../../../components/app/results/PortfolioResultsComponent'
import { useRouter } from 'next/router'
import { usePortfolio, usePortfolioScenarios } from '../../../data/project'

const PortfolioResultsContainer = (props) => {
    const router = useRouter()
    const { portfolio: currentPortfolioId } = router.query
    const { data: portfolio } = usePortfolio(currentPortfolioId)
    const scenarios = usePortfolioScenarios(currentPortfolioId).data ?? []
    return <PortfolioResultsComponent {...props} scenarios={scenarios} portfolio={portfolio} />
}

export default PortfolioResultsContainer
