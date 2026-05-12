
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
        abstract: [
            `Cloud datacenters are important for the digital
society, serving stakeholders across industry, government, and
academia. Simulation is a critical part of exploring datacenter
technologies, enabling scalable experimentation with millions of
jobs and hundreds of thousands of machines, and what-if analysis
in a matter of minutes to hours. Although the community has
already developed powerful simulators, emerging technologies
and applications in modern datacenters require new approaches.
Addressing this requirement, in this work we propose OpenDC,
a new platform for datacenter simulation. OpenDC includes
novel models for emerging cloud-datacenter technologies and
applications, such as serverless computing with FaaS deploy-
ment and TensorFlow-based machine learning. Our design also
focuses on convenience, with a web-based interface for interactive
experimentation, support for experiment automation, a library of
prefabs for constructing and sharing datacenter designs, and sup-
port for diverse input formats and output metrics. We implement,
validate, and open-source OpenDC 2.0, a significant redesign and
release after a multi-year research and development process. We
demonstrate the benefits of OpenDC for the field through a set of
representative use-cases: serverless, machine learning, procure-
ment of HPC-as-a-Service infrastructure, educational practices,
and reproducibility studies. Overall, OpenDC helps understand
how datacenters work, design datacenter infrastructure, and
train the next generation of experts.`
        ]
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
        abstract: [
            `In the new Digital Economy, massive computer sys-
tems, often grouped in datacenters, serve as factories “producing”
cloud services with massive consumption. However, to afford
cloud services globally, we must address new research challenges
in designing, operating, and using modern datacenters. We
must also address challenges in educating and training the next
generation of datacenter engineers. Addressing such challenges,
in this work we present our vision on OpenDC: we envision
the exploration of various datacenter concepts and technologies,
using existing and new scientific methods, enabling new education
practices and topics, and leading to the creation of new software
and data artifacts. We present the datacenter concepts and
technologies we are currently planning to explore using OpenDC.
We identify the scientific methods we want to use, and explain
our vision of education practices. We present the architecture and
open-source program underlying the OpenDC software, and the
format and open-access data we use for datacenter experiments.
We conclude with an open invitation for the community to join
our effort.`
        ]
    },
]

const usingOpenDC = [
    {
        title: "M3SA: Exploring Datacenter Performance and Climate-Impact with Multi- and Meta-Model Simulation and Analysis",
        venue: "Computing Frontiers",
        year: 2026,
        authors: [
            "Radu Nicolae", "Dante Niewenhuis", "Sacheendra Talluri", "Alexandru Iosup"
        ],
        abstract: [
            `Datacenters are vital to our digital society, but consume a con-
siderable fraction of global electricity and demand is projected to
increase. To improve their sustainability and performance, we en-
vision that simulators will become primary decision-making tools.
However, and unlike other fields focusing on key societal infrastruc-
ture such as waterworks and mass transit, datacenter simulators do
not yet combine multiple independent models into their operation
and thus suffer from issues associated with singular models, such as
specialization, and lack of adaptability to operational phenomena.
To address this challenge, we propose M3SA, a datacenter simula-
tion and analysis framework that uses discrete-event simulation
to predict, for each model, the impact on climate and performance
under various realistic datacenter conditions, and then combines
these predictions. We design an architecture for simulating multi-
ple concurrent models (Multi-Model), a technique for integrating
the results of multiple models into a Meta-Model, and a procedure
for quantifying Meta-Model accuracy. Through experiments with
an M3SA prototype, we reproduce and enhance a peer-reviewed
experiment with multi-model analysis and identify that M3SA can
halve error rates of singular models (from 7.59% to 3.81%), with
under 20% computational overhead.`
        ]
    },
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
        title: "OpenDT: Exploring Datacenter Performance and Sustainability with a Self-Calibrating Digital Twin",
        link: "https://dl.acm.org/doi/10.1145/3777911.3800634",
        venue: "HotCloudPerf",
        year: 2026,
        authors: [
            "Radu Nicolae", "Jules van der Toorn", "Stavriana Kraniti", "Houcen Liu", "Alexandru Iosup"
        ],
        abstract: [
            `Datacenters are the backbone of our digital society, but raise numerous operational challenges. 
            We envision digital twins becoming primary instruments in datacenter operations, continuously and autonomously helping with major 
            operational decisions and with adapting ICT infrastructure, live, with a human-in-the-loop. Although fields such as aviation and 
            autonomous driving successfully employ digital twins, an open-source digital twin for datacenters has not been demonstrated to 
            the community. Addressing this challenge, we design, implement, and experiment using OpenDT, an Open-source, Digital Twin for 
            monitoring and operating datacenters through a continuous integration cycle that includes: (1) live and continuous telemetry data; 
            (2) discrete-event simulation using live telemetry from the physical ICT, with self-calibration; and (3) SLO-aware and human-approved 
            feedback to physical ICT. Through trace-driven experiments with a prototype mainly covering stages 1 and 2 of the cycle, we show that 
            (i) OpenDT can be used to reproduce peer-reviewed experiments and extend the analysis with performance and energy-efficiency results; 
            (ii) OpenDT's online re-calibration can increase digital-twinning accuracy, quantified to a MAPE 4.39% vs. 7.86% in peer-reviewed work. 
            OpenDT adheres to FAIR/FOSS principles and is available at: https://github.com/atlarge-research/opendt/tree/hcp`
        ]
    },
    {
        title: "Cloud Uptime Archive: Open-Access Availability Data of Web, Cloud, and Gaming Services",
        link: "https://arxiv.org/abs/2504.09476",
        venue: "TPDS",
        year: 2026,
        authors: [
            "Sacheendra Talluri", "Dante Niewenhuis", "Xiaoyu Chu", "Jakob Kyselica", "Mehmet Cetin", "Alexander Balgavy", "Alexandru Iosup"
        ],
        abstract: [
            `Cloud services are critical to society. However, their
reliability is poorly understood. Towards solving the problem, we
propose a standard repository for cloud uptime data. We populate
this repository with the data we collect containing failure reports
from users and operators of cloud services, web services, and
online games. The multiple vantage points help reduce bias from
individual users and operators. We compare our new data to
existing failure data from the Failure Trace Archive and the
Google cluster trace.
We analyze the MTBF and MTTR, time patterns, failure sever-
ity, user-reported symptoms, and operator-reported symptoms
of failures in the data we collect. We observe that high-level
user facing services fail less often than low-level infrastructure
services, likely due to them using fault-tolerance techniques. We
use simulation-based experiments to demonstrate the impact of
different failure traces on the performance of checkpointing and
retry mechanisms.
We release the data, and the analysis and simulation tools,
as open-source artifacts available at https://github.com/atlarge-
research/cloud-uptime-archive.`
        ]
    },
    {
        title: "FootPrinter: Quantifying Data Center Carbon Footprint",
        link: "https://dl.acm.org/doi/10.1145/3629527.3651419",
        venue: "HotCloudPerf",
        year: 2024,
        authors: [
            "Dante Niewenhuis", "Sacheendra Talluri", "Alexandru Iosup", "Tiziano de Matteis"
        ],
        abstract: [
            `Data centers have become an increasingly significant contributor to the global carbon footprint. 
            In 2021, the global data center industry was responsible for around 1% of the worldwide greenhouse gas emissions. 
            With more resource-intensive workloads, such as Large Language Models, gaining popularity, this percentage is expected to increase further. 
            Therefore, it is crucial for data center service providers to become aware of and accountable for the sustainability 
            impact of their design and operational choices. However, reducing the carbon footprint of data centers has been a challenging 
            process due to the lack of comprehensive metrics, carbon-aware design tools, and guidelines for carbon-aware optimization. 
            In this work, we propose FootPrinter, a first-of-its-kind tool that supports data center designers and operators in assessing 
            the environmental impact of their data center. FootPrinter uses coarse-grained operational data, grid energy mix information, 
            and discrete event simulation to determine the data center's operational carbon footprint and evaluate the impact of infrastructural 
            or operational changes. FootPrinter can simulate days of operations of a regional data center on a commodity laptop in a few seconds, 
            returning the estimated footprint with marginal error. By making this project open source, we hope to engage the community in the 
            development of methodologies and tools for systematically assessing and exploring the sustainability of data centers.`
        ]
    },
    {
        title: "Capelin: Data-Driven Compute Capacity Procurement for Cloud Datacenters Using Portfolios of Scenarios",
        link: "https://www.computer.org/csdl/journal/td/2022/01/09444213/1tYo2a8BeWA",
        venue: "IEEE Transactions on Parallel and Distributed Systems (TPDS)",
        year: 2022,
        authors: ["Georgios Andreadis", "Fabian Mastenbroek", "Vincent van Beek", "Alexandru Iosup"],
        abstract: [
            `Cloud datacenters provide a backbone to our digital society. Inaccurate capacity procurement for cloud datacenters can lead to significant 
            performance degradation, denser targets for failure, and unsustainable energy consumption. Although this activity is core to improving cloud 
            infrastructure, relatively few comprehensive approaches and support tools exist for mid-tier operators, leaving many planners with merely 
            rule-of-thumb judgement. We derive requirements from a unique survey of experts in charge of diverse datacenters in several countries. 
            We propose Capelin, a data-driven, scenario-based capacity planning system for mid-tier cloud datacenters. Capelin introduces the notion 
            of portfolios of scenarios, which it leverages in its probing for alternative capacity-plans. At the core of the system, a trace-based, discrete-event 
            simulator enables the exploration of different possible topologies, with support for scaling the volume, variety, and velocity of resources, and 
            for horizontal (scale-out) and vertical (scale-up) scaling. Capelin compares alternative topologies and for each gives detailed quantitative operational 
            information, which could facilitate human decisions of capacity planning. We implement and open-source Capelin, and show through comprehensive trace-based 
            experiments it can aid practitioners. The results give evidence that reasonable choices can be worse by a factor of 1.5-2.0 than the best, in terms of 
            performance degradation or energy consumption.`
        ]
    },
    {
        title: "A Reference Architecture for Datacenter Scheduling",
        link: "https://arxiv.org/pdf/1808.04224",
        venue: "SC",
        year: 2018,
        authors: ["Georgios Andreadis", "Laurens Versluis", "Fabian Mastenbroek", "Alexandru Iosup"],
        abstract: [
            `Datacenters act as cloud-infrastructure to stakehold-
ers across industry, government, and academia. To meet growing
demand yet operate efficiently, datacenter operators employ
increasingly more sophisticated scheduling systems, mechanisms,
and policies. Although many scheduling techniques already
exist, relatively little research has gone into the abstraction
of the scheduling process itself, hampering design, tuning, and
comparison of existing techniques. In this work, we propose a
reference architecture for datacenter schedulers. The architecture
follows five design principles: components with clearly distinct
responsibilities, grouping of related components where possible,
separation of mechanism from policy, scheduling as complex
workflow, and hierarchical multi-scheduler structure. To demon-
strate the validity of the reference architecture, we map to it state-
of-the-art datacenter schedulers. We find scheduler-stages are
commonly underspecified in peer-reviewed publications. Through
trace-based simulation and real-world experiments, we show
underspecification of scheduler-stages can lead to significant
variations in performance.`
        ]
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
