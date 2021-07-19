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
import { useProject } from '../../../data/project'
import { AppPage } from '../../../components/AppPage'
import Head from 'next/head'
import {
    Breadcrumb,
    BreadcrumbItem,
    Card,
    CardActions,
    CardBody,
    CardHeader,
    CardTitle,
    DescriptionList,
    DescriptionListDescription,
    DescriptionListGroup,
    DescriptionListTerm,
    Grid,
    GridItem,
    PageSection,
    PageSectionVariants,
    Skeleton,
    Text,
    TextContent,
} from '@patternfly/react-core'
import BreadcrumbLink from '../../../components/util/BreadcrumbLink'
import PortfolioTable from '../../../components/projects/PortfolioTable'
import TopologyTable from '../../../components/projects/TopologyTable'
import NewTopology from '../../../components/projects/NewTopology'
import NewPortfolio from '../../../components/projects/NewPortfolio'

function Project() {
    const router = useRouter()
    const { project: projectId } = router.query

    const { data: project } = useProject(projectId)

    const breadcrumb = (
        <Breadcrumb>
            <BreadcrumbItem to="/projects" component={BreadcrumbLink}>
                Projects
            </BreadcrumbItem>
            <BreadcrumbItem to={`/projects/${projectId}`} component={BreadcrumbLink} isActive>
                Project details
            </BreadcrumbItem>
        </Breadcrumb>
    )

    return (
        <AppPage breadcrumb={breadcrumb}>
            <Head>
                <title>{project?.name ?? 'Project'} - OpenDC</title>
            </Head>
            <PageSection variant={PageSectionVariants.light}>
                <TextContent>
                    <Text component="h1">
                        {project?.name ?? <Skeleton width="15%" screenreaderText="Loading project" />}
                    </Text>
                </TextContent>
            </PageSection>
            <PageSection isFilled>
                <Grid hasGutter>
                    <GridItem md={2}>
                        <Card>
                            <CardTitle>Details</CardTitle>
                            <CardBody>
                                <DescriptionList>
                                    <DescriptionListGroup>
                                        <DescriptionListTerm>Name</DescriptionListTerm>
                                        <DescriptionListDescription>
                                            {project?.name ?? <Skeleton screenreaderText="Loading project" />}
                                        </DescriptionListDescription>
                                    </DescriptionListGroup>
                                </DescriptionList>
                            </CardBody>
                        </Card>
                    </GridItem>
                    <GridItem md={5}>
                        <Card>
                            <CardHeader>
                                <CardActions>
                                    <NewTopology projectId={projectId} />
                                </CardActions>
                                <CardTitle>Topologies</CardTitle>
                            </CardHeader>
                            <CardBody>
                                <TopologyTable projectId={projectId} />
                            </CardBody>
                        </Card>
                    </GridItem>
                    <GridItem md={5}>
                        <Card>
                            <CardHeader>
                                <CardActions>
                                    <NewPortfolio projectId={projectId} />
                                </CardActions>
                                <CardTitle>Portfolios</CardTitle>
                            </CardHeader>
                            <CardBody>
                                <PortfolioTable projectId={projectId} />
                            </CardBody>
                        </Card>
                    </GridItem>
                </Grid>
            </PageSection>
        </AppPage>
    )
}

export default Project
