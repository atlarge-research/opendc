import { connect } from 'react-redux'
import { openEditRackNameModal } from '../../../../../actions/modals/topology'
import RackNameComponent from '../../../../../components/app/sidebars/topology/rack/RackNameComponent'

const mapStateToProps = state => {
    return {
        rackName:
        state.objects.rack[
            state.objects.tile[state.interactionLevel.tileId].objectId
            ].name,
    }
}

const mapDispatchToProps = dispatch => {
    return {
        onEdit: () => dispatch(openEditRackNameModal()),
    }
}

const RackNameContainer = connect(mapStateToProps, mapDispatchToProps)(
    RackNameComponent,
)

export default RackNameContainer
