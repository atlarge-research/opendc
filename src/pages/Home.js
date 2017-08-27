import React from 'react';
import ContactSection from "../components/home/ContactSection";
import JumbotronHeader from "../components/home/JumbotronHeader";
import ModelingSection from "../components/home/ModelingSection";
import SimulationSection from "../components/home/SimulationSection";
import StakeholderSection from "../components/home/StakeholderSection";
import TeamSection from "../components/home/TeamSection";
import TechnologiesSection from "../components/home/TechnologiesSection";
import HomeNavbar from "../components/navigation/HomeNavbar";
import jQuery from "../util/jquery";
import "./Home.css";

class Home extends React.Component {
    componentDidMount() {
        jQuery("body-wrapper").scrollspy({target: "#navbar"});
    }

    render() {
        return (
            <div>
                <HomeNavbar/>
                <div className="body-wrapper page-container" data-spy="scroll" data-target="#navbar">
                    <JumbotronHeader/>
                    <StakeholderSection/>
                    <ModelingSection/>
                    <SimulationSection/>
                    <TechnologiesSection/>
                    <TeamSection/>
                    <ContactSection/>
                </div>
            </div>
        );
    }
}

export default Home;
