import React from 'react'
import ScreenshotSection from './ScreenshotSection'

const ModelingSection = () => (
    <ScreenshotSection
        name="project"
        title="Datacenter Simulation"
        imageUrl="/img/screenshot-simulation.png"
        caption="Running an experiment in OpenDC"
        imageIsRight={false}
    >
        <h3>Working with OpenDC:</h3>
        <ul>
            <li>Seamlessly switch between construction and simulation modes</li>
            <li>
                Choose one of several predefined workloads (Business Critical, Workflows, Machine Learning, Serverless,
                etc.)
            </li>
            <li>Compare datacenter topologies using automated plots and visual summaries</li>
        </ul>
    </ScreenshotSection>
)

export default ModelingSection
