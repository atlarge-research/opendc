import Link from 'next/link'
import {
    Gallery,
    Bullseye,
    EmptyState,
    EmptyStateIcon,
    Card,
    CardTitle,
    CardActions,
    DropdownItem,
    CardHeader,
    Dropdown,
    KebabToggle,
    CardBody,
    CardHeaderMain,
    TextVariants,
    Text,
    TextContent,
    Tooltip,
    Button,
    Label,
} from '@patternfly/react-core'
import { PlusIcon, FolderIcon, TrashIcon } from '@patternfly/react-icons'
import PropTypes from 'prop-types'
import React, { useReducer, useMemo } from 'react'
import { Project, Status } from '../../shapes'
import { parseAndFormatDateTime } from '../../util/date-time'
import { AUTH_DESCRIPTION_MAP, AUTH_ICON_MAP, AUTH_NAME_MAP } from '../../util/authorizations'
import TableEmptyState from '../util/TableEmptyState'

function ProjectCard({ project, onDelete }) {
    const [isKebabOpen, toggleKebab] = useReducer((t) => !t, false)
    const { id, role, name, updatedAt } = project
    const Icon = AUTH_ICON_MAP[role]

    return (
        <Card
            isCompact
            isRounded
            isFlat
            className="pf-u-min-height"
            style={{ '--pf-u-min-height--MinHeight': '175px' }}
        >
            <CardHeader className="pf-u-flex-grow-1">
                <CardHeaderMain className="pf-u-align-self-flex-start">
                    <FolderIcon />
                </CardHeaderMain>
                <CardActions>
                    <Tooltip content={AUTH_DESCRIPTION_MAP[role]}>
                        <Label icon={<Icon />}>{AUTH_NAME_MAP[role]}</Label>
                    </Tooltip>
                    <Dropdown
                        isPlain
                        position="right"
                        toggle={<KebabToggle className="pf-u-px-0" onToggle={toggleKebab} />}
                        isOpen={isKebabOpen}
                        dropdownItems={[
                            <DropdownItem
                                key="trash"
                                onClick={() => {
                                    onDelete()
                                    toggleKebab()
                                }}
                                position="right"
                                icon={<TrashIcon />}
                            >
                                Delete
                            </DropdownItem>,
                        ]}
                    />
                </CardActions>
            </CardHeader>
            <CardTitle component={Link} className="pf-u-pb-0" href={`/projects/${id}`}>
                {name}
            </CardTitle>
            <CardBody isFilled={false}>
                <TextContent>
                    <Text component={TextVariants.small}>Last modified {parseAndFormatDateTime(updatedAt)}</Text>
                </TextContent>
            </CardBody>
        </Card>
    )
}

function ProjectCollection({ status, projects, onDelete, onCreate, isFiltering }) {
    const sortedProjects = useMemo(() => {
        const res = [...projects]
        res.sort((a, b) => (new Date(a.updatedAt) < new Date(b.updatedAt) ? 1 : -1))
        return res
    }, [projects])

    if (sortedProjects.length === 0) {
        return (
            <TableEmptyState
                status={status}
                isFiltering={isFiltering}
                loadingTitle="Loading Projects"
                emptyTitle="No projects"
                emptyText="You have not created any projects yet. Create a new project to get started quickly."
                emptyAction={
                    <Button icon={<PlusIcon />} onClick={onCreate}>
                        Create Project
                    </Button>
                }
            />
        )
    }

    return (
        <Gallery hasGutter aria-label="Available projects">
            {sortedProjects.map((project) => (
                <ProjectCard key={project.id} project={project} onDelete={() => onDelete(project)} />
            ))}
            <Card isCompact isFlat isRounded style={{ borderStyle: 'dotted' }}>
                <Bullseye>
                    <EmptyState>
                        <Button isBlock variant="link" onClick={onCreate}>
                            <EmptyStateIcon icon={PlusIcon} />
                            <br />
                            Create Project
                        </Button>
                    </EmptyState>
                </Bullseye>
            </Card>
        </Gallery>
    )
}

ProjectCollection.propTypes = {
    status: Status.isRequired,
    isFiltering: PropTypes.bool,
    projects: PropTypes.arrayOf(Project).isRequired,
    onDelete: PropTypes.func,
    onCreate: PropTypes.func,
}

export default ProjectCollection
