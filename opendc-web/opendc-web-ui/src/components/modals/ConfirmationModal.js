import PropTypes from 'prop-types'
import React from 'react'
import Modal from './Modal'

class ConfirmationModal extends React.Component {
    static propTypes = {
        title: PropTypes.string.isRequired,
        message: PropTypes.string.isRequired,
        show: PropTypes.bool.isRequired,
        callback: PropTypes.func.isRequired,
    }

    onConfirm() {
        this.props.callback(true)
    }

    onCancel() {
        this.props.callback(false)
    }

    render() {
        return (
            <Modal
                title={this.props.title}
                show={this.props.show}
                onSubmit={this.onConfirm.bind(this)}
                onCancel={this.onCancel.bind(this)}
                submitButtonType="danger"
                submitButtonText="Confirm"
            >
                {this.props.message}
            </Modal>
        )
    }
}

export default ConfirmationModal
