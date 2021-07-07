import React from 'react'
import PortfolioResultsComponent from '../../../components/app/results/PortfolioResultsComponent'
import { useRouter } from 'next/router'
import { usePortfolio, useScenarios } from '../../../data/project'

const PortfolioResultsContainer = (props) => {
    const router = useRouter()
    const { portfolio: currentPortfolioId } = router.query
    const { data: portfolio } = usePortfolio(currentPortfolioId)
    const scenarios = useScenarios(portfolio?.scenarioIds ?? [])
        .filter((res) => res.data)
        .map((res) => res.data)

    return <PortfolioResultsComponent {...props} scenarios={scenarios} portfolio={portfolio} />
}

export default PortfolioResultsContainer
