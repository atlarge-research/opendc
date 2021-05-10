import React from 'react'
import { Row, Col } from 'reactstrap'
import ContentSection from './ContentSection'
import './ScreenshotSection.sass'

const ScreenshotSection = ({ name, title, imageUrl, caption, imageIsRight, children }) => (
    <ContentSection name={name} title={title}>
        <Row>
            <Col xl="5" lg="5" md="5" sm="12" className={`text-left ${!imageIsRight ? 'order-1' : ''}`}>
                {children}
            </Col>
            <Col xl="7" lg="7" md="7" sm="12">
                <img src={imageUrl} className="col-12 screenshot" alt={caption} />
                <div className="row text-muted justify-content-center">{caption}</div>
            </Col>
        </Row>
    </ContentSection>
)

export default ScreenshotSection
