import PropTypes from 'prop-types'
import React from 'react'
import Shapes from '../../../shapes'
import Modal from '../Modal'

class NewScenarioModalComponent extends React.Component {
    static propTypes = {
        show: PropTypes.bool.isRequired,
        currentPortfolioId: PropTypes.string.isRequired,
        currentPortfolioScenarioIds: PropTypes.arrayOf(PropTypes.string),
        traces: PropTypes.arrayOf(Shapes.Trace),
        topologies: PropTypes.arrayOf(Shapes.Topology),
        schedulers: PropTypes.arrayOf(Shapes.Scheduler),
        callback: PropTypes.func.isRequired,
    }

    componentDidMount() {
        this.reset()
    }

    componentDidUpdate() {
        if (this.textInput) {
            if (this.props.currentPortfolioScenarioIds.length === 0) {
                this.textInput.value = 'Base scenario'
            } else if (this.textInput.value === 'Base scenario') {
                this.textInput.value = ''
            }
        }
    }

    reset() {
        if (this.textInput) {
            this.textInput.value = this.props.currentPortfolioScenarioIds.length === 0 ? 'Base scenario' : ''
            this.traceSelect.selectedIndex = 0
            this.traceLoadInput.value = 1.0
            this.topologySelect.selectedIndex = 0
            this.failuresCheckbox.checked = false
            this.performanceInterferenceCheckbox.checked = false
            this.schedulerSelect.selectedIndex = 0
        }
    }

    onSubmit() {
        this.props.callback(
            this.textInput.value,
            this.props.currentPortfolioId,
            {
                traceId: this.traceSelect.value,
                loadSamplingFraction: parseFloat(this.traceLoadInput.value),
            },
            {
                topologyId: this.topologySelect.value,
            },
            {
                failuresEnabled: this.failuresCheckbox.checked,
                performanceInterferenceEnabled: this.performanceInterferenceCheckbox.checked,
                schedulerName: this.schedulerSelect.value,
            }
        )
        this.reset()
    }

    onCancel() {
        this.props.callback(undefined)
        this.reset()
    }

    render() {
        return (
            <Modal
                title="New Scenario"
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
                            disabled={this.props.currentPortfolioScenarioIds.length === 0}
                            ref={(textInput) => (this.textInput = textInput)}
                        />
                    </div>
                    <h4>Trace</h4>
                    <div className="form-group">
                        <label className="form-control-label">Trace</label>
                        <select className="form-control" ref={(traceSelect) => (this.traceSelect = traceSelect)}>
                            {this.props.traces.map((trace) => (
                                <option value={trace._id} key={trace._id}>
                                    {trace.name}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label className="form-control-label">Load sampling fraction</label>
                        <input
                            type="number"
                            className="form-control"
                            required
                            ref={(traceLoadInput) => (this.traceLoadInput = traceLoadInput)}
                        />
                    </div>
                    <h4>Topology</h4>
                    <div className="form-group">
                        <label className="form-control-label">Topology</label>
                        <select
                            className="form-control"
                            ref={(topologySelect) => (this.topologySelect = topologySelect)}
                        >
                            {this.props.topologies.map((topology) => (
                                <option value={topology._id} key={topology._id}>
                                    {topology.name}
                                </option>
                            ))}
                        </select>
                    </div>
                    <h4>Operational Phenomena</h4>
                    <div className="form-check">
                        <label className="form-check-label">
                            <input
                                type="checkbox"
                                className="form-check-input"
                                ref={(failuresCheckbox) => (this.failuresCheckbox = failuresCheckbox)}
                            />
                            <span className="ml-2">Enable failures</span>
                        </label>
                    </div>
                    <div className="form-check">
                        <label className="form-check-label">
                            <input
                                type="checkbox"
                                className="form-check-input"
                                ref={(performanceInterferenceCheckbox) =>
                                    (this.performanceInterferenceCheckbox = performanceInterferenceCheckbox)
                                }
                            />
                            <span className="ml-2">Enable performance interference</span>
                        </label>
                    </div>
                    <div className="form-group">
                        <label className="form-control-label">Scheduler</label>
                        <select
                            className="form-control"
                            ref={(schedulerSelect) => (this.schedulerSelect = schedulerSelect)}
                        >
                            {this.props.schedulers.map((scheduler) => (
                                <option value={scheduler.name} key={scheduler.name}>
                                    {scheduler.name}
                                </option>
                            ))}
                        </select>
                    </div>
                </form>
            </Modal>
        )
    }
}

export default NewScenarioModalComponent
