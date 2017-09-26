import React from "react";
import FontAwesome from "react-fontawesome";
import Mailto from "react-mailto";
import "./ContactSection.css";
import ContentSection from "./ContentSection";

const ContactSection = () => (
    <ContentSection name="contact" title="Contact">
        <div className="row justify-content-center">
            <div className="col-4">
                <a href="https://github.com/atlarge-research/opendc">
                    <FontAwesome name="github" size="3x" className="mb-2"/>
                    <div className="w-100"/>
                    atlarge-research/opendc
                </a>
            </div>
            <div className="col-4">
                <Mailto title="Contact us" email="opendc@atlarge-research.com">
                    <FontAwesome name="envelope" size="3x" className="mb-2"/>
                    <div className="w-100"/>
                    opendc@atlarge-research.com
                </Mailto>
            </div>
        </div>
        <div className="row">
            <div className="col text-center">
                <img src="img/tudelft-icon.png" className="img-fluid tudelft-icon" alt="TU Delft"/>
            </div>
        </div>
        <div className="row">
            <div className="col text-center">
                A project by the &nbsp;
                <a href="http://atlarge.science" target="_blank" rel="noopener noreferrer">
                    <strong>@Large Research Group</strong>
                </a>.
            </div>
        </div>
        <div className="row">
            <div className="col text-center disclaimer mt-5 small">
                <FontAwesome name="exclamation-triangle" size="2x" className="mr-2"/>
                <br/>
                OpenDC is an experimental tool. Your data may get lost, overwritten, or otherwise become unavailable.
                <br/>
                The OpenDC authors should in no way be liable in the event this happens (see our <strong><a
                href="https://github.com/atlarge-research/opendc/blob/master/LICENSE.md">license</a></strong>). Sorry
                for the inconvenience.
            </div>
        </div>
    </ContentSection>
);

export default ContactSection;
