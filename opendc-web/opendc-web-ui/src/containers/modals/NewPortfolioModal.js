import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { closeNewPortfolioModal } from '../../actions/modals/portfolios'
import NewPortfolioModalComponent from '../../components/modals/custom-components/NewPortfolioModalComponent'
import { addPortfolio } from '../../actions/portfolios'

const NewPortfolioModal = (props) => {
    const show = useSelector((state) => state.modals.newPortfolioModalVisible)
    const dispatch = useDispatch()
    const callback = (name, targets) => {
        if (name) {
            dispatch(
                addPortfolio({
                    name,
                    targets,
                })
            )
        }
        dispatch(closeNewPortfolioModal())
    }
    return <NewPortfolioModalComponent {...props} callback={callback} show={show} />
}

export default NewPortfolioModal
