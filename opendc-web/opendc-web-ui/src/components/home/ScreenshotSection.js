import PropTypes from 'prop-types'
import React from 'react'
import Image from 'next/image'
import { Row, Col } from 'reactstrap'
import ContentSection from './ContentSection'
import { screenshot } from './ScreenshotSection.module.scss'

const ScreenshotSection = ({ className, name, title, imageUrl, caption, imageIsRight, children }) => (
    <ContentSection name={name} title={title} className={className}>
        <Row>
            <Col xl="5" lg="5" md="5" sm="12" className={`text-left my-auto ${!imageIsRight ? 'order-1' : ''}`}>
                {children}
            </Col>
            <Col xl="7" lg="7" md="7" sm="12">
                <Image
                    src={imageUrl}
                    className={`col-12 ${screenshot}`}
                    layout="intrinsic"
                    width={635}
                    height={419}
                    alt={caption}
                />
                <div className="row text-muted justify-content-center">{caption}</div>
            </Col>
        </Row>
    </ContentSection>
)

ScreenshotSection.propTypes = {
    className: PropTypes.string,
    name: PropTypes.string,
    title: PropTypes.string,
    imageUrl: PropTypes.string,
    caption: PropTypes.string,
    imageIsRight: PropTypes.bool,
    children: PropTypes.node,
}

export default ScreenshotSection
