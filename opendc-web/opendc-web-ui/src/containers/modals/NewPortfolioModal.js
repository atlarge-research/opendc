import { connect } from 'react-redux'
import NewPortfolioModalComponent from '../../components/modals/custom-components/NewPortfolioModalComponent'
import { addPortfolio } from '../../actions/portfolios'
import { closeNewPortfolioModal } from '../../actions/modals/portfolios'

const mapStateToProps = (state) => {
    return {
        show: state.modals.newPortfolioModalVisible,
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        callback: (name, targets) => {
            if (name) {
                dispatch(
                    addPortfolio({
                        name,
                        targets,
                    })
                )
            }
            dispatch(closeNewPortfolioModal())
        },
    }
}

const NewPortfolioModal = connect(mapStateToProps, mapDispatchToProps)(NewPortfolioModalComponent)

export default NewPortfolioModal
