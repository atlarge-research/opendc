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
    Label,
    Skeleton,
} from '@patternfly/react-core'
import React from 'react'
import { useExperiment } from '../../data/project'
import { METRIC_NAMES } from '../../util/available-metrics'
import NewScenario from './NewScenario'
import ScenarioTable from './ScenarioTable'

function ExperimentOverview({ projectId, experimentId }) {
    const { status, data: experiment } = useExperiment(projectId, experimentId)

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
                                    {experiment?.name ?? <Skeleton screenreaderText="Loading experiment" />}
                                </DescriptionListDescription>
                            </DescriptionListGroup>
                            <DescriptionListGroup>
                                <DescriptionListTerm>Scenarios</DescriptionListTerm>
                                <DescriptionListDescription>
                                    {experiment?.scenarios?.length ?? <Skeleton screenreaderText="Loading experiment" />}
                                </DescriptionListDescription>
                            </DescriptionListGroup>
                            <DescriptionListGroup>
                                <DescriptionListTerm>Metrics</DescriptionListTerm>
                                <DescriptionListDescription>
                                    {experiment ? (
                                        experiment.targets.metrics.length > 0 ? (
                                            <div className="pf-u-display-flex pf-u-flex-wrap pf-u-gap-xs">
                                                {experiment.targets.metrics.map((metric) => (
                                                    <Label key={metric}>{METRIC_NAMES[metric]}</Label>
                                                ))}
                                            </div>
                                        ) : (
                                            'No metrics enabled'
                                        )
                                    ) : (
                                        <Skeleton screenreaderText="Loading experiment" />
                                    )}
                                </DescriptionListDescription>
                            </DescriptionListGroup>
                            <DescriptionListGroup>
                                <DescriptionListTerm>Repeats per Scenario</DescriptionListTerm>
                                <DescriptionListDescription>
                                    {experiment?.targets?.repeats ?? <Skeleton screenreaderText="Loading experiment" />}
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
                            <NewScenario projectId={projectId} experimentId={experimentId} />
                        </CardActions>
                        <CardTitle>Scenarios</CardTitle>
                    </CardHeader>
                    <CardBody>
                        <ScenarioTable experiment={experiment} status={status} />
                    </CardBody>
                </Card>
            </GridItem>
        </Grid>
    )
}

ExperimentOverview.propTypes = {
    projectId: PropTypes.number,
    experimentId: PropTypes.number,
}

export default ExperimentOverview
