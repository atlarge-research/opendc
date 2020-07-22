import classNames from 'classnames'
import React from 'react'

const RackConstructionComponent = ({ onStart, onStop, inRackConstructionMode, isEditingRoom }) => {
    if (inRackConstructionMode) {
        return (
            <div className="btn btn-primary btn-block" onClick={onStop}>
                <span className="fa fa-times mr-2" />
                Stop rack construction
            </div>
        )
    }

    return (
        <div
            className={classNames('btn btn-outline-primary btn-block', {
                disabled: isEditingRoom,
            })}
            onClick={() => (isEditingRoom ? undefined : onStart())}
        >
            <span className="fa fa-plus mr-2" />
            Start rack construction
        </div>
    )
}

export default RackConstructionComponent
