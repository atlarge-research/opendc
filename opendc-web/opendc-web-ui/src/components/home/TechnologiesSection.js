import React from 'react'
import FontAwesome from 'react-fontawesome'
import { ListGroup, ListGroupItem } from 'reactstrap'
import ContentSection from './ContentSection'

const TechnologiesSection = ({ className }) => (
    <ContentSection name="technologies" title="Technologies" className={className}>
        <ListGroup className="list-group text-left">
            <ListGroupItem color="primary" className="d-flex justify-content-between align-items-center">
                <span style={{ minWidth: 100 }}>
                    <FontAwesome name="window-maximize" className="mr-2" />
                    <strong className="">Browser</strong>
                </span>
                <span className="text-right">JavaScript, React, Redux, Konva</span>
            </ListGroupItem>
            <ListGroupItem color="warning" className="d-flex justify-content-between align-items-center">
                <span style={{ minWidth: 100 }}>
                    <FontAwesome name="television" className="mr-2" />
                    <strong>Server</strong>
                </span>
                <span className="text-right">Python, Flask, FlaskSocketIO, OpenAPI</span>
            </ListGroupItem>
            <ListGroupItem color="success" className="d-flex justify-content-between align-items-center">
                <span style={{ minWidth: 100 }}>
                    <FontAwesome name="database" className="mr-2" />
                    <strong>Database</strong>
                </span>
                <span className="text-right">MongoDB</span>
            </ListGroupItem>
            <ListGroupItem color="danger" className="d-flex justify-content-between align-items-center">
                <span style={{ minWidth: 100 }}>
                    <FontAwesome name="cogs" className="mr-2" />
                    <strong>Simulator</strong>
                </span>
                <span className="text-right">Kotlin</span>
            </ListGroupItem>
        </ListGroup>
    </ContentSection>
)

export default TechnologiesSection
