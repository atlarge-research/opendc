import PropTypes from 'prop-types'
import React, { useRef, useState } from 'react'
import Modal from './Modal'
import { Form, FormGroup, TextInput } from '@patternfly/react-core'

function TextInputModal({ title, label, isOpen, callback, initialValue }) {
    const textInput = useRef(null)
    const [isSubmitted, setSubmitted] = useState(false)
    const [isValid, setValid] = useState(true)

    const resetState = () => {
        textInput.current.value = ''
        setSubmitted(false)
        setValid(false)
    }
    const onSubmit = (event) => {
        const value = textInput.current.value
        setSubmitted(true)

        if (event) {
            event.preventDefault()
        }

        if (!value) {
            setValid(false)
            return false
        }

        callback(value)
        resetState()
        return true
    }
    const onCancel = () => {
        callback(undefined)
        resetState()
    }

    return (
        <Modal title={title} isOpen={isOpen} onSubmit={onSubmit} onCancel={onCancel}>
            <Form onSubmit={onSubmit}>
                <FormGroup
                    label={label}
                    fieldId="text-input"
                    isRequired
                    validated={isSubmitted && !isValid ? 'error' : 'default'}
                    helperTextInvalid="This field cannot be empty"
                >
                    <TextInput
                        id="text-input"
                        name="text-input"
                        isRequired
                        type="text"
                        ref={textInput}
                        defaultValue={initialValue}
                    />
                </FormGroup>
            </Form>
        </Modal>
    )
}

TextInputModal.propTypes = {
    title: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    isOpen: PropTypes.bool.isRequired,
    callback: PropTypes.func.isRequired,
    initialValue: PropTypes.string,
}

export default TextInputModal
