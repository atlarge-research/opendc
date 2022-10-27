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
import ProjectOverview from '../../../components/projects/ProjectOverview'
import { useProject } from '../../../data/project'
import { AppPage } from '../../../components/AppPage'
import Head from 'next/head'
import Link from 'next/link'
import {
    Breadcrumb,
    BreadcrumbItem,
    PageSection,
    PageSectionVariants,
    Skeleton,
    Text,
    TextContent,
} from '@patternfly/react-core'

function Project() {
    const router = useRouter()
    const projectId = +router.query['project']

    const { data: project } = useProject(+projectId)

    const breadcrumb = (
        <Breadcrumb>
            <BreadcrumbItem to="/projects" component={Link}>
                Projects
            </BreadcrumbItem>
            <BreadcrumbItem to={`/projects/${projectId}`} component={Link} isActive>
                Project details
            </BreadcrumbItem>
        </Breadcrumb>
    )

    return (
        <AppPage breadcrumb={breadcrumb}>
            <Head>
                <title>{`${project?.name ?? 'Project'} - OpenDC`}</title>
            </Head>
            <PageSection variant={PageSectionVariants.light}>
                <TextContent>
                    <Text component="h1">
                        {project?.name ?? <Skeleton width="15%" screenreaderText="Loading project" />}
                    </Text>
                </TextContent>
            </PageSection>
            <PageSection isFilled>
                <ProjectOverview projectId={projectId} />
            </PageSection>
        </AppPage>
    )
}

export default Project
