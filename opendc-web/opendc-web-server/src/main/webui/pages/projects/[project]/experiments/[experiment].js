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

import dynamic from 'next/dynamic'
import { useRouter } from 'next/router'
import Head from 'next/head'
import Link from 'next/link'
import React, { useRef } from 'react'
import {
    Breadcrumb,
    BreadcrumbItem,
    Divider,
    PageSection,
    PageSectionVariants,
    Tab,
    TabContent,
    Tabs,
    TabTitleText,
    Text,
    TextContent,
} from '@patternfly/react-core'
import { AppPage } from '../../../../components/AppPage'
import ContextSelectionSection from '../../../../components/context/ContextSelectionSection'
import ExperimentSelector from '../../../../components/context/ExperimentSelector'
import ExperimentOverview from '../../../../components/experiments/ExperimentOverview'
import { useExperiment } from '../../../../data/project'

const ExperimentResults = dynamic(() => import('../../../../components/experiments/ExperimentResults'), { ssr: false })

/**
 * Page that displays the results in an experiment.
 */
function Experiment() {
    const router = useRouter()
    const projectId = +router.query['project']
    const experimentNumber = +router.query['experiment']

    const overviewRef = useRef(null)
    const resultsRef = useRef(null)

    const { data: experiment } = useExperiment(projectId, experimentNumber)

    const breadcrumb = (
        <Breadcrumb>
            <BreadcrumbItem to="/projects" component={Link}>
                Projects
            </BreadcrumbItem>
            <BreadcrumbItem to={`/projects/${projectId}`} component={Link}>
                Project details
            </BreadcrumbItem>
            <BreadcrumbItem to={`/projects/${projectId}/experiments/${experimentNumber}`} component={Link} isActive>
                Experiment
            </BreadcrumbItem>
        </Breadcrumb>
    )

    const contextSelectors = (
        <ContextSelectionSection>
            <ExperimentSelector activeExperiment={experiment} />
        </ContextSelectionSection>
    )

    return (
        <AppPage breadcrumb={breadcrumb} contextSelectors={contextSelectors}>
            <Head>
                <title>Experiment - OpenDC</title>
            </Head>
            <PageSection variant={PageSectionVariants.light}>
                <TextContent>
                    <Text component="h1" ouiaId="experiment-heading">Experiment</Text>
                </TextContent>
            </PageSection>
            <PageSection type="tabs" variant={PageSectionVariants.light} stickyOnBreakpoint={{ default: 'top' }}>
                <Divider component="div" />
                <Tabs defaultActiveKey={0} className="pf-m-page-insets" ouiaId="experiment-tabs">
                    <Tab
                        eventKey={0}
                        title={<TabTitleText>Overview</TabTitleText>}
                        tabContentId="overview"
                        tabContentRef={overviewRef}
                    />
                    <Tab
                        eventKey={1}
                        title={<TabTitleText>Results</TabTitleText>}
                        tabContentId="results"
                        tabContentRef={resultsRef}
                    />
                </Tabs>
            </PageSection>
            <PageSection isFilled>
                <TabContent eventKey={0} id="overview" ref={overviewRef} aria-label="Overview tab">
                    <ExperimentOverview projectId={projectId} experimentId={experimentNumber} />
                </TabContent>
                <TabContent eventKey={1} id="results" ref={resultsRef} aria-label="Results tab" hidden>
                    <ExperimentResults projectId={projectId} experimentId={experimentNumber} />
                </TabContent>
            </PageSection>
        </AppPage>
    )
}

export default Experiment
