import React from 'react'

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

function Publication({ title, link, venue, year, authors }) {
    return (
        <li style={{ marginBottom: '1rem' }}>
            <a href={link}>{title}</a>
            <br />
            <strong>{venue}, {year}</strong>
            <br />
            <span>{authors.join(', ')}</span>
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
