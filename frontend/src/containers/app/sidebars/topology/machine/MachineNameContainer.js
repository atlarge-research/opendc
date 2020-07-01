import { connect } from 'react-redux'
import MachineNameComponent from '../../../../../components/app/sidebars/topology/machine/MachineNameComponent'

const mapStateToProps = state => {
    return {
        position: state.interactionLevel.position,
    }
}

const MachineNameContainer = connect(mapStateToProps)(MachineNameComponent)

export default MachineNameContainer
