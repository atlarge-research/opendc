import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import ConfirmationModal from '../../../../../components/modals/ConfirmationModal'
import { deleteMachine } from '../../../../../redux/actions/topology/machine'
import { Button } from 'reactstrap'

const DeleteMachineContainer = () => {
    const dispatch = useDispatch()
    const [isVisible, setVisible] = useState(false)
    const callback = (isConfirmed) => {
        if (isConfirmed) {
            dispatch(deleteMachine())
        }
        setVisible(false)
    }
    return (
        <>
            <Button color="danger" outline block onClick={() => setVisible(true)}>
                <span className="fa fa-trash mr-2" />
                Delete this machine
            </Button>
            <ConfirmationModal
                title="Delete this machine"
                message="Are you sure you want to delete this machine?"
                show={isVisible}
                callback={callback}
            />
        </>
    )
}

export default DeleteMachineContainer
