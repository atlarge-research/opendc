import React, { useState } from 'react'
import { Button } from '@patternfly/react-core'
import { PlusIcon } from '@patternfly/react-icons'
import { useNewProject } from '../../data/project'
import { buttonContainer } from './NewProject.module.scss'
import TextInputModal from '../util/modals/TextInputModal'

/**
 * A container for creating a new project.
 */
const NewProject = () => {
    const [isVisible, setVisible] = useState(false)
    const { mutate: addProject } = useNewProject()

    const onSubmit = (name) => {
        if (name) {
            addProject({ name })
        }
        setVisible(false)
    }

    return (
        <>
            <div className={buttonContainer}>
                <Button
                    icon={<PlusIcon />}
                    color="primary"
                    className="pf-u-float-right"
                    onClick={() => setVisible(true)}
                >
                    New Project
                </Button>
            </div>
            <TextInputModal title="New Project" label="Project name" isOpen={isVisible} callback={onSubmit} />
        </>
    )
}

export default NewProject
