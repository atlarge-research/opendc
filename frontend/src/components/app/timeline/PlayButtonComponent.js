import React from "react";

const PlayButtonComponent = ({
  isPlaying,
  currentTick,
  lastSimulatedTick,
  onPlay,
  onPause
}) => (
  <div
    className="play-btn"
    onClick={() => {
      if (isPlaying) {
        onPause();
      } else {
        if (currentTick !== lastSimulatedTick) {
          onPlay();
        }
      }
    }}
  >
    {isPlaying ? (
      <span className="fa fa-pause" />
    ) : (
      <span className="fa fa-play" />
    )}
  </div>
);

export default PlayButtonComponent;
