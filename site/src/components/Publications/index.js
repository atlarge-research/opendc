
const aboutOpenDC = [
    {
        title: "OpenDC 2.0: Convenient Modeling and Simulation of Emerging Technologies in Cloud Datacenters",
        link: "https://atlarge-research.com/pdfs/ccgrid21-opendc-paper.pdf",
        venue: "CCGrid",
        year: 2021,
        authors: [
            "Fabian Mastenbroek", "Georgios Andreadis", "Soufiane Jounaid", "Wenchen Lai",
            "Jacob Burley", "Jaro Bosch", "Erwin van Eyk", "Laurens Versluis",
            "Vincent van Beek", "Alexandru Iosup"
        ],
    },
    {
        title: "The OpenDC vision: Towards collaborative datacenter simulation and exploration for everybody",
        link: "https://atlarge-research.com/pdfs/opendc-vision17ispdc_cr.pdf",
        venue: "ISPDC",
        year: 2017,
        authors: [
            "Alexandru Iosup", "Georgios Andreadis", "Vincent van Beek", "Matthijs Bijman",
            "Erwin van Eyk", "Mihai Neacsu", "Leon Overweel", "Sacheendra Talluri",
            "Laurens Versluis", "Maaike Visser"
        ],
    },
]

const usingOpenDC = [
    {
        title: "OpenDC-STEAM: Realistic Modeling and Systematic Exploration of Composable Techniques for Sustainable Datacenters",
        venue: "CCGrid",
        year: 2026,
        authors: [
            "Dante Niewenhuis", "Sacheendra Talluri", "Alexandru Iosup", "Tiziano de Matteis"
        ],
        abstract: [
            `The need to reduce datacenter carbon-footprint is
            urgent. While many sustainability techniques have been pro-
            posed, they are often evaluated in isolation, using limited setups
            or analytical models that overlook real-world dynamics and
            interactions between methods. This makes it challenging for
            researchers and operators to understand the effectiveness and
            trade-offs of combining such techniques. We design OpenDC-
            STEAM, an open-source customizable datacenter simulator, to
            investigate the individual and combined impact of sustainability
            techniques on datacenter operational and embodied carbon emis-
            sions, and their trade-off with performance. Using STEAM, we
            systematically explore three representative techniques–horizontal
            scaling, leveraging batteries, and temporal shifting–with diverse
            representative workloads, datacenter configurations, and carbon-
            intensity traces. Our analysis highlights that datacenter dynamics
            can influence their effectiveness and that combining strategies
            can significantly lower emissions, but introduces complex cost-
            emissions-performance trade-offs that STEAM can help nav-
            igate. STEAM supports the integration of new models and
            techniques, making it a foundation framework for holistic,
            quantitative, and reproducible research in sustainable computing.
            Following open-science principles, STEAM is available as FOSS:
            https://github.com/atlarge-research/OpenDC-STEAM. This is an
            extended version of a paper published at CCGRID 2026.`
        ]
    },
    {
        title: "Capelin: Data-Driven Compute Capacity Procurement for Cloud Datacenters Using Portfolios of Scenarios",
        link: "https://www.computer.org/csdl/journal/td/2022/01/09444213/1tYo2a8BeWA",
        venue: "IEEE Transactions on Parallel and Distributed Systems (TPDS)",
        year: 2022,
        authors: ["Georgios Andreadis", "Fabian Mastenbroek", "Vincent van Beek", "Alexandru Iosup"],
    },
    {
        title: "A Reference Architecture for Datacenter Scheduling",
        link: "https://arxiv.org/pdf/1808.04224",
        venue: "SC",
        year: 2018,
        authors: ["Georgios Andreadis", "Laurens Versluis", "Fabian Mastenbroek", "Alexandru Iosup"],
    },
]

function Publication({ title, link, venue, year, authors, abstract }) {
    return (
        <li style={{ marginBottom: '1rem' }}>
            {link ? <a href={link}>{title}</a> : <span>{title}</span>}
            <br />
            <strong>{venue}, {year}</strong>
            <br />
            <span>{authors.join(', ')}</span>
            {abstract && (
                <details style={{ marginTop: '0.5rem' }}>
                    <summary style={{ cursor: 'pointer' }}>Abstract</summary>
                    {abstract.map((paragraph, i) => (
                        <p key={i} style={{ marginTop: '0.5rem', textAlign: 'justify' }}>{paragraph}</p>
                    ))}
                </details>
            )}
        </li>
    )
}

function PublicationList({ publications }) {
    const sorted = [...publications].sort((a, b) => b.year - a.year)
    return (
        <ol>
            {sorted.map((pub) => (
                <Publication key={pub.title} {...pub} />
            ))}
        </ol>
    )
}

export function PublicationsAboutOpenDC() {
    return <PublicationList publications={aboutOpenDC} />
}

export function PublicationsUsingOpenDC() {
    return <PublicationList publications={usingOpenDC} />
}
