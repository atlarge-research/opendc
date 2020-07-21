import { connect } from 'react-redux'
import RackFillBar from '../../../components/app/map/elements/RackFillBar'

const mapStateToProps = (state, ownProps) => {
    const machineIds = state.objects.rack[state.objects.tile[ownProps.tileId].rackId].machineIds
    return {
        type: 'space',
        fillFraction: machineIds.filter((id) => id !== null).length / machineIds.length,
    }
}

const RackSpaceFillContainer = connect(mapStateToProps)(RackFillBar)

export default RackSpaceFillContainer
