import React from 'react'
import ScreenshotSection from './ScreenshotSection'

const ModelingSection = () => (
    <ScreenshotSection
        name="simulation"
        title="Datacenter Simulation"
        imageUrl="/img/screenshot-simulation-zoom.png"
        caption="Running an experiment in OpenDC"
        imageIsRight={false}
    >
        <h3>Working with OpenDC:</h3>
        <ul>
            <li>Seamlessly switch between construction and simulation modes</li>
            <li>Choose one of several predefined workloads (Big Data, Bag of Tasks, Hadoop, etc.)</li>
            <li>Play, pause, and skip around the informative simulation timeline</li>
            <li>Visualize and demo live</li>
        </ul>
    </ScreenshotSection>
)

export default ModelingSection
