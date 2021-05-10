import React from 'react'
import { Row, Col } from 'reactstrap'
import ContentSection from './ContentSection'

const Stakeholder = ({ name, title, subtitle }) => (
    <Col xl="4" lg="4" md="4" sm="6">
        <Col
            tag="img"
            xl="3"
            lg="3"
            md="4"
            sm="4"
            src={'img/stakeholders/' + name + '.png'}
            className="img-fluid"
            alt={title}
        />
        <div className="text-center mt-2">
            <h4>{title}</h4>
            <p>{subtitle}</p>
        </div>
    </Col>
)

const StakeholderSection = () => (
    <ContentSection name="stakeholders" title="Stakeholders">
        <Row className="justify-content-center">
            <Stakeholder name="Manager" title="Managers" subtitle="Seeing is deciding" />
            <Stakeholder name="Sales" title="Sales" subtitle="Demo concepts" />
            <Stakeholder name="Developer" title="DevOps" subtitle="Develop & tune" />
            <Stakeholder name="Researcher" title="Researchers" subtitle="Understand & design" />
            <Stakeholder name="Student" title="Students" subtitle="Grasp complex concepts" />
        </Row>
    </ContentSection>
)

export default StakeholderSection
