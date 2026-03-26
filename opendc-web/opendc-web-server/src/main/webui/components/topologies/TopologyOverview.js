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
import {
    Card,
    CardBody,
    CardTitle,
    DescriptionList,
    DescriptionListDescription,
    DescriptionListGroup,
    DescriptionListTerm,
    Grid,
    GridItem,
    Skeleton,
} from '@patternfly/react-core'
import React from 'react'
import { useTopology } from '../../data/topology'
import { parseAndFormatDateTime } from '../../util/date-time'
import RoomTable from './RoomTable'

function TopologyOverview({ projectId, topologyNumber, onSelect }) {
    const { data: topology } = useTopology(projectId, topologyNumber)
    return (
        <Grid hasGutter>
            <GridItem md={2}>
                <Card>
                    <CardTitle>Details</CardTitle>
                    <CardBody>
                        <DescriptionList>
                            <DescriptionListGroup>
                                <DescriptionListTerm>Name</DescriptionListTerm>
                                <DescriptionListDescription>
                                    {topology?.name ?? <Skeleton screenreaderText="Loading topology" />}
                                </DescriptionListDescription>
                            </DescriptionListGroup>
                            <DescriptionListGroup>
                                <DescriptionListTerm>Last edited</DescriptionListTerm>
                                <DescriptionListDescription>
                                    {topology ? (
                                        parseAndFormatDateTime(topology.updatedAt)
                                    ) : (
                                        <Skeleton screenreaderText="Loading topology" />
                                    )}
                                </DescriptionListDescription>
                            </DescriptionListGroup>
                        </DescriptionList>
                    </CardBody>
                </Card>
            </GridItem>
            <GridItem md={5}>
                <Card>
                    <CardTitle>Rooms</CardTitle>
                    <CardBody>
                        <RoomTable
                            projectId={projectId}
                            topologyId={topologyNumber}
                            onSelect={(room) => onSelect('room', room)}
                        />
                    </CardBody>
                </Card>
            </GridItem>
        </Grid>
    )
}

TopologyOverview.propTypes = {
    projectId: PropTypes.number,
    topologyNumber: PropTypes.number,
    onSelect: PropTypes.func,
}

export default TopologyOverview
