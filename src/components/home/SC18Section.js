import React from "react";
import ContentSection from "./ContentSection";
import "./SC18Section.css";

const SC18Section = () => (
  <ContentSection name="sc18" title="OpenDC @ SC18">
    <div className="text-center lead mt-3">
      Our research on a reference architecture for datacenter scheduling will be presented at SC18 (Dallas, TX)! Join our presentation on
      Wednesday, November 14, at 1:30pm, in the Cloud and Distributed Computing track.
    </div>
    <div className="text-center">
      <a href="http://bit.ly/sc18-scheduling" className="btn btn-primary mt-4">
        Read our Technical Report here
      </a>
    </div>
  </ContentSection>
);

export default SC18Section;
