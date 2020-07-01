import { connect } from 'react-redux'
import { goDownOneInteractionLevel } from '../../../../../actions/interaction-level'
import BackToBuildingComponent from '../../../../../components/app/sidebars/topology/room/BackToBuildingComponent'

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(goDownOneInteractionLevel()),
    }
}

const BackToBuildingContainer = connect(undefined, mapDispatchToProps)(
    BackToBuildingComponent,
)

export default BackToBuildingContainer
