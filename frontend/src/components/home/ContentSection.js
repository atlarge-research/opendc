import classNames from 'classnames'
import PropTypes from 'prop-types'
import React from 'react'
import './ContentSection.sass'

const ContentSection = ({ name, title, children }) => (
    <div id={name} className={classNames(name + '-section', 'content-section')}>
        <div className="container">
            <h1>{title}</h1>
            {children}
        </div>
    </div>
)

ContentSection.propTypes = {
    name: PropTypes.string.isRequired,
}

export default ContentSection
