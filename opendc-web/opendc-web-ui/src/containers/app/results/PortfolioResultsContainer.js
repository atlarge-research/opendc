import React from 'react'
import { useSelector } from 'react-redux'
import PortfolioResultsComponent from '../../../components/app/results/PortfolioResultsComponent'
import { useRouter } from 'next/router'

const PortfolioResultsContainer = (props) => {
    const router = useRouter()
    const { portfolio: currentPortfolioId } = router.query
    const { scenarios, portfolio } = useSelector((state) => {
        if (
            !currentPortfolioId ||
            !state.objects.portfolio[currentPortfolioId] ||
            state.objects.portfolio[currentPortfolioId].scenarioIds
                .map((scenarioId) => state.objects.scenario[scenarioId])
                .some((s) => s === undefined)
        ) {
            return {
                portfolio: undefined,
                scenarios: [],
            }
        }

        return {
            portfolio: state.objects.portfolio[currentPortfolioId],
            scenarios: state.objects.portfolio[currentPortfolioId].scenarioIds.map(
                (scenarioId) => state.objects.scenario[scenarioId]
            ),
        }
    })

    return <PortfolioResultsComponent {...props} scenarios={scenarios} portfolio={portfolio} />
}

export default PortfolioResultsContainer
