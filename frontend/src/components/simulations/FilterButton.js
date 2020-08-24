import classNames from "classnames";
import PropTypes from "prop-types";
import React from "react";

const FilterButton = ({ active, children, onClick }) => (
  <button
    className={classNames("btn btn-secondary", { active: active })}
    onClick={() => {
      if (!active) {
        onClick();
      }
    }}
  >
    {children}
  </button>
);

FilterButton.propTypes = {
  active: PropTypes.bool.isRequired,
  children: PropTypes.node.isRequired,
  onClick: PropTypes.func.isRequired
};

export default FilterButton;
