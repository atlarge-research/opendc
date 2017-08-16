import PropTypes from "prop-types";
import React from "react";
import Modal from "./Modal";

class TextInputModal extends React.Component {
    static propTypes = {
        title: PropTypes.string.isRequired,
        label: PropTypes.string.isRequired,
        show: PropTypes.bool.isRequired,
        callback: PropTypes.func.isRequired,
        initialValue: PropTypes.string,
    };

    onSubmit() {
        this.props.callback(this.refs.textInput.value);
        this.refs.textInput.value = "";
    }

    onCancel() {
        this.props.callback(undefined);
        this.refs.textInput.value = "";
    }

    render() {
        return (
            <Modal title={this.props.title}
                   show={this.props.show}
                   onSubmit={this.onSubmit.bind(this)}
                   onCancel={this.onCancel.bind(this)}>
                <form onSubmit={e => {
                    e.preventDefault();
                    this.onSubmit();
                }}>
                    <div className="form-group">
                        <label className="form-control-label">{this.props.label}:</label>
                        <input type="text" className="form-control" ref="textInput" value={this.props.initialValue}
                               autoFocus/>
                    </div>
                </form>
            </Modal>
        );
    }
}

export default TextInputModal;
