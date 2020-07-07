import React from 'react'

const EmptySlotComponent = ({ position, onAdd }) => (
    <li className="list-group-item d-flex justify-content-between align-items-center">
    <span className="badge badge-default badge-info mr-1 disabled">
      {position}
    </span>
        <button className="btn btn-outline-primary" onClick={onAdd}>
            <span className="fa fa-plus mr-2"/>
            Add machine
        </button>
    </li>
)

export default EmptySlotComponent
