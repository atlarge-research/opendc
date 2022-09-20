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

import { PlusIcon } from '@patternfly/react-icons'
import React, { useMemo, useState } from 'react'
import Head from 'next/head'
import ProjectFilterPanel from '../../components/projects/FilterPanel'
import { AppPage } from '../../components/AppPage'
import {
    PageSection,
    PageSectionVariants,
    Title,
    Toolbar,
    ToolbarItem,
    ToolbarContent,
    Button,
    TextContent,
    Text,
} from '@patternfly/react-core'
import ProjectCollection from '../../components/projects/ProjectCollection'
import TextInputModal from '../../components/util/modals/TextInputModal'
import { useProjects, useDeleteProject, useNewProject } from '../../data/project'

const getVisibleProjects = (projects, filter) => {
    switch (filter) {
        case 'SHOW_ALL':
            return projects
        case 'SHOW_OWN':
            return projects.filter((project) => project.role === 'OWNER')
        case 'SHOW_SHARED':
            return projects.filter((project) => project.role !== 'OWNER')
        default:
            return projects
    }
}

function Projects() {
    const { status, data: projects } = useProjects()
    const [filter, setFilter] = useState('SHOW_ALL')
    const visibleProjects = useMemo(() => getVisibleProjects(projects ?? [], filter), [projects, filter])

    const { mutate: deleteProject } = useDeleteProject()
    const { mutate: addProject } = useNewProject()

    const [isProjectCreationModalVisible, setProjectCreationModalVisible] = useState(false)
    const onProjectCreation = (name) => {
        if (name) {
            addProject({ name })
        }
        setProjectCreationModalVisible(false)
    }

    return (
        <AppPage>
            <Head>
                <title>My Projects - OpenDC</title>
            </Head>
            <PageSection variant={PageSectionVariants.light} isFilled>
                <div className="pf-u-mx-auto pf-u-max-width" style={{ '--pf-u-max-width--MaxWidth': '100ch' }}>
                    <Title className="pf-u-mt-xl-on-md" headingLevel="h1" size="4xl">
                        Welcome
                    </Title>
                    <TextContent>
                        <Text component="p">Find all your personal and shared projects</Text>
                    </TextContent>
                    <Toolbar inset={{ default: 'insetNone' }}>
                        <ToolbarContent>
                            <ToolbarItem>
                                <ProjectFilterPanel onSelect={setFilter} activeFilter={filter} />
                            </ToolbarItem>
                            <ToolbarItem alignment={{ default: 'alignRight' }}>
                                <Button icon={<PlusIcon />} onClick={() => setProjectCreationModalVisible(true)}>
                                    Create Project
                                </Button>
                            </ToolbarItem>
                        </ToolbarContent>
                    </Toolbar>
                    <ProjectCollection
                        status={status}
                        isFiltering={filter !== 'SHOW_ALL'}
                        projects={visibleProjects}
                        onDelete={(project) => deleteProject(project.id)}
                        onCreate={() => setProjectCreationModalVisible(true)}
                    />
                    <TextInputModal
                        title="New Project"
                        label="Project name"
                        isOpen={isProjectCreationModalVisible}
                        callback={onProjectCreation}
                    />
                </div>
            </PageSection>
        </AppPage>
    )
}

export default Projects
