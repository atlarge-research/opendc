import React from 'react'
import classNames from 'classnames'
import { Container } from 'reactstrap'
import PropTypes from 'prop-types'
import './ContentSection.sass'

const ContentSection = ({ name, title, children, className }) => (
    <section id={name} className={classNames(className, name + '-section', 'content-section')}>
        <Container>
            <h1>{title}</h1>
            {children}
        </Container>
    </section>
)

ContentSection.propTypes = {
    name: PropTypes.string.isRequired,
}

export default ContentSection
