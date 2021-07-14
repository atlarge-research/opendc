import PropTypes from 'prop-types'
import React from 'react'
import { InteractionLevel } from '../../../../shapes'
import BuildingSidebarComponent from './building/BuildingSidebarComponent'
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
import { goDownOneInteractionLevel } from '../../../../redux/actions/interaction-level'
import { backButton } from './TopologySidebar.module.scss'
import RoomSidebarContainer from './room/RoomSidebarContainer'
import RackSidebarContainer from './rack/RackSidebarContainer'
import MachineSidebarContainer from './machine/MachineSidebarContainer'

const name = {
    BUILDING: 'Building',
    ROOM: 'Room',
    RACK: 'Rack',
    MACHINE: 'Machine',
}

const TopologySidebar = ({ interactionLevel, onClose }) => {
    let sidebarContent

    switch (interactionLevel.mode) {
        case 'BUILDING':
            sidebarContent = <BuildingSidebarComponent />
            break
        case 'ROOM':
            sidebarContent = <RoomSidebarContainer />
            break
        case 'RACK':
            sidebarContent = <RackSidebarContainer />
            break
        case 'MACHINE':
            sidebarContent = <MachineSidebarContainer />
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
