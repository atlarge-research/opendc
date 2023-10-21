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
import {
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
    Skeleton,
} from '@patternfly/react-core'
import NewTopology from './NewTopology'
import TopologyTable from './TopologyTable'
import NewPortfolio from './NewPortfolio'
import PortfolioTable from './PortfolioTable'
import { useProject } from '../../data/project'

function ProjectOverview({ projectId }) {
    const { data: project } = useProject(projectId)

    return (
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
    )
}

ProjectOverview.propTypes = {
    projectId: PropTypes.number,
}

export default ProjectOverview
