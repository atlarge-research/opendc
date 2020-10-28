import PropTypes from 'prop-types'
import React from 'react'
import Modal from '../Modal'
import { AVAILABLE_METRICS } from '../../../util/available-metrics'

class NewPortfolioModalComponent extends React.Component {
    static propTypes = {
        show: PropTypes.bool.isRequired,
        callback: PropTypes.func.isRequired,
    }

    constructor(props) {
        super(props)
        this.metricCheckboxes = {}
    }

    componentDidMount() {
        this.reset()
    }

    reset() {
        if (this.textInput) {
            this.textInput.value = ''
            AVAILABLE_METRICS.forEach((metric) => {
                this.metricCheckboxes[metric].checked = true
            })
            this.repeatsInput.value = 1
        }
    }

    onSubmit() {
        this.props.callback(this.textInput.value, {
            enabledMetrics: AVAILABLE_METRICS.filter((metric) => this.metricCheckboxes[metric].checked),
            repeatsPerScenario: parseInt(this.repeatsInput.value),
        })
        this.reset()
    }

    onCancel() {
        this.props.callback(undefined)
        this.reset()
    }

    render() {
        return (
            <Modal
                title="New Portfolio"
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
                        <label className="form-control-label">Name</label>
                        <input
                            type="text"
                            className="form-control"
                            required
                            ref={(textInput) => (this.textInput = textInput)}
                        />
                    </div>
                    <h4>Targets</h4>
                    <h5>Metrics</h5>
                    <div className="form-group">
                        {AVAILABLE_METRICS.map((metric) => (
                            <div className="form-check" key={metric}>
                                <label className="form-check-label">
                                    <input
                                        type="checkbox"
                                        className="form-check-input"
                                        ref={(checkbox) => (this.metricCheckboxes[metric] = checkbox)}
                                    />
                                    <code>{metric}</code>
                                </label>
                            </div>
                        ))}
                    </div>
                    <div className="form-group">
                        <label className="form-control-label">Repeats per scenario</label>
                        <input
                            type="number"
                            className="form-control"
                            required
                            ref={(repeatsInput) => (this.repeatsInput = repeatsInput)}
                        />
                    </div>
                </form>
            </Modal>
        )
    }
}

export default NewPortfolioModalComponent
