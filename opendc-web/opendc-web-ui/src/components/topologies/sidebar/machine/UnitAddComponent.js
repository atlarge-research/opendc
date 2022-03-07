import PropTypes from 'prop-types'
import React, { useState } from 'react'
import { Button, InputGroup, Select, SelectOption, SelectVariant } from '@patternfly/react-core'
import PlusIcon from '@patternfly/react-icons/dist/js/icons/plus-icon'

function UnitAddComponent({ units, onAdd }) {
    const [isOpen, setOpen] = useState(false)
    const [selected, setSelected] = useState(null)

    return (
        <InputGroup>
            <Select
                variant={SelectVariant.single}
                placeholderText="Select a unit"
                aria-label="Select Unit"
                onToggle={() => setOpen(!isOpen)}
                isOpen={isOpen}
                onSelect={(_, selection) => {
                    setSelected(selection)
                    setOpen(false)
                }}
                selections={selected}
            >
                {units.map((unit) => (
                    <SelectOption value={unit.id} key={unit.id}>
                        {unit.name}
                    </SelectOption>
                ))}
            </Select>
            <Button icon={<PlusIcon />} variant="control" onClick={() => onAdd(selected)}>
                Add
            </Button>
        </InputGroup>
    )
}

UnitAddComponent.propTypes = {
    units: PropTypes.array.isRequired,
    onAdd: PropTypes.func.isRequired,
}

export default UnitAddComponent
