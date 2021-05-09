import classNames from 'classnames'
import React from 'react'
import { Row, Col } from 'reactstrap'
import ContentSection from './ContentSection'
import './ScreenshotSection.sass'

const ScreenshotSection = ({ name, title, imageUrl, caption, imageIsRight, children }) => (
    <ContentSection name={name} title={title}>
        <Row>
            <Col
                xl="5"
                lg="5"
                md="5"
                sm="!2"
                className={classNames('text-left my-auto', {
                    'order-1': !imageIsRight,
                })}
            >
                {children}
            </Col>
            <Col xl="7" lg="7" md="7" sm="12">
                <img src={imageUrl} className="col-12 screenshot" alt={caption} />
                <Row className="text-muted justify-content-center">{caption}</Row>
            </Col>
        </Row>
    </ContentSection>
)

export default ScreenshotSection
