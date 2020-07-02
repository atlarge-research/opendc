import PropTypes from 'prop-types'
import React from 'react'
import Shapes from '../../../shapes'
import Modal from '../Modal'

class ChangeTopologyModalComponent extends React.Component {
    static propTypes = {
        show: PropTypes.bool.isRequired,
        topologies: PropTypes.arrayOf(Shapes.Topology),
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
                    {this.props.topologies.forEach(topology => (
                        <div key={topology._id}>
                            {topology.name}
                            <div
                                className="btn btn-danger"
                                onClick={() => this.onDelete(topology._id)}
                            >
                                Delete
                            </div>
                        </div>
                    ))}
                </div>

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
