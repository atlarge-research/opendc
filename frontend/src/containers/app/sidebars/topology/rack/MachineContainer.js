import { connect } from 'react-redux'
import { goFromRackToMachine } from '../../../../../actions/interaction-level'
import MachineComponent from '../../../../../components/app/sidebars/topology/rack/MachineComponent'

const mapStateToProps = (state, ownProps) => {
    return {
        machine: state.objects.machine[ownProps.machineId],
    }
}

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onClick: () => dispatch(goFromRackToMachine(ownProps.position)),
    }
}

const MachineContainer = connect(mapStateToProps, mapDispatchToProps)(MachineComponent)

export default MachineContainer
