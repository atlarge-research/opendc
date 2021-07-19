import PropTypes from 'prop-types'
import React from 'react'
import RoomName from './RoomName'
import RackConstructionContainer from './RackConstructionContainer'
import EditRoomContainer from './EditRoomContainer'
import DeleteRoomContainer from './DeleteRoomContainer'
import {
    TextContent,
    TextList,
    TextListItem,
    TextListItemVariants,
    TextListVariants,
    Title,
} from '@patternfly/react-core'

const RoomSidebar = ({ roomId }) => {
    return (
        <TextContent>
            <Title headingLevel="h2">Details</Title>
            <TextList component={TextListVariants.dl}>
                <TextListItem
                    component={TextListItemVariants.dt}
                    className="pf-u-display-inline-flex pf-u-align-items-center"
                >
                    Name
                </TextListItem>
                <TextListItem component={TextListItemVariants.dd}>
                    <RoomName roomId={roomId} />
                </TextListItem>
            </TextList>
            <Title headingLevel="h2">Construction</Title>
            <RackConstructionContainer />
            <EditRoomContainer roomId={roomId} />
            <DeleteRoomContainer roomId={roomId} />
        </TextContent>
    )
}

RoomSidebar.propTypes = {
    roomId: PropTypes.string.isRequired,
}

export default RoomSidebar
