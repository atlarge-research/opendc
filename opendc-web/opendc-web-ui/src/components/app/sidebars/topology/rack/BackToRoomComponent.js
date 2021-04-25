import React from 'react'

const BackToRoomComponent = ({ onClick }) => (
    <div className="btn btn-secondary btn-block mb-2" onClick={onClick}>
        <span className="fa fa-angle-left mr-2" />
        Back to room
    </div>
)

export default BackToRoomComponent
