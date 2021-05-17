import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import { addProject } from '../../redux/actions/projects'
import TextInputModal from '../../components/modals/TextInputModal'
import { Button } from 'reactstrap'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlus } from '@fortawesome/free-solid-svg-icons'

/**
 * A container for creating a new project.
 */
const NewProjectContainer = () => {
    const [isVisible, setVisible] = useState(false)
    const dispatch = useDispatch()
    const callback = (text) => {
        if (text) {
            dispatch(addProject(text))
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
