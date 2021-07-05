import PropTypes from 'prop-types'
import React from 'react'
import Image from 'next/image'
import { Col, Row } from 'reactstrap'
import ContentSection from './ContentSection'

const SimulationSection = ({ className }) => {
    return (
        <ContentSection name="project" title="Datecenter Simulation" className={className}>
            <Row>
                <Col xl="5" lg="5" md="5" sm="2" className="text-left my-auto order-1">
                    <h3>Working with OpenDC:</h3>
                    <ul>
                        <li>Seamlessly switch between construction and simulation modes</li>
                        <li>
                            Choose one of several predefined workloads (Business Critical, Workflows, Machine Learning,
                            Serverless, etc.)
                        </li>
                        <li>Compare datacenter topologies using automated plots and visual summaries</li>
                    </ul>
                </Col>
                <Col xl="7" lg="7" md="7" sm="12">
                    <Image
                        src="/img/screenshot-simulation.png"
                        className="col-12"
                        layout="intrinsic"
                        width={635}
                        height={419}
                        alt="Running an experiment in OpenDC"
                    />
                    <Row className="text-muted justify-content-center">Running an experiment in OpenDC</Row>
                </Col>
            </Row>
            <Row className="mt-5">
                <Col xl="5" lg="5" md="5" sm="2" className="text-left my-auto">
                    <h3>OpenDC&apos;s Simulator:</h3>
                    <ul>
                        <li>Includes a detailed operational model of modern datacenters</li>
                        <li>
                            Support for emerging datacenter technologies around <em>cloud computing</em>,{' '}
                            <em>serverless computing</em>, <em>big data</em>, and <em>machine learning</em>
                        </li>
                    </ul>
                </Col>

                <Col xl="7" lg="7" md="7" sm="12">
                    <Image
                        src="/img/opendc-architecture.png"
                        className="col-12"
                        layout="intrinsic"
                        width={635}
                        height={232}
                        alt="OpenDC's Architecture"
                    />
                    <Row className="text-muted justify-content-center">OpenDC&apos;s Architecture</Row>
                </Col>
            </Row>
        </ContentSection>
    )
}

SimulationSection.propTypes = {
    className: PropTypes.string,
}

export default SimulationSection
