import PropTypes from 'prop-types'
import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus } from '@fortawesome/free-solid-svg-icons'
import { ListGroupItem, Badge, Button } from 'reactstrap'

const EmptySlotComponent = ({ position, onAdd }) => (
    <ListGroupItem className="d-flex justify-content-between align-items-center">
        <Badge color="info" className="mr-1">
            {position}
        </Badge>
        <Button color="primary" outline onClick={onAdd}>
            <FontAwesomeIcon icon={faPlus} className="mr-2" />
            Add machine
        </Button>
    </ListGroupItem>
)

EmptySlotComponent.propTypes = {
    position: PropTypes.number,
    onAdd: PropTypes.func,
}

export default EmptySlotComponent
