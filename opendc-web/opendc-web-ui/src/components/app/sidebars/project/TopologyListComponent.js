import PropTypes from 'prop-types'
import React from 'react'
import { Topology } from '../../../../shapes'
import FontAwesome from 'react-fontawesome'

function TopologyListComponent({ topologies, currentTopologyId, onChooseTopology, onNewTopology, onDeleteTopology }) {
    return (
        <div className="pb-3">
            <h2>
                Topologies
                <button className="btn btn-outline-primary float-right" onClick={onNewTopology}>
                    <FontAwesome name="plus" />
                </button>
            </h2>

            {topologies.map((topology, idx) => (
                <div key={topology._id} className="row mb-1">
                    <div
                        className={
                            'col-7 align-self-center ' + (topology._id === currentTopologyId ? 'font-weight-bold' : '')
                        }
                    >
                        {topology.name}
                    </div>
                    <div className="col-5 text-right">
                        <span
                            className="btn btn-outline-primary mr-1 fa fa-play"
                            onClick={() => onChooseTopology(topology._id)}
                        />
                        <span
                            className={'btn btn-outline-danger fa fa-trash ' + (idx === 0 ? 'disabled' : '')}
                            onClick={() => (idx !== 0 ? onDeleteTopology(topology._id) : undefined)}
                        />
                    </div>
                </div>
            ))}
        </div>
    )
}

TopologyListComponent.propTypes = {
    topologies: PropTypes.arrayOf(Topology),
    currentTopologyId: PropTypes.string,
    onChooseTopology: PropTypes.func.isRequired,
    onNewTopology: PropTypes.func.isRequired,
    onDeleteTopology: PropTypes.func.isRequired,
}

export default TopologyListComponent
