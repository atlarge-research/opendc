import React from 'react'
import { useDispatch } from 'react-redux'
import { addPrefab } from '../../../../../actions/prefabs'
import AddPrefabComponent from '../../../../../components/app/sidebars/topology/rack/AddPrefabComponent'

const AddPrefabContainer = (props) => {
    const dispatch = useDispatch()
    return <AddPrefabComponent {...props} onClick={() => dispatch(addPrefab('name'))} />
}

export default AddPrefabContainer
