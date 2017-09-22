import React from "react";

const PlayButtonComponent = ({isPlaying, onPlay, onPause}) => (
    <div className="play-btn" onClick={() => isPlaying ? onPause() : onPlay()}>
        {isPlaying ?
            <span className="fa fa-pause"/> :
            <span className="fa fa-play"/>
        }
    </div>
);

export default PlayButtonComponent;
