import React from 'react'
import { useDispatch } from 'react-redux'
import { openDeleteRoomModal } from '../../../../../actions/modals/topology'
import DeleteRoomComponent from '../../../../../components/app/sidebars/topology/room/DeleteRoomComponent'

const DeleteRoomContainer = (props) => {
    const dispatch = useDispatch()
    const onClick = () => dispatch(openDeleteRoomModal())
    return <DeleteRoomComponent {...props} onClick={onClick} />
}

export default DeleteRoomContainer
