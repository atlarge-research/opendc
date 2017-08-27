import React from "react";
import ContentSection from "./ContentSection";
import "./ContentSection.css";

const ContactSection = () => (
    <ContentSection name="contact">
        <h1>Contact</h1>
        <div className="row">
            <img src="img/tudelfticon.png" className="col-lg-2 col-md-2 col-sm-3 col-xs-6
                    col-lg-offset-4 col-md-offset-4 col-sm-offset-3 col-xs-offset-3 tudelft-icon" alt="TU Delft Logo"/>
            <div className="col-lg-4 col-md-5 col-sm-6 col-xs-10
                col-lg-offset-0 col-md-offset-0 col-sm-offset-0 col-xs-offset-1 text-left">
                <div className="row vcenter">
                    <img src="img/email-icon.png" className="col-lg-2 col-md-2 col-sm-2 col-xs-2" alt="Email Icon"/>
                    <div className="info-points col-lg-10 col-md-10 col-sm-10 col-xs-10">
                        <a href="mailto:opendc@atlarge-research.com">opendc@atlarge-research.com</a>
                    </div>
                </div>
                <div className="row vcenter">
                    <img src="img/github-icon.png" className="col-lg-2 col-md-2 col-sm-2 col-xs-2" alt="Github Icon"/>
                    <div className="info-points col-lg-10 col-md-10 col-sm-10 col-xs-10">
                        <a href="https://github.com/atlarge-research/opendc">atlarge-research/opendc</a>
                    </div>
                </div>
            </div>
        </div>
        <div className="atlarge-footer row">
            A project by the <a href="http://atlarge-research.com"><strong>@Large Research Group</strong></a>.
        </div>
    </ContentSection>
);

export default ContactSection;
