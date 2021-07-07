import React, { useState } from 'react'
import TextInputModal from '../../components/modals/TextInputModal'
import { Button } from 'reactstrap'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus } from '@fortawesome/free-solid-svg-icons'
import { useMutation, useQueryClient } from 'react-query'
import { addProject } from '../../api/projects'
import { useAuth } from '../../auth'

/**
 * A container for creating a new project.
 */
const NewProjectContainer = () => {
    const [isVisible, setVisible] = useState(false)
    const auth = useAuth()
    const queryClient = useQueryClient()
    const mutation = useMutation((data) => addProject(auth, data), {
        onSuccess: (result) => queryClient.setQueryData('projects', (old) => [...(old || []), result]),
    })
    const callback = (text) => {
        if (text) {
            mutation.mutate({ name: text })
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
