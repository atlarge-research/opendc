import PropTypes from 'prop-types'
import React from 'react'
import { ListGroup, ListGroupItem } from 'reactstrap'
import ContentSection from './ContentSection'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faWindowMaximize, faTv, faDatabase, faCogs } from '@fortawesome/free-solid-svg-icons'

const TechnologiesSection = ({ className }) => (
    <ContentSection name="technologies" title="Technologies" className={className}>
        <ListGroup className="list-group text-left">
            <ListGroupItem color="primary" className="d-flex justify-content-between align-items-center">
                <span style={{ minWidth: 100 }}>
                    <FontAwesomeIcon icon={faWindowMaximize} className="mr-2" />
                    <strong className="">Browser</strong>
                </span>
                <span className="text-right">JavaScript, React, Redux, Konva</span>
            </ListGroupItem>
            <ListGroupItem color="warning" className="d-flex justify-content-between align-items-center">
                <span style={{ minWidth: 100 }}>
                    <FontAwesomeIcon icon={faTv} className="mr-2" />
                    <strong>Server</strong>
                </span>
                <span className="text-right">Python, Flask, FlaskSocketIO, OpenAPI</span>
            </ListGroupItem>
            <ListGroupItem color="success" className="d-flex justify-content-between align-items-center">
                <span style={{ minWidth: 100 }}>
                    <FontAwesomeIcon icon={faDatabase} className="mr-2" />
                    <strong>Database</strong>
                </span>
                <span className="text-right">MongoDB</span>
            </ListGroupItem>
            <ListGroupItem color="danger" className="d-flex justify-content-between align-items-center">
                <span style={{ minWidth: 100 }}>
                    <FontAwesomeIcon icon={faCogs} className="mr-2" />
                    <strong>Simulator</strong>
                </span>
                <span className="text-right">Kotlin</span>
            </ListGroupItem>
        </ListGroup>
    </ContentSection>
)

TechnologiesSection.propTypes = {
    className: PropTypes.string,
}

export default TechnologiesSection
