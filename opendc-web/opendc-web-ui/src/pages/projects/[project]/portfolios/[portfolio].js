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
import React, { useRef } from 'react'
import { usePortfolio, useProject } from '../../../../data/project'
import {
    Breadcrumb,
    BreadcrumbItem,
    Card,
    CardActions,
    CardBody,
    CardHeader,
    CardTitle,
    Chip,
    ChipGroup,
    DescriptionList,
    DescriptionListDescription,
    DescriptionListGroup,
    DescriptionListTerm,
    Divider,
    Grid,
    GridItem,
    PageSection,
    PageSectionVariants,
    Skeleton,
    Tab,
    TabContent,
    Tabs,
    TabTitleText,
    Text,
    TextContent,
} from '@patternfly/react-core'
import { AppPage } from '../../../../components/AppPage'
import BreadcrumbLink from '../../../../components/util/BreadcrumbLink'
import { METRIC_NAMES } from '../../../../util/available-metrics'
import NewScenario from '../../../../components/portfolios/NewScenario'
import ScenarioTable from '../../../../components/portfolios/ScenarioTable'
import PortfolioResults from '../../../../components/portfolios/results/PortfolioResults'

/**
 * Page that displays the results in a portfolio.
 */
function Portfolio() {
    const router = useRouter()
    const { project: projectId, portfolio: portfolioId } = router.query

    const { data: project } = useProject(projectId)
    const { data: portfolio } = usePortfolio(portfolioId)

    const overviewRef = useRef(null)
    const resultsRef = useRef(null)

    const breadcrumb = (
        <Breadcrumb>
            <BreadcrumbItem to="/projects" component={BreadcrumbLink}>
                Projects
            </BreadcrumbItem>
            <BreadcrumbItem to={`/projects/${projectId}`} component={BreadcrumbLink}>
                Project details
            </BreadcrumbItem>
            <BreadcrumbItem to={`/projects/${projectId}/portfolios/${portfolioId}`} component={BreadcrumbLink} isActive>
                Portfolio
            </BreadcrumbItem>
        </Breadcrumb>
    )

    return (
        <AppPage breadcrumb={breadcrumb}>
            <Head>
                <title>{project?.name ?? 'Portfolios'} - OpenDC</title>
            </Head>
            <PageSection variant={PageSectionVariants.light}>
                <TextContent>
                    <Text component="h1">Portfolio</Text>
                </TextContent>
            </PageSection>
            <PageSection type="none" variant={PageSectionVariants.light} className="pf-c-page__main-tabs" sticky="top">
                <Divider component="div" />
                <Tabs defaultActiveKey={0} className="pf-m-page-insets">
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
                    <Grid hasGutter>
                        <GridItem md={2}>
                            <Card>
                                <CardTitle>Details</CardTitle>
                                <CardBody>
                                    <DescriptionList>
                                        <DescriptionListGroup>
                                            <DescriptionListTerm>Name</DescriptionListTerm>
                                            <DescriptionListDescription>
                                                {portfolio?.name ?? <Skeleton screenreaderText="Loading portfolio" />}
                                            </DescriptionListDescription>
                                        </DescriptionListGroup>
                                        <DescriptionListGroup>
                                            <DescriptionListTerm>Scenarios</DescriptionListTerm>
                                            <DescriptionListDescription>
                                                {portfolio?.scenarioIds.length ?? (
                                                    <Skeleton screenreaderText="Loading portfolio" />
                                                )}
                                            </DescriptionListDescription>
                                        </DescriptionListGroup>
                                        <DescriptionListGroup>
                                            <DescriptionListTerm>Metrics</DescriptionListTerm>
                                            <DescriptionListDescription>
                                                {portfolio?.targets?.enabledMetrics ? (
                                                    portfolio.targets.enabledMetrics.length > 0 ? (
                                                        <ChipGroup>
                                                            {portfolio.targets.enabledMetrics.map((metric) => (
                                                                <Chip isReadOnly key={metric}>
                                                                    {METRIC_NAMES[metric]}
                                                                </Chip>
                                                            ))}
                                                        </ChipGroup>
                                                    ) : (
                                                        'No metrics enabled'
                                                    )
                                                ) : (
                                                    <Skeleton screenreaderText="Loading portfolio" />
                                                )}
                                            </DescriptionListDescription>
                                        </DescriptionListGroup>
                                        <DescriptionListGroup>
                                            <DescriptionListTerm>Repeats per Scenario</DescriptionListTerm>
                                            <DescriptionListDescription>
                                                {portfolio?.targets?.repeatsPerScenario ?? (
                                                    <Skeleton screenreaderText="Loading portfolio" />
                                                )}
                                            </DescriptionListDescription>
                                        </DescriptionListGroup>
                                    </DescriptionList>
                                </CardBody>
                            </Card>
                        </GridItem>
                        <GridItem md={6}>
                            <Card>
                                <CardHeader>
                                    <CardActions>
                                        <NewScenario portfolioId={portfolioId} />
                                    </CardActions>
                                    <CardTitle>Scenarios</CardTitle>
                                </CardHeader>
                                <CardBody>
                                    <ScenarioTable portfolioId={portfolioId} />
                                </CardBody>
                            </Card>
                        </GridItem>
                    </Grid>
                </TabContent>
                <TabContent eventKey={1} id="results" ref={resultsRef} aria-label="Results tab" hidden>
                    <PortfolioResults portfolioId={portfolioId} />
                </TabContent>
            </PageSection>
        </AppPage>
    )
}

export default Portfolio
