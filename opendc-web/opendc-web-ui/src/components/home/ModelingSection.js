import React from 'react'
import ScreenshotSection from './ScreenshotSection'

const ModelingSection = ({ className }) => (
    <ScreenshotSection
        name="modeling"
        title="Datacenter Modeling"
        imageUrl="/img/screenshot-construction.png"
        caption="Building a datacenter in OpenDC"
        imageIsRight={true}
        className={className}
    >
        <h3>Collaboratively...</h3>
        <ul>
            <li>Model DC layout, and room locations and types</li>
            <li>Place racks in rooms and nodes in racks</li>
            <li>Add real-world CPU, GPU, memory, storage and network units to each node</li>
            <li>Select from diverse scheduling policies</li>
        </ul>
    </ScreenshotSection>
)

export default ModelingSection
