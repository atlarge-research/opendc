import PropTypes from 'prop-types'
import React from 'react'
import { Button } from '@patternfly/react-core'
import { PlusIcon, TimesIcon } from '@patternfly/react-icons'

const RackConstructionComponent = ({ onStart, onStop, inRackConstructionMode, isEditingRoom }) => {
    if (inRackConstructionMode) {
        return (
            <Button isBlock={true} icon={<TimesIcon />} onClick={onStop} className="pf-u-mb-sm">
                Stop rack construction
            </Button>
        )
    }

    return (
        <Button
            icon={<PlusIcon />}
            isBlock
            isDisabled={isEditingRoom}
            onClick={() => (isEditingRoom ? undefined : onStart())}
            className="pf-u-mb-sm"
        >
            Start rack construction
        </Button>
    )
}

RackConstructionComponent.propTypes = {
    onStart: PropTypes.func,
    onStop: PropTypes.func,
    inRackConstructionMode: PropTypes.bool,
    isEditingRoom: PropTypes.bool,
}

export default RackConstructionComponent
