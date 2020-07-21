import React from 'react'
import DocumentTitle from 'react-document-title'
import ContactSection from '../components/home/ContactSection'
import IntroSection from '../components/home/IntroSection'
import JumbotronHeader from '../components/home/JumbotronHeader'
import ModelingSection from '../components/home/ModelingSection'
import SimulationSection from '../components/home/SimulationSection'
import StakeholderSection from '../components/home/StakeholderSection'
import TeamSection from '../components/home/TeamSection'
import TechnologiesSection from '../components/home/TechnologiesSection'
import HomeNavbar from '../components/navigation/HomeNavbar'
import jQuery from '../util/jquery'
import './Home.css'

class Home extends React.Component {
    state = {
        scrollSpySetup: false,
    }

    componentDidMount() {
        const scrollOffset = 60
        jQuery('#navbar')
            .find('li a')
            .click(function (e) {
                if (jQuery(e.target).parents('.auth-links').length > 0) {
                    return
                }
                e.preventDefault()
                jQuery(jQuery(this).attr('href'))[0].scrollIntoView()
                window.scrollBy(0, -scrollOffset)
            })

        if (!this.state.scrollSpySetup) {
            jQuery('body').scrollspy({
                target: '#navbar',
                offset: scrollOffset,
            })
            this.setState({ scrollSpySetup: true })
        }
    }

    render() {
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
                    <DocumentTitle title="OpenDC" />
                </div>
            </div>
        )
    }
}

export default Home
