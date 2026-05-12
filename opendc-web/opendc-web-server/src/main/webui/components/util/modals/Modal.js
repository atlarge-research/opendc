import React from 'react'
import PropTypes from 'prop-types'
import { Button, Modal as PModal, ModalVariant } from '@patternfly/react-core'

function Modal({ children, title, isOpen, onSubmit, onCancel, submitButtonType, submitButtonText, ouiaId }) {
    const actions = [
        <Button variant={submitButtonType} onClick={onSubmit} key="confirm" ouiaId="modal-submit">
            {submitButtonText}
        </Button>,
        <Button variant="link" onClick={onCancel} key="cancel" ouiaId="modal-cancel">
            Cancel
        </Button>,
    ]

    return (
        <PModal variant={ModalVariant.small} isOpen={isOpen} onClose={onCancel} title={title} actions={actions} ouiaId={ouiaId}>
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
    ouiaId: PropTypes.string,
    children: PropTypes.node,
}

Modal.defaultProps = {
    submitButtonType: 'primary',
    submitButtonText: 'Save',
    isOpen: false,
}

export default Modal
