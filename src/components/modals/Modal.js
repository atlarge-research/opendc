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

    /**
     * Local, up-to-date copy of modal visibility for time between close event and a props update (to prevent duplicate
     * close triggers).
     */
    visible = false;

    constructor() {
        super();
        this.id = "modal-" + Modal.idCounter++;
    }

    componentDidMount() {
        this.visible = this.props.show;
        this.openOrCloseModal();

        // Trigger auto-focus
        window["$"]("#" + this.id).on("shown.bs.modal", function () {
            window["$"](this).find("input").first().focus();
        });

        window["$"]("#" + this.id).on("hide.bs.modal", () => {
            if (this.visible) {
                this.props.onCancel();
            }
        });
    }

    componentDidUpdate() {
        this.visible = this.props.show;
        this.openOrCloseModal();
    }

    onSubmit() {
        if (this.visible) {
            this.props.onSubmit();
            this.visible = false;
            this.closeModal();
        }
    }

    onCancel() {
        if (this.visible) {
            this.props.onCancel();
            this.visible = false;
            this.closeModal();
        }
    }

    openModal() {
        window["$"]("#" + this.id).modal("show");
    }

    closeModal() {
        window["$"]("#" + this.id).modal("hide");
    }

    openOrCloseModal() {
        if (this.visible) {
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
