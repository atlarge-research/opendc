import PropTypes from 'prop-types'
import React from 'react'
import Shapes from '../../../shapes'
import Modal from '../Modal'

class ChangeTopologyModalComponent extends React.Component {
    static propTypes = {
        show: PropTypes.bool.isRequired,
        topologies: PropTypes.arrayOf(Shapes.Topology),
        currentTopologyId: PropTypes.string,
        onChooseTopology: PropTypes.func.isRequired,
        onCreateTopology: PropTypes.func.isRequired,
        onDuplicateTopology: PropTypes.func.isRequired,
        onDeleteTopology: PropTypes.func.isRequired,
        onCancel: PropTypes.func.isRequired,
    }

    reset() {
        this.textInput.value = ''
        this.originTopology.selectedIndex = 0
    }

    onSubmit() {
        if (this.originTopology.selectedIndex === 0) {
            this.onCreate()
        } else {
            this.onDuplicate()
        }
    }

    onChoose(id) {
        this.props.onChooseTopology(id)
        this.reset()
    }

    onCreate() {
        this.props.onCreateTopology(this.textInput.value)
        this.reset()
    }

    onDuplicate() {
        this.props.onCreateTopology(
            this.textInput.value,
            this.originTopology.value,
        )
        this.reset()
    }

    onDelete(id) {
        this.props.onDeleteTopology(id)
        this.reset()
    }

    onCancel() {
        this.props.onCancel()
        this.reset()
    }

    render() {
        return (
            <Modal
                title="Change Topology"
                show={this.props.show}
                onSubmit={this.onSubmit.bind(this)}
                onCancel={this.onCancel.bind(this)}
            >
                <div>
                    {this.props.topologies.map((topology, idx) => (
                        <div key={topology._id} className="row mb-1">
                            <div className="col-6">
                                <em>{topology._id === this.props.currentTopologyId ? 'Active: ' : ''}</em>
                                {topology.name}
                            </div>
                            <div className="col-6 text-right">
                                <span
                                    className="btn btn-primary mr-1"
                                    onClick={() => this.onChoose(topology._id)}
                                >
                                    Choose
                                </span>
                                <span
                                    className={'btn btn-danger ' + (idx === 0 ? 'disabled' : '')}
                                    onClick={() => idx !== 0 ? this.onDelete(topology._id) : undefined}
                                >
                                    Delete
                                </span>
                            </div>
                        </div>
                    ))}
                </div>

                <h5 className="pt-3 pt-1">New Topology</h5>
                <form
                    onSubmit={e => {
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
                            ref={textInput => (this.textInput = textInput)}
                        />
                    </div>
                    <div className="form-group">
                        <label className="form-control-label">Topology to duplicate</label>
                        <select
                            className="form-control"
                            ref={originTopology => (this.originTopology = originTopology)}
                        >
                            <option value={-1} key={-1}>
                                None - start from scratch
                            </option>
                            {this.props.topologies.map(topology => (
                                <option value={topology._id} key={topology._id}>
                                    {topology.name}
                                </option>
                            ))}
                        </select>
                    </div>
                </form>
            </Modal>
        )
    }
}

export default ChangeTopologyModalComponent
