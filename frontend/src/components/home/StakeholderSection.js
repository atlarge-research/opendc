import React from "react";
import ContentSection from "./ContentSection";

const Stakeholder = ({ name, title, subtitle }) => (
  <div className="col-xl-4 col-lg-4 col-md-4 col-sm-6 col-6">
    <img
      src={"img/stakeholders/" + name + ".png"}
      className="col-xl-3 col-lg-4 col-md-4 col-sm-4 col-4 img-fluid"
      alt={title}
    />
    <div className="text-center mt-2">
      <h4>{title}</h4>
      <p>{subtitle}</p>
    </div>
  </div>
);

const StakeholderSection = () => (
  <ContentSection name="stakeholders" title="Stakeholders">
    <div className="row justify-content-center">
      <Stakeholder
        name="Manager"
        title="Managers"
        subtitle="Seeing is deciding"
      />
      <Stakeholder name="Sales" title="Sales" subtitle="Demo concepts" />
      <Stakeholder name="Developer" title="DevOps" subtitle="Develop & tune" />
      <Stakeholder
        name="Researcher"
        title="Researchers"
        subtitle="Understand & design"
      />
      <Stakeholder
        name="Student"
        title="Students"
        subtitle="Grasp complex concepts"
      />
    </div>
  </ContentSection>
);

export default StakeholderSection;
