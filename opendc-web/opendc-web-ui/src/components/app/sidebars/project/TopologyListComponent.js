import PropTypes from 'prop-types'
import React from 'react'
import { Topology } from '../../../../shapes'
import FontAwesome from 'react-fontawesome'
import { Button, Col, Row } from 'reactstrap'
import classNames from 'classnames'

function TopologyListComponent({ topologies, currentTopologyId, onChooseTopology, onNewTopology, onDeleteTopology }) {
    return (
        <div className="pb-3">
            <h2>
                Topologies
                <Button color="primary" outline className="float-right" onClick={onNewTopology}>
                    <FontAwesome name="plus" />
                </Button>
            </h2>

            {topologies.map((topology, idx) => (
                <Row key={topology._id} className="mb-1">
                    <Col
                        xs="7"
                        className={classNames('align-self-center', {
                            'font-weight-bold': topology._id === currentTopologyId,
                        })}
                    >
                        {topology.name}
                    </Col>
                    <Col xs="5" className="text-right">
                        <Button
                            color="primary"
                            outline
                            className="mr-1 fa fa-play"
                            onClick={() => onChooseTopology(topology._id)}
                        />
                        <Button
                            color="danger"
                            outline
                            className="fa fa-trash"
                            disable={idx === 0}
                            onClick={() => (idx !== 0 ? onDeleteTopology(topology._id) : undefined)}
                        />
                    </Col>
                </Row>
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
