import PropTypes from "prop-types";
import React from "react";

class Modal extends React.Component {
    static propTypes = {
        title: PropTypes.string.isRequired,
        show: PropTypes.bool.isRequired,
        onSubmit: PropTypes.func.isRequired,
        onCancel: PropTypes.func.isRequired,
    };
    static idCounter = 0;

    constructor() {
        super();
        this.id = "modal-" + Modal.idCounter;
    }

    componentDidMount() {
        this.openOrCloseModal();
    }

    componentDidUpdate() {
        this.openOrCloseModal();
    }

    onSubmit() {
        this.props.onSubmit();
        this.closeModal();
    }

    onCancel() {
        this.props.onCancel();
        this.closeModal();
    }

    openModal() {
        window["$"]("#" + this.id).modal("show");
    }

    closeModal() {
        window["$"]("#" + this.id).modal("hide");
    }

    openOrCloseModal() {
        if (this.props.show) {
            this.openModal();
        } else {
            this.closeModal();
        }
    }

    render() {
        return (
            <div className="modal" id={this.id} role="dialog">
                <div className="modal-dialog" role="document">
                    <div className="modal-content">
                        <div className="modal-header">
                            <h5 className="modal-title">{this.props.title}</h5>
                            <button type="button" className="close" onClick={this.onCancel.bind(this)}
                                    aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div className="modal-body">
                            {this.props.children}
                        </div>
                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={this.onCancel.bind(this)}>
                                Close
                            </button>
                            <button type="button" className="btn btn-primary" onClick={this.onSubmit.bind(this)}>
                                Save
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

export default Modal;
