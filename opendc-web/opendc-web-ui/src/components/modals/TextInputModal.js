import PropTypes from 'prop-types'
import React, { useRef } from 'react'
import Modal from './Modal'

function TextInputModal({ title, label, show, callback, initialValue }) {
    const textInput = useRef(null)
    const onSubmit = () => {
        callback(textInput.current.value)
        textInput.current.value = ''
    }
    const onCancel = () => {
        callback(undefined)
        textInput.current.value = ''
    }

    return (
        <Modal title={title} show={show} onSubmit={onSubmit} onCancel={onCancel}>
            <form
                onSubmit={(e) => {
                    e.preventDefault()
                    onSubmit()
                }}
            >
                <div className="form-group">
                    <label className="form-control-label">{label}</label>
                    <input type="text" className="form-control" ref={textInput} defaultValue={initialValue} />
                </div>
            </form>
        </Modal>
    )
}

TextInputModal.propTypes = {
    title: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    show: PropTypes.bool.isRequired,
    callback: PropTypes.func.isRequired,
    initialValue: PropTypes.string,
}

export default TextInputModal
