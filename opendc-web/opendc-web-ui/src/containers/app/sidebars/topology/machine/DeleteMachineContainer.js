import { connect } from 'react-redux'
import { openDeleteMachineModal } from '../../../../../actions/modals/topology'
import DeleteMachineComponent from '../../../../../components/app/sidebars/topology/machine/DeleteMachineComponent'

const mapDispatchToProps = (dispatch) => {
    return {
        onClick: () => dispatch(openDeleteMachineModal()),
    }
}

const DeleteMachineContainer = connect(undefined, mapDispatchToProps)(DeleteMachineComponent)

export default DeleteMachineContainer
