import React from 'react'
import { useSelector } from 'react-redux'
import PortfolioResultsComponent from '../../../components/app/results/PortfolioResultsComponent'

const PortfolioResultsContainer = (props) => {
    const { scenarios, portfolio } = useSelector((state) => {
        if (
            state.currentPortfolioId === '-1' ||
            !state.objects.portfolio[state.currentPortfolioId] ||
            state.objects.portfolio[state.currentPortfolioId].scenarioIds
                .map((scenarioId) => state.objects.scenario[scenarioId])
                .some((s) => s === undefined)
        ) {
            return {
                portfolio: undefined,
                scenarios: [],
            }
        }

        return {
            portfolio: state.objects.portfolio[state.currentPortfolioId],
            scenarios: state.objects.portfolio[state.currentPortfolioId].scenarioIds.map(
                (scenarioId) => state.objects.scenario[scenarioId]
            ),
        }
    })

    return <PortfolioResultsComponent {...props} scenarios={scenarios} portfolio={portfolio} />
}

export default PortfolioResultsContainer
