import PropTypes from "prop-types";
import React from "react";
import Shapes from "../../../shapes";
import Modal from "../Modal";

class NewExperimentModalComponent extends React.Component {
    static propTypes = {
        show: PropTypes.bool.isRequired,
        paths: PropTypes.arrayOf(Shapes.Path),
        schedulers: PropTypes.arrayOf(Shapes.Scheduler),
        traces: PropTypes.arrayOf(Shapes.Trace),
        callback: PropTypes.func.isRequired,
    };

    reset() {
        this.textInput.value = "";
        this.pathSelect.selectedIndex = 0;
        this.traceSelect.selectedIndex = 0;
        this.schedulerSelect.selectedIndex = 0;
    }

    onSubmit() {
        this.props.callback(
            this.textInput.value,
            parseInt(this.pathSelect.value, 10),
            parseInt(this.traceSelect.value, 10),
            this.schedulerSelect.value
        );
        this.reset();
    }

    onCancel() {
        this.props.callback(undefined);
        this.reset();
    }

    render() {
        return (
            <Modal title="New Experiment"
                   show={this.props.show}
                   onSubmit={this.onSubmit.bind(this)}
                   onCancel={this.onCancel.bind(this)}>
                <form onSubmit={e => {
                    e.preventDefault();
                    this.onSubmit();
                }}>
                    <div className="form-group">
                        <label className="form-control-label">Name</label>
                        <input type="text" className="form-control"
                               ref={textInput => this.textInput = textInput}/>
                    </div>
                    <div className="form-group">
                        <label className="form-control-label">Path</label>
                        <select className="form-control"
                                ref={pathSelect => this.pathSelect = pathSelect}>
                            {this.props.paths.map(path => (
                                <option value={path.id} key={path.id}>
                                    {path.name ? path.name : "Path " + path.id}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label className="form-control-label">Trace</label>
                        <select className="form-control"
                                ref={traceSelect => this.traceSelect = traceSelect}>
                            {this.props.traces.map(trace => (
                                <option value={trace.id} key={trace.id}>
                                    {trace.name}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="form-group">
                        <label className="form-control-label">Scheduler</label>
                        <select className="form-control"
                                ref={schedulerSelect => this.schedulerSelect = schedulerSelect}>
                            {this.props.schedulers.map(scheduler => (
                                <option value={scheduler.name} key={scheduler.name}>
                                    {scheduler.name}
                                </option>
                            ))}
                        </select>
                    </div>
                </form>
            </Modal>
        );
    }
}

export default NewExperimentModalComponent;
