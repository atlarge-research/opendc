import PropTypes from 'prop-types'
import React from 'react'
import { Button, Toolbar, ToolbarContent, ToolbarGroup, ToolbarItem } from '@patternfly/react-core'
import PlusIcon from '@patternfly/react-icons/dist/js/icons/plus-icon'
import CheckIcon from '@patternfly/react-icons/dist/js/icons/check-icon'

function NewRoomConstructionComponent({ onStart, onFinish, onCancel, currentRoomInConstruction }) {
    if (currentRoomInConstruction === '-1') {
        return (
            <Button isBlock icon={<PlusIcon />} onClick={onStart} ouiaId="start-room-construction">
                Construct a new room
            </Button>
        )
    }
    return (
        <Toolbar
            id="room-construction-toolbar"
            ouiaId="room-construction-toolbar"
            inset={{
                default: 'insetNone',
            }}
        >
            <ToolbarContent toolbarId="room-construction-toolbar">
                <ToolbarGroup>
                    <ToolbarItem>
                        <Button icon={<CheckIcon />} onClick={onFinish} ouiaId="finish-room-construction">
                            Finalize new room
                        </Button>
                    </ToolbarItem>
                    <ToolbarItem widths={{ default: '100%' }}>
                        <Button isBlock variant="secondary" onClick={onCancel} ouiaId="cancel-room-construction">
                            Cancel
                        </Button>
                    </ToolbarItem>
                </ToolbarGroup>
            </ToolbarContent>
        </Toolbar>
    )
}

NewRoomConstructionComponent.propTypes = {
    onStart: PropTypes.func,
    onFinish: PropTypes.func,
    onCancel: PropTypes.func,
    currentRoomInConstruction: PropTypes.string,
}

export default NewRoomConstructionComponent
