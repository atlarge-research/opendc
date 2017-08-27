import React from "react";
import FontAwesome from "react-fontawesome";
import ContentSection from "./ContentSection";

const TechnologiesSection = () => (
    <ContentSection name="technologies" title="Technologies">
        <ul className="list-group text-left">
            <li className="list-group-item list-group-item-primary">
                <FontAwesome name="window-maximize"/> &nbsp;
                <strong>Browser</strong> &nbsp;
                JavaScript, React, Redux, HTML5 Canvas
            </li>
            <li className="list-group-item list-group-item-warning">
                <FontAwesome name="television"/> &nbsp;
                <strong>Server</strong> &nbsp;
                Python, Flask, FlaskSocketIO, OpenAPI
            </li>
            <li className="list-group-item list-group-item-success">
                <FontAwesome name="database"/> &nbsp;
                <strong>Database</strong> &nbsp;
                MariaDB
            </li>
            <li className="list-group-item list-group-item-danger">
                <FontAwesome name="cogs"/> &nbsp;
                <strong>Simulator</strong> &nbsp;
                Kotlin
            </li>
        </ul>
    </ContentSection>
);

export default TechnologiesSection;
