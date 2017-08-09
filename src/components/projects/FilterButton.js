import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import "./FilterButton.css";

const FilterButton = ({active, children, onClick}) => (
    <div className={classNames("project-filter-button", {"active": active})}
         onClick={() => {
             if (!active) {
                 onClick();
             }
         }}>
        {children}
    </div>
);

FilterButton.propTypes = {
    active: PropTypes.bool.isRequired,
    children: PropTypes.node.isRequired,
    onClick: PropTypes.func.isRequired
};

export default FilterButton;
