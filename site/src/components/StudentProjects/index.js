
const studentProjects = [
    {
        title: "Radice: Data-driven Risk Analysis of Sustainable Cloud Infrastructure using Simulation",
        link: "https://repository.tudelft.nl/islandora/object/uuid:00afeb36-724d-4edf-adc7-67ce991c7d12",
        abstract: [
            `Cloud datacenters underpin our increasingly digital society, serving stakeholders across industry, government, and academia. These stakeholders have come to expect reliable operation and high quality of service, yet demand low cost, high scalability, and corporate (environmental) responsibility. Datacenter operators are confronted frequently with highly complex decisions that involve numerous aspects of risk. The consequence of bad decisions can be financial penalties or even loss of customers on the one hand, or a competitive disadvantage or unsustainable environmental impact on the other hand. Despite risk analysis being an integral part of the design and operation of cloud infrastructure, relatively few comprehensive approaches and tools exist, leaving many datacenter operators ill-equipped to make informed decisions with confidence.`,
            `We propose Radice, an instrument for data-driven analysis of IT-related operational risks in sustainable cloud datacenters. Unlike most state-of-the-art approaches used by the industry, Radice automates the process of risk analysis in datacenters and utilizes the large and diverse volume of data reported by the monitoring systems in datacenters, including environmental data. Underpinning this system is the trace-based, discrete-event simulator OpenDC, which enables the exploration of many risk scenarios through its support for diverse workloads, datacenter topologies, and operational phenomena. Radice’s interactive and explorative user interface assists datacenter operators in addressing complex decisions involving risks, providing them with actionable insights, automated visualizations, and suggestions to reduce risk.`,
            `We implement Radice and conduct a comprehensive evaluation of the system to demonstrate how it can aid datacenter operators when confronted with fundamental risk trade-offs. Although Radice is designed to work across many kinds of datacenters, in this work, we focus on private-cloud, business-critical workloads, and on public-cloud operations, representing the majority of workloads in Dutch datacenters. Our experiments show many interesting findings, supporting our claim for a need for data-driven risk analysis in datacenters. We highlight the increasing risk faced by datacenter operators due to price surges in the electricity and CO2 bond markets, and demonstrate how Radice can be used to control such risks. We further show that Radice can automatically optimize topology and operational settings in datacenters for risk, revealing configurations that reduce the overall risk by 10%–30%. Following extensive performance engineering, Radice is able to evaluate risk scenarios by a factor 70x–330x faster than others, opening possibilities for interactive risk exploration. We release Radice as free and open-source software for the community to inspect and re-use.`,
        ],
        type: "Master Thesis",
        year: 2022,
        student: "Fabian Mastenbroek",
    },
    {
        title: "How Can Datacenters Join the Smart Grid to Address the Climate Crisis?",
        link: "https://arxiv.org/abs/2108.01776",
        type: "Bachelor Thesis",
        year: 2021,
        student: "Hongyu He",
    },
    {
        title: "Capelin: Fast Data-Driven Capacity Planning for Cloud Datacenters",
        link: "https://repository.tudelft.nl/islandora/object/uuid:d6d50861-86a3-4dd3-a13f-42d84db7af66?collection=education",
        type: "Master Thesis",
        year: 2020,
        student: "Georgios Andreadis",
    },
    {
        title: "Modeling and Simulation of the Google TensorFlow Ecosystem",
        link: "https://atlarge-research.com/pdfs/lai2020thesis.pdf",
        type: "Master Thesis",
        year: 2020,
        student: "Wenchen Lai",
        abstract: [
            `Recently, many powerful machine learning (ML) systems or ecosystems have
            been developed to render ML solutions feasible for more complex applications.
            Google TensorFlow ecosystem is one of the most famous and popular machine
            learning ecosystem. Because of some emerging technologies, such as big data,
            Internet of Things (IoT), high-performance computing (HPC), the power of dat-
            acenters are expected to be applied in Artificial Intelligence (AI) field. However,
            when performing ML tasks in datacenters, new challenges and issues arise, such
            as data management. Understanding the behaviors of the Google TensorFlow
            ecosystem is our main objective.`,
            `We adopt the reference architecture method and extend our reference archi-
            tecture created in our literature survey. We add additional deeper layers and
            identify more than 10 new components to enrich our reference architecture.
            Based on the reference architecture, we create a predictive model of Google
            TensorFlow and integrate it into a discrete event simulator OpenDC. We design simulation experiments to validate our model and evaluate the performance
            of the TensorFlow ecosystem in HPC environments.`
        ]
    },
    {
        title: "OpenDC Serverless: Design, Implementation and Evaluation of a FaaS Platform Simulator",
        link: "https://zenodo.org/record/4046675",
        type: "Bachelor Thesis",
        year: 2020,
        student: "Soufiane Jounaid",
    },
    {
        title: "LEGO, but with Servers: Creating the Building Blocks to Design and Simulate Datacenters",
        link: "https://atlarge-research.com/pdfs/BSc-Thesis-JACOB_BURLEY_FINAL.pdf",
        type: "Bachelor Thesis",
        year: 2020,
        student: "Jacob Burley",
    },
    {
        title: "A Trace-Based Validation Study of OpenDC",
        link: "https://atlarge-research.com/pdfs/2020-12-02_bsc_thesis_jaro_final.pdf",
        type: "Bachelor Thesis",
        year: 2020,
        student: "Jaro Bosch",
    },
    {
        title: "A Systematic Design Space Exploration of Datacenter Schedulers",
        link: "https://repository.tudelft.nl/islandora/object/uuid%3A20478016-cc7d-4c87-aa12-25b46f511277?collection=education",
        type: "Bachelor Thesis",
        year: 2019,
        student: "Fabian Mastenbroek",
    },
]

const categories = ["Master Thesis", "Bachelor Thesis", "Other"]

const standardTypes = ["Master Thesis", "Bachelor Thesis"]

function StudentProject({ title, link, type, year, student, abstract }) {
    return (
        <li style={{ marginBottom: '1rem' }}>
            {link ? <a href={link}>{title}</a> : <span>{title}</span>}
            <br />
            <strong>{!standardTypes.includes(type) ? `${type}, ` : ''}{year}</strong>
            <br />
            <span>{student}</span>
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

function StudentProjectSection({ category }) {
    const projects = studentProjects
        .filter((p) => (category === "Other" ? !["Master Thesis", "Bachelor Thesis"].includes(p.type) : p.type === category))
        .sort((a, b) => b.year - a.year)

    if (projects.length === 0) return null

    return (
        <>
            <h3>{category}</h3>
            <ol>
                {projects.map((project) => (
                    <StudentProject key={project.title} {...project} />
                ))}
            </ol>
        </>
    )
}

export function StudentProjects() {
    return (
        <>
            {categories.map((category) => (
                <StudentProjectSection key={category} category={category} />
            ))}
        </>
    )
}
