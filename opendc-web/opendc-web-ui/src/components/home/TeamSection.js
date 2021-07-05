import PropTypes from 'prop-types'
import React from 'react'
import Image from 'next/image'
import { Row, Col } from 'reactstrap'
import ContentSection from './ContentSection'

const TeamLead = ({ photoId, name, description }) => (
    <Col xl="3" lg="3" md="4" sm="6" className="justify-content-center">
        <Row>
            <Col xl="10" lg="10" md="10" sm="8" col="5" className="my-2 mx-auto" alt={name}>
                <Image
                    src={'/img/portraits/' + photoId + '.png'}
                    layout="intrinsic"
                    width={182}
                    height={182}
                    alt={name}
                />
            </Col>
            <Col>
                <h4>{name}</h4>
                <div className="team-member-description">{description}</div>
            </Col>
        </Row>
    </Col>
)

TeamLead.propTypes = {
    photoId: PropTypes.string,
    name: PropTypes.string,
    description: PropTypes.string,
}

const TeamMember = ({ photoId, name }) => (
    <Col xl="2" lg="2" md="3" sm="4" className="justify-content-center">
        <Row>
            <Col xl="10" lg="10" md="10" sm="8" xs="5" className="my-2 mx-auto">
                <Image
                    src={'/img/portraits/' + photoId + '.png'}
                    layout="intrinsic"
                    width={100}
                    height={100}
                    alt={name}
                />
            </Col>
            <Col>
                <h5>{name}</h5>
            </Col>
        </Row>
    </Col>
)

TeamMember.propTypes = {
    photoId: PropTypes.string,
    name: PropTypes.string,
}

const TeamSection = ({ className }) => (
    <ContentSection name="team" title="OpenDC Team" className={className}>
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

TeamSection.propTypes = {
    className: PropTypes.string,
}

export default TeamSection
