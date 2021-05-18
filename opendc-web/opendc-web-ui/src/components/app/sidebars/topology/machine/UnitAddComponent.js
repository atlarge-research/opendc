import PropTypes from 'prop-types'
import React, { useRef } from 'react'
import { Button, Form, FormGroup, Input } from 'reactstrap'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus } from '@fortawesome/free-solid-svg-icons'

function UnitAddComponent({ units, onAdd }) {
    const unitSelect = useRef(null)

    return (
        <Form inline>
            <FormGroup className="w-100">
                <Input type="select" className="w-70 mr-1" innerRef={unitSelect}>
                    {units.map((unit) => (
                        <option value={unit._id} key={unit._id}>
                            {unit.name}
                        </option>
                    ))}
                </Input>
                <Button color="primary" outline onClick={() => onAdd(unitSelect.current.value)}>
                    <FontAwesomeIcon icon={faPlus} className="mr-2" />
                    Add
                </Button>
            </FormGroup>
        </Form>
    )
}

UnitAddComponent.propTypes = {
    units: PropTypes.array.isRequired,
    onAdd: PropTypes.func.isRequired,
}

export default UnitAddComponent
