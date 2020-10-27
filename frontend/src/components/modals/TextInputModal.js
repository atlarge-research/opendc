import PropTypes from 'prop-types'
import React from 'react'
import Modal from './Modal'

class TextInputModal extends React.Component {
    static propTypes = {
        title: PropTypes.string.isRequired,
        label: PropTypes.string.isRequired,
        show: PropTypes.bool.isRequired,
        callback: PropTypes.func.isRequired,
        initialValue: PropTypes.string,
    }

    componentDidUpdate() {
        if (this.props.initialValue && this.textInput) {
            this.textInput.value = this.props.initialValue
        }
    }

    onSubmit() {
        this.props.callback(this.textInput.value)
        this.textInput.value = ''
    }

    onCancel() {
        this.props.callback(undefined)
        this.textInput.value = ''
    }

    render() {
        return (
            <Modal
                title={this.props.title}
                show={this.props.show}
                onSubmit={this.onSubmit.bind(this)}
                onCancel={this.onCancel.bind(this)}
            >
                <form
                    onSubmit={(e) => {
                        e.preventDefault()
                        this.onSubmit()
                    }}
                >
                    <div className="form-group">
                        <label className="form-control-label">{this.props.label}</label>
                        <input type="text" className="form-control" ref={(textInput) => (this.textInput = textInput)} />
                    </div>
                </form>
            </Modal>
        )
    }
}

export default TextInputModal
