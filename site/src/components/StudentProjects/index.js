
const studentProjects = [
    {
        title: "Radice: Data-driven Risk Analysis of Sustainable Cloud Infrastructure using Simulation",
        link: "https://repository.tudelft.nl/islandora/object/uuid:00afeb36-724d-4edf-adc7-67ce991c7d12",
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

function StudentProject({ title, link, type, year, student }) {
    return (
        <li style={{ marginBottom: '1rem' }}>
            {link ? <a href={link}>{title}</a> : <span>{title}</span>}
            <br />
            <strong>{!standardTypes.includes(type) ? `${type}, ` : ''}{year}</strong>
            <br />
            <span>{student}</span>
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
