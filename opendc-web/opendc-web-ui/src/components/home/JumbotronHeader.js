import React from 'react'
import { Container, Jumbotron, Button } from 'reactstrap'
import './JumbotronHeader.sass'

const JumbotronHeader = () => (
    <section className="jumbotron-header">
        <Container>
            <Jumbotron className="text-center">
                <h1>
                    Open<span className="dc">DC</span>
                </h1>
                <p className="lead">Collaborative Datacenter Simulation and Exploration for Everybody</p>
                <img src="img/logo.png" className="img-responsive mt-3" alt="OpenDC" />
                <p className="lead mt-5">
                    <Button
                        tag="a"
                        target="_blank"
                        href="https://atlarge-research.com/pdfs/ccgrid21-opendc-paper.pdf"
                        color="warning"
                    >
                        Read about <strong>OpenDC 2.0</strong> <i className="fa fa-external-link" />
                    </Button>
                </p>
            </Jumbotron>
        </Container>
    </section>
)

export default JumbotronHeader
