import classNames from 'classnames'
import PropTypes from 'prop-types'
import React from 'react'
import jQuery from '../../util/jquery'

class Modal extends React.Component {
    static propTypes = {
        title: PropTypes.string.isRequired,
        show: PropTypes.bool.isRequired,
        onSubmit: PropTypes.func.isRequired,
        onCancel: PropTypes.func.isRequired,
        submitButtonType: PropTypes.string,
        submitButtonText: PropTypes.string,
    }
    static defaultProps = {
        submitButtonType: 'primary',
        submitButtonText: 'Save',
    }
    static idCounter = 0

    // Local, up-to-date copy of modal visibility for time between close event and a props update (to prevent duplicate
    // 'close' triggers)
    visible = false

    constructor(props) {
        super(props)
        this.id = 'modal-' + Modal.idCounter++
    }

    componentDidMount() {
        this.visible = this.props.show
        this.openOrCloseModal()

        // Trigger auto-focus
        jQuery('#' + this.id)
            .on('shown.bs.modal', function () {
                jQuery(this).find('input').first().focus()
            })
            .on('hide.bs.modal', () => {
                if (this.visible) {
                    this.props.onCancel()
                }
            })
            .on('keydown', (e) => {
                e.stopPropagation()
            })
    }

    componentDidUpdate() {
        if (this.visible !== this.props.show) {
            this.visible = this.props.show
            this.openOrCloseModal()
        }
    }

    onSubmit() {
        if (this.visible) {
            this.props.onSubmit()
            this.visible = false
            this.closeModal()
        }
    }

    onCancel() {
        if (this.visible) {
            this.props.onCancel()
            this.visible = false
            this.closeModal()
        }
    }

    openModal() {
        jQuery('#' + this.id).modal('show')
    }

    closeModal() {
        jQuery('#' + this.id).modal('hide')
    }

    openOrCloseModal() {
        if (this.visible) {
            this.openModal()
        } else {
            this.closeModal()
        }
    }

    render() {
        return (
            <div className="modal fade" id={this.id} role="dialog">
                <div className="modal-dialog" role="document">
                    <div className="modal-content">
                        <div className="modal-header">
                            <h5 className="modal-title">{this.props.title}</h5>
                            <button
                                type="button"
                                className="close"
                                onClick={this.onCancel.bind(this)}
                                aria-label="Close"
                            >
                                <span>&times;</span>
                            </button>
                        </div>
                        <div className="modal-body">{this.props.children}</div>
                        <div className="modal-footer">
                            <button type="button" className="btn btn-secondary" onClick={this.onCancel.bind(this)}>
                                Close
                            </button>
                            <button
                                type="button"
                                className={classNames('btn', 'btn-' + this.props.submitButtonType)}
                                onClick={this.onSubmit.bind(this)}
                            >
                                {this.props.submitButtonText}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}

export default Modal
