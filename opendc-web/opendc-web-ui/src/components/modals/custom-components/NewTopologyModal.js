import PropTypes from 'prop-types'
import React, { useRef, useState } from 'react'
import Modal from '../Modal'
import { Form, FormGroup, FormSelect, FormSelectOption, TextInput } from '@patternfly/react-core'
import { useProjectTopologies } from '../../../data/topology'

const NewTopologyModal = ({ projectId, isOpen, onSubmit: onSubmitUpstream, onCancel: onCancelUpstream }) => {
    const nameInput = useRef(null)
    const [isSubmitted, setSubmitted] = useState(false)
    const [originTopology, setOriginTopology] = useState(-1)
    const [errors, setErrors] = useState({})

    const { data: topologies = [] } = useProjectTopologies(projectId)

    const clearState = () => {
        nameInput.current.value = ''
        setSubmitted(false)
        setOriginTopology(-1)
        setErrors({})
    }

    const onSubmit = (event) => {
        setSubmitted(true)

        if (event) {
            event.preventDefault()
        }

        const name = nameInput.current.value

        if (!name) {
            setErrors({ name: true })
            return false
        } else if (originTopology === -1) {
            onSubmitUpstream(name)
        } else {
            onSubmitUpstream(name, originTopology)
        }

        clearState()
        return true
    }

    const onCancel = () => {
        onCancelUpstream()
        clearState()
    }

    return (
        <Modal title="New Topology" isOpen={isOpen} onSubmit={onSubmit} onCancel={onCancel}>
            <Form onSubmit={onSubmit}>
                <FormGroup
                    label="Name"
                    fieldId="name"
                    isRequired
                    validated={isSubmitted && errors.name ? 'error' : 'default'}
                    helperTextInvalid="This field cannot be empty"
                >
                    <TextInput id="name" name="name" type="text" isRequired ref={nameInput} />
                </FormGroup>
                <FormGroup label="Topology to duplicate" fieldId="origin" isRequired>
                    <FormSelect id="origin" name="origin" value={originTopology} onChange={setOriginTopology}>
                        <FormSelectOption value={-1} key={-1} label="None - start from scratch" />
                        {topologies.map((topology) => (
                            <FormSelectOption value={topology._id} key={topology._id} label={topology.name} />
                        ))}
                    </FormSelect>
                </FormGroup>
            </Form>
        </Modal>
    )
}

NewTopologyModal.propTypes = {
    projectId: PropTypes.string,
    isOpen: PropTypes.bool.isRequired,
    onSubmit: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
}

export default NewTopologyModal
