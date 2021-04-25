import React from 'react'

const AddPrefabComponent = ({ onClick }) => (
    <div className="btn btn-primary btn-block" onClick={onClick}>
        <span className="fa fa-floppy-o mr-2" />
        Save this rack to a prefab
    </div>
)

export default AddPrefabComponent
