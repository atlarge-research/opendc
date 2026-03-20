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

import React, { useState, useRef } from 'react'
import {
    Bullseye,
    Drawer,
    DrawerContent,
    DrawerContentBody,
    EmptyState,
    EmptyStateIcon,
    Spinner,
    Title,
} from '@patternfly/react-core'
import MapStage from './map/MapStage'
import Collapse from './map/controls/Collapse'
import { useSelector } from 'react-redux'
import TopologySidebar from './sidebar/TopologySidebar'

function TopologyMap() {
    const topologyIsLoading = useSelector((state) => !state.topology.root)
    const interactionLevel = useSelector((state) => state.interactionLevel)

    const [isExpanded, setExpanded] = useState(true)
    const panelContent = <TopologySidebar interactionLevel={interactionLevel} onClose={() => setExpanded(false)} />

    const hotkeysRef = useRef()

    return topologyIsLoading ? (
        <Bullseye>
            <EmptyState>
                <EmptyStateIcon variant="container" component={Spinner} />
                <Title size="lg" headingLevel="h4">
                    Loading Topology
                </Title>
            </EmptyState>
        </Bullseye>
    ) : (
        <Drawer isExpanded={isExpanded}>
            <DrawerContent panelContent={panelContent}>
                <DrawerContentBody style={{ position: 'relative' }}>
                    <MapStage hotkeysRef={hotkeysRef} />
                    <Collapse onClick={() => setExpanded(true)} />
                </DrawerContentBody>
            </DrawerContent>
        </Drawer>
    )
}

export default TopologyMap
