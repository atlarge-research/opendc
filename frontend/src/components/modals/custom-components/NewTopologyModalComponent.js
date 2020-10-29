import PropTypes from 'prop-types'
import { Form, FormGroup, Input, Label } from 'reactstrap'
import React, { useRef } from 'react'
import Shapes from '../../../shapes'
import Modal from '../Modal'

const NewTopologyModalComponent = ({ show, onCreateTopology, onDuplicateTopology, onCancel, topologies }) => {
    const textInput = useRef(null)
    const originTopology = useRef(null)

    const onCreate = () => {
        onCreateTopology(textInput.current.value)
    }

    const onDuplicate = () => {
        onDuplicateTopology(textInput.current.value, originTopology.current.value)
    }

    const onSubmit = () => {
        if (originTopology.current.selectedIndex === 0) {
            onCreate()
        } else {
            onDuplicate()
        }
    }

    return (
        <Modal title="New Topology" show={show} onSubmit={onSubmit} onCancel={onCancel}>
            <Form
                onSubmit={(e) => {
                    e.preventDefault()
                    onSubmit()
                }}
            >
                <FormGroup>
                    <Label for="name">Name</Label>
                    <Input name="name" type="text" required innerRef={textInput} />
                </FormGroup>
                <FormGroup>
                    <Label for="origin">Topology to duplicate</Label>
                    <Input name="origin" type="select" innerRef={originTopology}>
                        <option value={-1} key={-1}>
                            None - start from scratch
                        </option>
                        {topologies.map((topology) => (
                            <option value={topology._id} key={topology._id}>
                                {topology.name}
                            </option>
                        ))}
                    </Input>
                </FormGroup>
            </Form>
        </Modal>
    )
}

NewTopologyModalComponent.propTypes = {
    show: PropTypes.bool.isRequired,
    topologies: PropTypes.arrayOf(Shapes.Topology),
    onCreateTopology: PropTypes.func.isRequired,
    onDuplicateTopology: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
}

export default NewTopologyModalComponent
