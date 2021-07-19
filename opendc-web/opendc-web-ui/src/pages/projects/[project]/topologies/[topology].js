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
import React, { useEffect, useRef, useState } from 'react'
import { configure, HotKeys } from 'react-hotkeys'
import { KeymapConfiguration } from '../../../../hotkeys'
import Head from 'next/head'
import { openProjectSucceeded } from '../../../../redux/actions/projects'
import { AppPage } from '../../../../components/AppPage'
import {
    Breadcrumb,
    BreadcrumbItem,
    Bullseye,
    Divider,
    Drawer,
    DrawerContent,
    DrawerContentBody,
    EmptyState,
    EmptyStateIcon,
    PageSection,
    PageSectionVariants,
    Spinner,
    Tab,
    TabContent,
    Tabs,
    TabTitleText,
    Text,
    TextContent,
    Title,
} from '@patternfly/react-core'
import BreadcrumbLink from '../../../../components/util/BreadcrumbLink'
import MapStage from '../../../../components/topologies/map/MapStage'
import Collapse from '../../../../components/topologies/map/controls/Collapse'
import TopologySidebar from '../../../../components/topologies/sidebar/TopologySidebar'

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

    const [activeTab, setActiveTab] = useState('overview')
    const overviewRef = useRef(null)
    const floorPlanRef = useRef(null)

    const topologyIsLoading = useSelector((state) => state.currentTopologyId === '-1')
    const interactionLevel = useSelector((state) => state.interactionLevel)

    const [isExpanded, setExpanded] = useState(true)

    const breadcrumb = (
        <Breadcrumb>
            <BreadcrumbItem to="/projects" component={BreadcrumbLink}>
                Projects
            </BreadcrumbItem>
            <BreadcrumbItem to={`/projects/${projectId}`} component={BreadcrumbLink}>
                Project details
            </BreadcrumbItem>
            <BreadcrumbItem to={`/projects/${projectId}/topologies/${topologyId}`} component={BreadcrumbLink} isActive>
                Topology
            </BreadcrumbItem>
        </Breadcrumb>
    )

    const panelContent = <TopologySidebar interactionLevel={interactionLevel} onClose={() => setExpanded(false)} />

    // Make sure that holding down a key will generate repeated events
    configure({
        ignoreRepeatedEventsWhenKeyHeldDown: false,
    })

    return (
        <AppPage breadcrumb={breadcrumb}>
            <Head>
                <title>{project?.name ?? 'Topologies'} - OpenDC</title>
            </Head>
            <PageSection variant={PageSectionVariants.light}>
                <TextContent>
                    <Text component="h1">Topology</Text>
                </TextContent>
            </PageSection>
            <PageSection type="none" variant={PageSectionVariants.light} className="pf-c-page__main-tabs" sticky="top">
                <Divider component="div" />
                <Tabs
                    activeKey={activeTab}
                    onSelect={(_, tabIndex) => setActiveTab(tabIndex)}
                    className="pf-m-page-insets"
                >
                    <Tab
                        eventKey="overview"
                        title={<TabTitleText>Overview</TabTitleText>}
                        tabContentId="overview"
                        tabContentRef={overviewRef}
                    />
                    <Tab
                        eventKey="floor-plan"
                        title={<TabTitleText>Floor Plan</TabTitleText>}
                        tabContentId="floor-plan"
                        tabContentRef={floorPlanRef}
                    />
                </Tabs>
            </PageSection>
            <PageSection padding={activeTab === 'floor-plan' && { default: 'noPadding' }} isFilled>
                <TabContent eventKey="overview" id="overview" ref={overviewRef} aria-label="Overview tab">
                    Test
                </TabContent>
                <TabContent
                    eventKey="floor-plan"
                    id="floor-plan"
                    ref={floorPlanRef}
                    aria-label="Floor Plan tab"
                    className="pf-u-h-100"
                    hidden
                >
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
                </TabContent>
            </PageSection>
        </AppPage>
    )
}

export default Topology
