import classNames from 'classnames'
import React from 'react'
import ContentSection from './ContentSection'
import './ScreenshotSection.css'

const ScreenshotSection = ({
                               name,
                               title,
                               imageUrl,
                               caption,
                               imageIsRight,
                               children,
                           }) => (
    <ContentSection name={name} title={title}>
        <div className="row">
            <div
                className={classNames(
                    'col-xl-5 col-lg-5 col-md-5 col-sm-12 col-12 text-left',
                    { 'order-1': !imageIsRight },
                )}
            >
                {children}
            </div>
            <div className="col-xl-7 col-lg-7 col-md-7 col-sm-12 col-12">
                <img src={imageUrl} className="col-12 screenshot" alt={caption}/>
                <div className="row text-muted justify-content-center">{caption}</div>
            </div>
        </div>
    </ContentSection>
)

export default ScreenshotSection
