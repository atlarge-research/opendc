import PropTypes from 'prop-types'
import classNames from 'classnames'
import React, { useState } from 'react'
import { collapseButton, collapseButtonRight, sidebar, sidebarRight } from './Sidebar.module.scss'

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
                <span className="fa fa-angle-left" title={isRight ? 'Expand' : 'Collapse'} />
            ) : (
                <span className="fa fa-angle-right" title={isRight ? 'Collapse' : 'Expand'} />
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
}

export default Sidebar
