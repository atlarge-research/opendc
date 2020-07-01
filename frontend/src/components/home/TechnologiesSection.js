import React from 'react'
import FontAwesome from 'react-fontawesome'
import ContentSection from './ContentSection'

const TechnologiesSection = () => (
    <ContentSection name="technologies" title="Technologies">
        <ul className="list-group text-left">
            <li className="d-flex list-group-item justify-content-between align-items-center list-group-item-primary">
        <span style={{ minWidth: 100 }}>
          <FontAwesome name="window-maximize" className="mr-2"/>
          <strong className="">Browser</strong>
        </span>
                <span className="text-right">JavaScript, React, Redux, Konva</span>
            </li>
            <li className="d-flex list-group-item justify-content-between align-items-center list-group-item-warning">
        <span style={{ minWidth: 100 }}>
          <FontAwesome name="television" className="mr-2"/>
          <strong>Server</strong>
        </span>
                <span className="text-right">
          Python, Flask, FlaskSocketIO, OpenAPI
        </span>
            </li>
            <li className="d-flex list-group-item justify-content-between align-items-center list-group-item-success">
        <span style={{ minWidth: 100 }}>
          <FontAwesome name="database" className="mr-2"/>
          <strong>Database</strong>
        </span>
                <span className="text-right">MariaDB</span>
            </li>
            <li className="d-flex list-group-item justify-content-between align-items-center list-group-item-danger">
        <span style={{ minWidth: 100 }}>
          <FontAwesome name="cogs" className="mr-2"/>
          <strong>Simulator</strong>
        </span>
                <span className="text-right">Kotlin</span>
            </li>
        </ul>
    </ContentSection>
)

export default TechnologiesSection
