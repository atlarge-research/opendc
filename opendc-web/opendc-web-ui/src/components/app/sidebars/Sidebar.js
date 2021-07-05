import PropTypes from 'prop-types'
import classNames from 'classnames'
import React, { useState } from 'react'
import { collapseButton, collapseButtonRight, sidebar, sidebarRight } from './Sidebar.module.scss'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleLeft, faAngleRight } from '@fortawesome/free-solid-svg-icons'

function Sidebar({ isRight, collapsible = true, children }) {
    const [isCollapsed, setCollapsed] = useState(false)

    const button = (
        <div
            className={classNames(collapseButton, {
                [collapseButtonRight]: isRight,
            })}
            onClick={() => setCollapsed(!isCollapsed)}
        >
            {(isCollapsed && isRight) || (!isCollapsed && !isRight) ? (
                <FontAwesomeIcon icon={faAngleLeft} title={isRight ? 'Expand' : 'Collapse'} />
            ) : (
                <FontAwesomeIcon icon={faAngleRight} title={isRight ? 'Collapse' : 'Expand'} />
            )}
        </div>
    )

    if (isCollapsed) {
        return button
    }
    return (
        <div
            className={classNames(`${sidebar} p-3 h-100`, {
                [sidebarRight]: isRight,
            })}
            onWheel={(e) => e.stopPropagation()}
        >
            {children}
            {collapsible && button}
        </div>
    )
}

Sidebar.propTypes = {
    isRight: PropTypes.bool.isRequired,
    collapsible: PropTypes.bool,
    children: PropTypes.node,
}

export default Sidebar
