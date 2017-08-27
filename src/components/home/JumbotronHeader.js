import React from "react";
import "./JumbotronHeader.css";

const JumbotronHeader = () => (
    <section className="jumbotron-header">
        <div className="container">
            <div className="jumbotron">
                <h1 className="display-3">Open<span className="dc">DC</span></h1>
                <p className="lead">
                    Collaborative Datacenter Simulation and Exploration for Everybody
                </p>
            </div>
        </div>
    </section>
);

export default JumbotronHeader;
