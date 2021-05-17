import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faSave } from '@fortawesome/free-solid-svg-icons'
import { Button } from 'reactstrap'

const AddPrefabComponent = ({ onClick }) => (
    <Button color="primary" block onClick={onClick}>
        <FontAwesomeIcon icon={faSave} className="mr-2" />
        Save this rack to a prefab
    </Button>
)

export default AddPrefabComponent
