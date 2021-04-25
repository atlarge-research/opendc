import { connect } from 'react-redux'
import RackGroup from '../../../components/app/map/groups/RackGroup'

const mapStateToProps = (state) => {
    return {
        interactionLevel: state.interactionLevel,
    }
}

const RackContainer = connect(mapStateToProps)(RackGroup)

export default RackContainer
