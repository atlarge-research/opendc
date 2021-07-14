import PropTypes from 'prop-types'
import React from 'react'
import UnitTabsComponent from './UnitTabsComponent'
import DeleteMachine from './DeleteMachine'
import {
    TextContent,
    TextList,
    TextListItem,
    TextListItemVariants,
    TextListVariants,
    Title,
} from '@patternfly/react-core'
import { useSelector } from 'react-redux'

const MachineSidebarComponent = ({ machineId }) => {
    const machine = useSelector((state) => state.objects.machine[machineId])
    return (
        <div>
            <TextContent>
                <Title headingLevel="h2">Details</Title>
                <TextList component={TextListVariants.dl}>
                    <TextListItem component={TextListItemVariants.dt}>Name</TextListItem>
                    <TextListItem component={TextListItemVariants.dd}>
                        Machine at position {machine.position}
                    </TextListItem>
                </TextList>

                <Title headingLevel="h2">Actions</Title>
                <DeleteMachine />

                <Title headingLevel="h2">Units</Title>
            </TextContent>
            <div className="pf-u-h-100">
                <UnitTabsComponent />
            </div>
        </div>
    )
}

MachineSidebarComponent.propTypes = {
    machineId: PropTypes.string,
}

export default MachineSidebarComponent
