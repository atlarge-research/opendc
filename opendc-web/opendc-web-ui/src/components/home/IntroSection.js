import PropTypes from 'prop-types'
import React from 'react'
import Image from 'next/image'
import { Container, Row, Col } from 'reactstrap'

const IntroSection = ({ className }) => (
    <section id="intro" className={className}>
        <Container className="pt-5 pb-3">
            <Row className="justify-content-center">
                <Col xl="4" lg="4" md="4" sm="8">
                    <h4>The datacenter (DC) industry...</h4>
                    <ul>
                        <li>Is worth over $15 bn, and growing</li>
                        <li>Has many hard-to-grasp concepts</li>
                        <li>Needs to become accessible to many</li>
                    </ul>
                </Col>
                <Col xl="4" lg="4" md="4" sm="8">
                    <Image
                        src="/img/datacenter-drawing.png"
                        className="col-12"
                        layout="intrinsic"
                        width={350}
                        height={197}
                        alt="Schematic top-down view of a datacenter"
                    />
                    <p className="col-12 figure-caption text-center">
                        <a href="http://www.dolphinhosts.co.uk/wp-content/uploads/2013/07/data-centers.gif">
                            Image source
                        </a>
                    </p>
                </Col>
                <Col xl="4" lg="4" md="4" sm="8">
                    <h4>OpenDC provides...</h4>
                    <ul>
                        <li>Collaborative online DC modeling</li>
                        <li>Diverse and effective DC simulation</li>
                        <li>Exploratory DC performance feedback</li>
                    </ul>
                </Col>
            </Row>
        </Container>
    </section>
)

IntroSection.propTypes = {
    className: PropTypes.string,
}

export default IntroSection
