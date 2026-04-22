/*
 * Copyright (c) 2021 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import PropTypes from 'prop-types'
import React from 'react'
import {
    TextContent,
    TextList,
    TextListItem,
    TextListItemVariants,
    TextListVariants,
    Title,
} from '@patternfly/react-core'
import DatacenterName from './DatacenterName'
import DeleteDatacenterContainer from './DeleteDatacenterContainer'
import NewRoomConstructionContainer from '../building/NewRoomConstructionContainer'
import ResizeDatacenterContainer from './ResizeDatacenterContainer'

const DatacenterSidebar = ({ datacenterId }) => {
    return (
        <TextContent>
            <Title headingLevel="h2" ouiaId="datacenter-details-title">Details</Title>
            <TextList component={TextListVariants.dl}>
                <TextListItem
                    component={TextListItemVariants.dt}
                    className="pf-u-display-inline-flex pf-u-align-items-center"
                >
                    Name
                </TextListItem>
                <TextListItem component={TextListItemVariants.dd}>
                    <DatacenterName datacenterId={datacenterId} />
                </TextListItem>
            </TextList>
            <Title headingLevel="h2" ouiaId="datacenter-floorplan-title">Floor Plan</Title>
            <ResizeDatacenterContainer datacenterId={datacenterId} />
            <Title headingLevel="h2" ouiaId="datacenter-rooms-title">Rooms</Title>
            <NewRoomConstructionContainer />
            <DeleteDatacenterContainer datacenterId={datacenterId} />
        </TextContent>
    )
}

DatacenterSidebar.propTypes = {
    datacenterId: PropTypes.string.isRequired,
}

export default DatacenterSidebar
