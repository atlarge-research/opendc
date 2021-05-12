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
import React, { useEffect } from 'react'
import Head from 'next/head'
import { useRouter } from 'next/router'
import { HotKeys } from 'react-hotkeys'
import { useDispatch, useSelector } from 'react-redux'
import { openPortfolioSucceeded } from '../../actions/portfolios'
import { openProjectSucceeded } from '../../actions/projects'
import ToolPanelComponent from '../../components/app/map/controls/ToolPanelComponent'
import LoadingScreen from '../../components/app/map/LoadingScreen'
import ScaleIndicatorContainer from '../../containers/app/map/controls/ScaleIndicatorContainer'
import MapStage from '../../containers/app/map/MapStage'
import TopologySidebarContainer from '../../containers/app/sidebars/topology/TopologySidebarContainer'
import DeleteMachineModal from '../../containers/modals/DeleteMachineModal'
import DeleteRackModal from '../../containers/modals/DeleteRackModal'
import DeleteRoomModal from '../../containers/modals/DeleteRoomModal'
import EditRackNameModal from '../../containers/modals/EditRackNameModal'
import EditRoomNameModal from '../../containers/modals/EditRoomNameModal'
import NewTopologyModal from '../../containers/modals/NewTopologyModal'
import AppNavbarContainer from '../../containers/navigation/AppNavbarContainer'
import ProjectSidebarContainer from '../../containers/app/sidebars/project/ProjectSidebarContainer'
import { openScenarioSucceeded } from '../../actions/scenarios'
import NewPortfolioModal from '../../containers/modals/NewPortfolioModal'
import NewScenarioModal from '../../containers/modals/NewScenarioModal'
import PortfolioResultsContainer from '../../containers/app/results/PortfolioResultsContainer'
import KeymapConfiguration from '../../shortcuts/keymap'
import { useRequireAuth } from '../../auth/hook'

const App = ({ projectId, portfolioId, scenarioId }) => {
    useRequireAuth()

    const projectName = useSelector(
        (state) =>
            state.currentProjectId !== '-1' &&
            state.objects.project[state.currentProjectId] &&
            state.objects.project[state.currentProjectId].name
    )
    const topologyIsLoading = useSelector((state) => state.currentTopologyIdd === '-1')

    const dispatch = useDispatch()
    useEffect(() => {
        if (scenarioId) {
            dispatch(openScenarioSucceeded(projectId, portfolioId, scenarioId))
        } else if (portfolioId) {
            dispatch(openPortfolioSucceeded(projectId, portfolioId))
        } else {
            dispatch(openProjectSucceeded(projectId))
        }
    }, [projectId, portfolioId, scenarioId, dispatch])

    const constructionElements = topologyIsLoading ? (
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
    )

    const portfolioElements = (
        <div className="full-height app-page-container">
            <ProjectSidebarContainer />
            <div className="container-fluid full-height">
                <PortfolioResultsContainer />
            </div>
        </div>
    )

    const scenarioElements = (
        <div className="full-height app-page-container">
            <ProjectSidebarContainer />
            <div className="container-fluid full-height">
                <h2>Scenario loading</h2>
            </div>
        </div>
    )

    const title = projectName ? projectName + ' - OpenDC' : 'Simulation - OpenDC'

    return (
        <HotKeys keyMap={KeymapConfiguration} allowChanges={true} className="page-container full-height">
            <Head>
                <title>{title}</title>
            </Head>
            <AppNavbarContainer fullWidth={true} />
            {scenarioId ? scenarioElements : portfolioId ? portfolioElements : constructionElements}
            <NewTopologyModal />
            <NewPortfolioModal />
            <NewScenarioModal />
            <EditRoomNameModal />
            <DeleteRoomModal />
            <EditRackNameModal />
            <DeleteRackModal />
            <DeleteMachineModal />
        </HotKeys>
    )
}

App.propTypes = {
    projectId: PropTypes.string.isRequired,
    portfolioId: PropTypes.string,
    scenarioId: PropTypes.string,
}

export default App
