import React from 'react'
import { Row, Col } from 'reactstrap'
import ContentSection from './ContentSection'

const TeamLead = ({ photoId, name, description }) => (
    <Col xl="3" lg="3" md="4" sm="6" className="justify-content-center">
        <Col
            tag="img"
            src={'img/portraits/' + photoId + '.png'}
            xl="10"
            lg="10"
            md="10"
            sm="8"
            col="5"
            className="mb-2 mt-2"
            alt={name}
        />
        <Col>
            <h4>{name}</h4>
            <div className="team-member-description">{description}</div>
        </Col>
    </Col>
)

const TeamMember = ({ photoId, name }) => (
    <Col xl="2" lg="2" md="3" sm="4" className="justify-content-center">
        <Col
            tag="img"
            src={'img/portraits/' + photoId + '.png'}
            xl="10"
            lg="10"
            md="10"
            sm="8"
            col="5"
            className="mb-2 mt-2"
            alt={name}
        />
        <Col>
            <h5>{name}</h5>
        </Col>
    </Col>
)

const TeamSection = () => (
    <ContentSection name="team" title="OpenDC Team">
        <Row className="justify-content-center">
            <TeamLead photoId="aiosup" name="Prof. dr. ir. Alexandru Iosup" description="Project Lead" />
            <TeamLead photoId="fmastenbroek" name="Fabian Mastenbroek" description="Technology Lead" />
            <TeamLead photoId="gandreadis" name="Georgios Andreadis" description="Former Technology Lead (2018-2020)" />
            <TeamLead photoId="vvanbeek" name="Vincent van Beek" description="Former Technology Lead (2017-2018)" />
        </Row>
        <Row className="justify-content-center mt-5">
            <TeamMember photoId="loverweel" name="Leon Overweel" />
            <TeamMember photoId="lfdversluis" name="Laurens Versluis" />
            <TeamMember photoId="evaneyk" name="Erwin van Eyk" />
            <TeamMember photoId="sjounaid" name="Soufiane Jounaid" />
            <TeamMember photoId="wlai" name="Wenchen Lai" />
            <TeamMember photoId="hhe" name="Hongyu He" />
            <TeamMember photoId="jburley" name="Jacob Burley" />
            <TeamMember photoId="jbosch" name="Jaro Bosch" />
        </Row>
    </ContentSection>
)

export default TeamSection
