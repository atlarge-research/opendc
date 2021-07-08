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
import React, { useEffect } from 'react'
import { HotKeys } from 'react-hotkeys'
import { KeymapConfiguration } from '../../../../hotkeys'
import Head from 'next/head'
import AppNavbarContainer from '../../../../containers/navigation/AppNavbarContainer'
import LoadingScreen from '../../../../components/app/map/LoadingScreen'
import MapStage from '../../../../containers/app/map/MapStage'
import ScaleIndicatorContainer from '../../../../containers/app/map/controls/ScaleIndicatorContainer'
import ToolPanelComponent from '../../../../components/app/map/controls/ToolPanelComponent'
import ProjectSidebarContainer from '../../../../containers/app/sidebars/project/ProjectSidebarContainer'
import TopologySidebarContainer from '../../../../containers/app/sidebars/topology/TopologySidebarContainer'
import { openProjectSucceeded } from '../../../../redux/actions/projects'

/**
 * Page that displays a datacenter topology.
 */
function Topology() {
    const router = useRouter()
    const { project: projectId, topology: topologyId } = router.query

    const { data: project } = useProject(projectId)
    const title = project?.name ? project?.name + ' - OpenDC' : 'Simulation - OpenDC'

    const dispatch = useDispatch()
    useEffect(() => {
        if (projectId) {
            dispatch(openProjectSucceeded(projectId))
        }
    }, [projectId, topologyId, dispatch])

    const topologyIsLoading = useSelector((state) => state.currentTopologyId === '-1')

    return (
        <HotKeys keyMap={KeymapConfiguration} allowChanges={true} className="page-container full-height">
            <Head>
                <title>{title}</title>
            </Head>
            <AppNavbarContainer fullWidth={true} />
            {topologyIsLoading ? (
                <div className="full-height d-flex align-items-center justify-content-center">
                    <LoadingScreen />
                </div>
            ) : (
                <div className="full-height">
                    <MapStage />
                    <ScaleIndicatorContainer />
                    <ToolPanelComponent />
                    <ProjectSidebarContainer />
                    <TopologySidebarContainer />
                </div>
            )}
        </HotKeys>
    )
}

export default Topology
