import PropTypes from 'prop-types'
import React from 'react'
import classNames from 'classnames'
import { Container } from 'reactstrap'
import { contentSection } from './ContentSection.module.scss'

const ContentSection = ({ name, title, children, className }) => (
    <section id={name} className={classNames(className, contentSection)}>
        <Container>
            <h1>{title}</h1>
            {children}
        </Container>
    </section>
)

ContentSection.propTypes = {
    name: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    children: PropTypes.node,
    className: PropTypes.string,
}

export default ContentSection
