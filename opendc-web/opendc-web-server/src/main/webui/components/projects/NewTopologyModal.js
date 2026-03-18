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

import produce from 'immer'
import PropTypes from 'prop-types'
import React, { useRef, useState } from 'react'
import { Form, FormGroup, FormSelect, FormSelectOption, TextInput } from '@patternfly/react-core'
import { useTopologies } from '../../data/topology'
import Modal from '../util/modals/Modal'

const NewTopologyModal = ({ projectId, isOpen, onSubmit: onSubmitUpstream, onCancel: onCancelUpstream }) => {
    const nameInput = useRef(null)
    const [isSubmitted, setSubmitted] = useState(false)
    const [originTopology, setOriginTopology] = useState(-1)
    const [errors, setErrors] = useState({})

    const { data: topologies = [] } = useTopologies(projectId, { enabled: isOpen })

    const clearState = () => {
        if (nameInput.current) {
            nameInput.current.value = ''
        }
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
        } else {
            const candidate = topologies.find((topology) => topology.id === originTopology) || { rooms: [] }
            const topology = produce(candidate, (draft) => {
                delete draft.project
                draft.projectId = projectId
                draft.name = name
            })
            onSubmitUpstream(topology)
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
                    <FormSelect
                        id="origin"
                        name="origin"
                        value={originTopology}
                        onChange={(v) => setOriginTopology(+v)}
                    >
                        <FormSelectOption value={-1} key={-1} label="None - start from scratch" />
                        {topologies.map((topology) => (
                            <FormSelectOption value={topology.id} key={topology.id} label={topology.name} />
                        ))}
                    </FormSelect>
                </FormGroup>
            </Form>
        </Modal>
    )
}

NewTopologyModal.propTypes = {
    projectId: PropTypes.number,
    isOpen: PropTypes.bool.isRequired,
    onSubmit: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
}

export default NewTopologyModal
