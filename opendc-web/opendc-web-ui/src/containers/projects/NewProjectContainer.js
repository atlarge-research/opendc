import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import { addProject } from '../../redux/actions/projects'
import TextInputModal from '../../components/modals/TextInputModal'
import { Button } from 'reactstrap'

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
                    <span className="fa fa-plus mr-2" />
                    New Project
                </Button>
            </div>
            <TextInputModal title="New Project" label="Project title" show={isVisible} callback={callback} />
        </>
    )
}

export default NewProjectContainer
