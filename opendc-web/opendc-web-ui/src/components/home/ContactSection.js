import React from 'react'
import Image from 'next/image'
import { Row, Col } from 'reactstrap'
import ContentSection from './ContentSection'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faExclamationTriangle, faEnvelope } from '@fortawesome/free-solid-svg-icons'
import { faGithub } from '@fortawesome/free-brands-svg-icons'
import { contactSection, tudelftIcon } from './ContactSection.module.scss'

const ContactSection = () => (
    <ContentSection name="contact" title="Contact" className={contactSection}>
        <Row className="justify-content-center">
            <Col md="4">
                <a href="https://github.com/atlarge-research/opendc">
                    <FontAwesomeIcon icon={faGithub} size="3x" className="mb-2" />
                    <div className="w-100" />
                    atlarge-research/opendc
                </a>
            </Col>
            <Col md="4">
                <a href="mailto:opendc@atlarge-research.com">
                    <FontAwesomeIcon icon={faEnvelope} size="3x" className="mb-2" />
                    <div className="w-100" />
                    opendc@atlarge-research.com
                </a>
            </Col>
        </Row>
        <Row>
            <Col className="text-center">
                <Image
                    src="/img/tudelft-icon.png"
                    className={tudelftIcon}
                    layout="intrinsic"
                    width={162}
                    height={100}
                    alt="TU Delft"
                />
            </Col>
        </Row>
        <Row>
            <Col className="text-center">
                A project by the &nbsp;
                <a href="http://atlarge.science" target="_blank" rel="noopener noreferrer">
                    <strong>@Large Research Group</strong>
                </a>
                .
            </Col>
        </Row>
        <Row>
            <Col className="text-center disclaimer mt-5 small">
                <FontAwesomeIcon icon={faExclamationTriangle} size="2x" className="mr-2" />
                <br />
                <strong>Disclaimer: </strong>
                OpenDC is an experimental tool. Your data may get lost, overwritten, or otherwise become unavailable.
                <br />
                The OpenDC authors should in no way be liable in the event this happens (see our{' '}
                <strong>
                    <a href="https://github.com/atlarge-research/opendc/blob/master/LICENSE.txt">license</a>
                </strong>
                ). Sorry for the inconvenience.
            </Col>
        </Row>
    </ContentSection>
)

export default ContactSection
