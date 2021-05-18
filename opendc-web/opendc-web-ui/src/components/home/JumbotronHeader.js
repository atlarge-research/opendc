import React from 'react'
import Image from 'next/image'
import { Container, Jumbotron, Button } from 'reactstrap'
import { jumbotronHeader, jumbotron, dc } from './JumbotronHeader.module.scss'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faExternalLinkAlt } from '@fortawesome/free-solid-svg-icons'

const JumbotronHeader = () => (
    <section className={jumbotronHeader}>
        <Container>
            <Jumbotron className={jumbotron}>
                <h1>
                    Open<span className={dc}>DC</span>
                </h1>
                <p className="lead">Collaborative Datacenter Simulation and Exploration for Everybody</p>
                <div className="mt-5">
                    <Image src="/img/logo.png" layout="intrinsic" height={110} width={110} alt="OpenDC" />
                </div>
                <p className="lead mt-5">
                    <Button
                        tag="a"
                        target="_blank"
                        href="https://atlarge-research.com/pdfs/ccgrid21-opendc-paper.pdf"
                        color="warning"
                    >
                        Read about <strong>OpenDC 2.0</strong> <FontAwesomeIcon icon={faExternalLinkAlt} />
                    </Button>
                </p>
            </Jumbotron>
        </Container>
    </section>
)

export default JumbotronHeader
