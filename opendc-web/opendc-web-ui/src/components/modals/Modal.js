import React, { useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import { Modal as RModal, ModalHeader, ModalBody, ModalFooter, Button } from 'reactstrap'

function Modal({ children, title, show, onSubmit, onCancel, submitButtonType, submitButtonText }) {
    const [modal, setModal] = useState(show)

    useEffect(() => setModal(show), [show])

    const toggle = () => setModal(!modal)
    const cancel = () => {
        if (onCancel() !== false) {
            toggle()
        }
    }
    const submit = () => {
        if (onSubmit() !== false) {
            toggle()
        }
    }

    return (
        <RModal isOpen={modal} toggle={cancel}>
            <ModalHeader toggle={cancel}>{title}</ModalHeader>
            <ModalBody>{children}</ModalBody>
            <ModalFooter>
                <Button color="secondary" onClick={cancel}>
                    Close
                </Button>
                <Button color={submitButtonType} onClick={submit}>
                    {submitButtonText}
                </Button>
            </ModalFooter>
        </RModal>
    )
}

Modal.propTypes = {
    title: PropTypes.string.isRequired,
    show: PropTypes.bool.isRequired,
    onSubmit: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    submitButtonType: PropTypes.string,
    submitButtonText: PropTypes.string,
    children: PropTypes.node,
}

Modal.defaultProps = {
    submitButtonType: 'primary',
    submitButtonText: 'Save',
    show: false,
}

export default Modal
