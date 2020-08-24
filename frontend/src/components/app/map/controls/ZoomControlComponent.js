import React from "react";

const ZoomControlComponent = ({ zoomInOnCenter }) => {
  return (
    <span>
      <button
        className="btn btn-default btn-circle btn-sm mr-1"
        title="Zoom in"
        onClick={() => zoomInOnCenter(true)}
      >
        <span className="fa fa-plus" />
      </button>
      <button
        className="btn btn-default btn-circle btn-sm mr-1"
        title="Zoom out"
        onClick={() => zoomInOnCenter(false)}
      >
        <span className="fa fa-minus" />
      </button>
    </span>
  );
};

export default ZoomControlComponent;
