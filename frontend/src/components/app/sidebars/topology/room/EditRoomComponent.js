import classNames from 'classnames'
import React from 'react'

const EditRoomComponent = ({
                               onEdit,
                               onFinish,
                               isEditing,
                               isInRackConstructionMode,
                           }) =>
    isEditing ? (
        <div className="btn btn-info btn-block" onClick={onFinish}>
            <span className="fa fa-check mr-2"/>
            Finish editing room
        </div>
    ) : (
        <div
            className={classNames('btn btn-outline-info btn-block', {
                disabled: isInRackConstructionMode,
            })}
            onClick={() => (isInRackConstructionMode ? undefined : onEdit())}
        >
            <span className="fa fa-pencil mr-2"/>
            Edit the tiles of this room
        </div>
    )

export default EditRoomComponent
