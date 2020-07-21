import PropTypes from 'prop-types'
import React from 'react'
import Shapes from '../../../../shapes'
import FontAwesome from 'react-fontawesome'

class TopologyListComponent extends React.Component {
    static propTypes = {
        topologies: PropTypes.arrayOf(Shapes.Topology),
        currentTopologyId: PropTypes.string,
        onChooseTopology: PropTypes.func.isRequired,
        onNewTopology: PropTypes.func.isRequired,
        onDeleteTopology: PropTypes.func.isRequired,
    }

    onChoose(id) {
        this.props.onChooseTopology(id)
    }

    onDelete(id) {
        this.props.onDeleteTopology(id)
    }

    render() {
        return (
            <div className="pb-3">
                <h2>
                    Topologies
                    <button className="btn btn-outline-primary float-right" onClick={this.props.onNewTopology}>
                        <FontAwesome name="plus" />
                    </button>
                </h2>

                {this.props.topologies.map((topology, idx) => (
                    <div key={topology._id} className="row mb-1">
                        <div
                            className={
                                'col-8 align-self-center ' +
                                (topology._id === this.props.currentTopologyId ? 'font-weight-bold' : '')
                            }
                        >
                            {topology.name}
                        </div>
                        <div className="col-4 text-right">
                            <span
                                className="btn btn-outline-primary mr-1 fa fa-play"
                                onClick={() => this.onChoose(topology._id)}
                            />
                            <span
                                className={'btn btn-outline-danger fa fa-trash ' + (idx === 0 ? 'disabled' : '')}
                                onClick={() => (idx !== 0 ? this.onDelete(topology._id) : undefined)}
                            />
                        </div>
                    </div>
                ))}
            </div>
        )
    }
}

export default TopologyListComponent
