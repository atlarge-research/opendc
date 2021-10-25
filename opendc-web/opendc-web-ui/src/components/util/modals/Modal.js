import React from 'react'
import PropTypes from 'prop-types'
import { Button, Modal as PModal, ModalVariant } from '@patternfly/react-core'

function Modal({ children, title, isOpen, onSubmit, onCancel, submitButtonType, submitButtonText }) {
    const actions = [
        <Button variant={submitButtonType} onClick={onSubmit} key="confirm">
            {submitButtonText}
        </Button>,
        <Button variant="link" onClick={onCancel} key="cancel">
            Cancel
        </Button>,
    ]

    return (
        <PModal variant={ModalVariant.small} isOpen={isOpen} onClose={onCancel} title={title} actions={actions}>
            {children}
        </PModal>
    )
}

Modal.propTypes = {
    title: PropTypes.string.isRequired,
    isOpen: PropTypes.bool,
    onSubmit: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    submitButtonType: PropTypes.string,
    submitButtonText: PropTypes.string,
    children: PropTypes.node,
}

Modal.defaultProps = {
    submitButtonType: 'primary',
    submitButtonText: 'Save',
    isOpen: false,
}

export default Modal
