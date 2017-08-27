import React from "react";
import ContentSection from "./ContentSection";

const TeamMember = ({photoId, name, description}) => (
    <div className="col-xl-3 col-lg-3 col-md-5 col-sm-6 col-12 justify-content-center">
        <img src={"img/portraits/" + photoId + ".png"}
             className="col-xl-10 col-lg-10 col-md-10 col-sm-8 col-5 mb-2 mt-2"
             alt={name}/>
        <div className="col-12">
            <h4>{name}</h4>
            <div className="team-member-description">
                {description}
            </div>
        </div>
    </div>
);

const TeamSection = () => (
    <ContentSection name="team" title="The Team">
        <div className="row justify-content-center">
            <TeamMember photoId="aiosup" name="Prof. dr. ir. Alexandru Iosup"
                        description="Project Lead"/>
            <TeamMember photoId="loverweel" name="Leon Overweel"
                        description="Product Lead and Software Engineer responsible for the web server, database, and
                        API specification"/>
            <TeamMember photoId="gandreadis" name="Georgios Andreadis"
                        description="Software Engineer responsible for the frontend web application"/>
            <TeamMember photoId="mbijman" name="Matthijs Bijman"
                        description="Software Engineer responsible for the datacenter simulator"/>
        </div>
    </ContentSection>
);

export default TeamSection;
