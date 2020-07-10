import PropTypes from 'prop-types'
import classNames from 'classnames'
import React from 'react'
import './Sidebar.css'

class Sidebar extends React.Component {
    static propTypes = {
        isRight: PropTypes.bool.isRequired,
        collapsible: PropTypes.bool,
    }

    static defaultProps = {
        collapsible: true
    }

    state = {
        collapsed: false,
    }

    render() {
        const collapseButton = (
            <div
                className={classNames('sidebar-collapse-button', {
                    'sidebar-collapse-button-right': this.props.isRight,
                })}
                onClick={() => this.setState({ collapsed: !this.state.collapsed })}
            >
                {(this.state.collapsed && this.props.isRight) ||
                (!this.state.collapsed && !this.props.isRight) ? (
                    <span
                        className="fa fa-angle-left"
                        title={this.props.isRight ? 'Expand' : 'Collapse'}
                    />
                ) : (
                    <span
                        className="fa fa-angle-right"
                        title={this.props.isRight ? 'Collapse' : 'Expand'}
                    />
                )}
            </div>
        )

        if (this.state.collapsed) {
            return collapseButton
        }
        return (
            <div
                className={classNames('sidebar p-3 h-100', {
                    'sidebar-right': this.props.isRight,
                })}
                onWheel={e => e.stopPropagation()}
            >
                {this.props.children}
                {this.props.collapsible && collapseButton}
            </div>
        )
    }
}

export default Sidebar
