import classNames from "classnames";
import PropTypes from "prop-types";
import React from "react";
import "./ContentSection.css";

const ContentSection = ({name, children}) => (
    <div id={name} className={classNames(name + "-section", "content-section")}>
        <div className="container">
            <div className="row">
                {children}
            </div>
        </div>
    </div>
);

ContentSection.propTypes = {
    name: PropTypes.string.isRequired,
};

export default ContentSection;
