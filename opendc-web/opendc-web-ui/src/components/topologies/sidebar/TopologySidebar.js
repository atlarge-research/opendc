import PropTypes from 'prop-types'
import React from 'react'
import { InteractionLevel } from '../../../shapes'
import BuildingSidebar from './building/BuildingSidebar'
import {
    Button,
    DrawerActions,
    DrawerCloseButton,
    DrawerHead,
    DrawerPanelBody,
    DrawerPanelContent,
    Flex,
    Title,
} from '@patternfly/react-core'
import { AngleLeftIcon } from '@patternfly/react-icons'
import { useDispatch } from 'react-redux'
import { backButton } from './TopologySidebar.module.css'
import RoomSidebar from './room/RoomSidebar'
import RackSidebar from './rack/RackSidebar'
import MachineSidebar from './machine/MachineSidebar'
import { goDownOneInteractionLevel } from '../../../redux/actions/interaction-level'

const name = {
    BUILDING: 'Building',
    ROOM: 'Room',
    RACK: 'Rack',
    MACHINE: 'Machine',
}

function TopologySidebar({ interactionLevel, onClose }) {
    let sidebarContent

    switch (interactionLevel.mode) {
        case 'BUILDING':
            sidebarContent = <BuildingSidebar />
            break
        case 'ROOM':
            sidebarContent = <RoomSidebar roomId={interactionLevel.roomId} />
            break
        case 'RACK':
            sidebarContent = <RackSidebar tileId={interactionLevel.tileId} />
            break
        case 'MACHINE':
            sidebarContent = <MachineSidebar tileId={interactionLevel.tileId} position={interactionLevel.position} />
            break
        default:
            sidebarContent = 'Missing Content'
    }

    const dispatch = useDispatch()
    const onClick = () => dispatch(goDownOneInteractionLevel())

    return (
        <DrawerPanelContent isResizable defaultSize="450px" minSize="400px">
            <DrawerHead>
                <Flex>
                    <Button
                        variant="tertiary"
                        isSmall
                        className={backButton}
                        onClick={interactionLevel.mode === 'BUILDING' ? onClose : onClick}
                    >
                        <AngleLeftIcon />
                    </Button>
                    <Title className="pf-u-align-self-center" headingLevel="h1">
                        {name[interactionLevel.mode]}
                    </Title>
                </Flex>
                <DrawerActions>
                    <DrawerCloseButton onClose={onClose} />
                </DrawerActions>
            </DrawerHead>
            <DrawerPanelBody>{sidebarContent}</DrawerPanelBody>
        </DrawerPanelContent>
    )
}

TopologySidebar.propTypes = {
    interactionLevel: InteractionLevel,
    onClose: PropTypes.func,
}

export default TopologySidebar
