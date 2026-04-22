import PropTypes from 'prop-types'
import React from 'react'
import { Button, FormSelect, FormSelectOption } from '@patternfly/react-core'
import { PlusIcon, TimesIcon } from '@patternfly/react-icons'

const RackConstructionComponent = ({ onStart, onStop, inRackConstructionMode, isEditingRoom, prefabs = [] }) => {
    const [selectedPrefabId, setSelectedPrefabId] = React.useState('')

    if (inRackConstructionMode) {
        return (
            <Button isBlock={true} icon={<TimesIcon />} onClick={onStop} className="pf-u-mb-sm" ouiaId="stop-rack-construction">
                Stop rack construction
            </Button>
        )
    }

    const onChangePrefab = (value) => {
        setSelectedPrefabId(value)
    }

    return (
        <>
            <FormSelect
                value={selectedPrefabId}
                onChange={onChangePrefab}
                aria-label="Select rack prefab"
                className="pf-u-mb-sm"
                ouiaId="rack-construction-prefab-select"
            >
                <FormSelectOption key="" value="" label="Empty Rack" />
                {prefabs.map((prefab) => (
                    <FormSelectOption key={prefab.id} value={prefab.id} label={prefab.name} />
                ))}
            </FormSelect>
            <Button
                icon={<PlusIcon />}
                isBlock
                isDisabled={isEditingRoom}
                ouiaId="start-rack-construction"
                onClick={() => {
                    if (!isEditingRoom) {
                        const prefab = prefabs.find((p) => p.id === parseInt(selectedPrefabId))
                        onStart(prefab)
                    }
                }}
                className="pf-u-mb-sm"
            >
                Start rack construction
            </Button>
        </>
    )
}

RackConstructionComponent.propTypes = {
    onStart: PropTypes.func,
    onStop: PropTypes.func,
    inRackConstructionMode: PropTypes.bool,
    isEditingRoom: PropTypes.bool,
    prefabs: PropTypes.array,
}

export default RackConstructionComponent
