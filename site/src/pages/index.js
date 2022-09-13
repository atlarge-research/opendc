import React from 'react'
import clsx from 'clsx'
import Link from '@docusaurus/Link'
import useDocusaurusContext from '@docusaurus/useDocusaurusContext'
import Layout from '@theme/Layout'
import HomepageInto from '@site/src/components/HomepageIntro'
import HomepageFeatures from '@site/src/components/HomepageFeatures'

import styles from './index.module.css'
import Logo from '@site/static/img/logo.svg'

function HomepageHeader() {
    const { siteConfig } = useDocusaurusContext()
    return (
        <header className={clsx('hero hero--primary', styles.heroBanner)}>
            <div className="container">
                <h1 className="hero__title">{siteConfig.title}</h1>
                <p className="hero__subtitle">{siteConfig.tagline}</p>
                <Logo role="img" width="100" height="100" alt="OpenDC logo" className={styles.logo} />
                <div className={styles.buttons}>
                    <Link className="button button--secondary button--lg" to="/docs/intro">
                        Getting Started with OpenDC - 10min ⏱️
                    </Link>
                </div>
            </div>
        </header>
    )
}

export default function Home() {
    const { siteConfig } = useDocusaurusContext()
    return (
        <Layout
            title={`${siteConfig.title} cloud datacenter simulator`}
            description="Collaborative Datacenter Simulation and Exploration for Everybody"
        >
            <HomepageHeader />
            <main>
                <HomepageInto />
                <HomepageFeatures />
            </main>
        </Layout>
    )
}
