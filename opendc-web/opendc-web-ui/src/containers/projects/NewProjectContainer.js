import React, { useState } from 'react'
import TextInputModal from '../../components/modals/TextInputModal'
import { Button } from 'reactstrap'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus } from '@fortawesome/free-solid-svg-icons'
import { useMutation } from 'react-query'

/**
 * A container for creating a new project.
 */
const NewProjectContainer = () => {
    const [isVisible, setVisible] = useState(false)
    const { mutate: addProject } = useMutation('addProject')
    const callback = (text) => {
        if (text) {
            addProject({ name: text })
        }
        setVisible(false)
    }

    return (
        <>
            <div className="bottom-btn-container">
                <Button color="primary" className="float-right" onClick={() => setVisible(true)}>
                    <FontAwesomeIcon icon={faPlus} className="mr-2" />
                    New Project
                </Button>
            </div>
            <TextInputModal title="New Project" label="Project title" show={isVisible} callback={callback} />
        </>
    )
}

export default NewProjectContainer
