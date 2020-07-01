import React from 'react'

const DeleteRoomComponent = ({ onClick }) => (
    <div className="btn btn-outline-danger btn-block" onClick={onClick}>
        <span className="fa fa-trash mr-2"/>
        Delete this room
    </div>
)

export default DeleteRoomComponent
