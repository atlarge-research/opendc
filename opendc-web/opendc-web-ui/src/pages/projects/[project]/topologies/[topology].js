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

import { useRouter } from 'next/router'
import { useProject } from '../../../../data/project'
import { useDispatch, useSelector } from 'react-redux'
import React, { useEffect, useState } from 'react'
import { configure, HotKeys } from 'react-hotkeys'
import { KeymapConfiguration } from '../../../../hotkeys'
import Head from 'next/head'
import MapStage from '../../../../components/app/map/MapStage'
import { openProjectSucceeded } from '../../../../redux/actions/projects'
import { AppPage } from '../../../../components/AppPage'
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
import Toolbar from '../../../../components/app/map/controls/Toolbar'
import ScaleIndicator from '../../../../components/app/map/controls/ScaleIndicator'
import TopologySidebar from '../../../../components/app/sidebars/topology/TopologySidebar'
import Collapse from '../../../../components/app/map/controls/Collapse'

/**
 * Page that displays a datacenter topology.
 */
function Topology() {
    const router = useRouter()
    const { project: projectId, topology: topologyId } = router.query

    const { data: project } = useProject(projectId)

    const dispatch = useDispatch()
    useEffect(() => {
        if (projectId) {
            dispatch(openProjectSucceeded(projectId))
        }
    }, [projectId, topologyId, dispatch])

    const topologyIsLoading = useSelector((state) => state.currentTopologyId === '-1')
    const interactionLevel = useSelector((state) => state.interactionLevel)

    const [isExpanded, setExpanded] = useState(true)
    const panelContent = <TopologySidebar interactionLevel={interactionLevel} onClose={() => setExpanded(false)} />

    // Make sure that holding down a key will generate repeated events
    configure({
        ignoreRepeatedEventsWhenKeyHeldDown: false,
    })

    return (
        <AppPage>
            <Head>
                <title>{project?.name ?? 'Topologies'} - OpenDC</title>
            </Head>
            {topologyIsLoading ? (
                <Bullseye>
                    <EmptyState>
                        <EmptyStateIcon variant="container" component={Spinner} />
                        <Title size="lg" headingLevel="h4">
                            Loading Topology
                        </Title>
                    </EmptyState>
                </Bullseye>
            ) : (
                <HotKeys keyMap={KeymapConfiguration} allowChanges={true} className="full-height">
                    <Drawer isExpanded={isExpanded}>
                        <DrawerContent panelContent={panelContent}>
                            <DrawerContentBody>
                                <MapStage />
                                <Collapse onClick={() => setExpanded(true)} />
                            </DrawerContentBody>
                        </DrawerContent>
                    </Drawer>
                </HotKeys>
            )}
        </AppPage>
    )
}

export default Topology
