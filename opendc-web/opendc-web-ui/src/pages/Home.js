import React from 'react'
import ContactSection from '../components/home/ContactSection'
import IntroSection from '../components/home/IntroSection'
import JumbotronHeader from '../components/home/JumbotronHeader'
import ModelingSection from '../components/home/ModelingSection'
import SimulationSection from '../components/home/SimulationSection'
import StakeholderSection from '../components/home/StakeholderSection'
import TeamSection from '../components/home/TeamSection'
import TechnologiesSection from '../components/home/TechnologiesSection'
import HomeNavbar from '../components/navigation/HomeNavbar'
import './Home.sass'
import { useDocumentTitle } from '../util/hooks'

function Home() {
    useDocumentTitle('OpenDC')
    return (
        <div>
            <HomeNavbar />
            <div className="body-wrapper page-container">
                <JumbotronHeader />
                <IntroSection />
                <StakeholderSection />
                <ModelingSection />
                <SimulationSection />
                <TechnologiesSection />
                <TeamSection />
                <ContactSection />
            </div>
        </div>
    )
}

export default Home
