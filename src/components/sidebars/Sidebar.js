import classNames from "classnames";
import React from "react";
import "./Sidebar.css";

const Sidebar = ({isRight, children}) => (
    <div
        className={classNames("sidebar p-3 h-100", {"sidebar-right": isRight})}
        onWheel={e => e.stopPropagation()}
    >
        {children}
    </div>
);

export default Sidebar;
