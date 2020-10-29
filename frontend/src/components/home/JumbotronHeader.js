import React from 'react'
import './JumbotronHeader.sass'

const JumbotronHeader = () => (
    <section className="jumbotron-header">
        <div className="container">
            <div className="jumbotron text-center">
                <h1>
                    Open<span className="dc">DC</span>
                </h1>
                <p className="lead">Collaborative Datacenter Simulation and Exploration for Everybody</p>
                <img src="img/logo.png" className="img-responsive mt-3" alt="OpenDC" />
            </div>
        </div>
    </section>
)

export default JumbotronHeader
