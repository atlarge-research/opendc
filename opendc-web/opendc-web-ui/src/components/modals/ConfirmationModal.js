import PropTypes from 'prop-types'
import React from 'react'
import Modal from './Modal'

function ConfirmationModal({ title, message, show, callback }) {
    return (
        <Modal
            title={title}
            show={show}
            onSubmit={() => callback(true)}
            onCancel={() => callback(false)}
            submitButtonType="danger"
            submitButtonText="Confirm"
        >
            {message}
        </Modal>
    )
}

ConfirmationModal.propTypes = {
    title: PropTypes.string.isRequired,
    message: PropTypes.string.isRequired,
    show: PropTypes.bool.isRequired,
    callback: PropTypes.func.isRequired,
}

export default ConfirmationModal
