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
import Head from 'next/head'
import AppNavbarContainer from '../../../../containers/navigation/AppNavbarContainer'
import React from 'react'
import { useProject } from '../../../../data/project'
import ProjectSidebarContainer from '../../../../containers/app/sidebars/project/ProjectSidebarContainer'
import PortfolioResultsContainer from '../../../../containers/app/results/PortfolioResultsContainer'
import { useDispatch } from 'react-redux'

/**
 * Page that displays the results in a portfolio.
 */
function Portfolio() {
    const router = useRouter()
    const { project: projectId, portfolio: portfolioId } = router.query

    const project = useProject(projectId)
    const title = project?.name ? project?.name + ' - OpenDC' : 'Simulation - OpenDC'

    const dispatch = useDispatch()

    return (
        <div className="page-container full-height">
            <Head>
                <title>{title}</title>
            </Head>
            <AppNavbarContainer fullWidth={true} />
            <div className="full-height app-page-container">
                <ProjectSidebarContainer />
                <div className="container-fluid full-height">
                    <PortfolioResultsContainer />
                </div>
            </div>
        </div>
    )
}

export default Portfolio
