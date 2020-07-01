import React from 'react'

const IntroSection = () => (
    <section id="intro" className="intro-section">
        <div className="container pt-5 pb-3">
            <div className="row justify-content-center">
                <div className="col-xl-4 col-lg-4 col-md-4 col-sm-8 col-8">
                    <h4>The datacenter (DC) industry...</h4>
                    <ul>
                        <li>Is worth over $15 bn, and growing</li>
                        <li>Has many hard-to-grasp concepts</li>
                        <li>Needs to become accessible to many</li>
                    </ul>
                </div>
                <div className="col-xl-4 col-lg-4 col-md-4 col-sm-8 col-8">
                    <img
                        src="img/datacenter-drawing.png"
                        className="col-12 img-fluid"
                        alt="Schematic top-down view of a datacenter"
                    />
                    <p className="col-12 figure-caption text-center">
                        <a href="http://www.dolphinhosts.co.uk/wp-content/uploads/2013/07/data-centers.gif">
                            Image source
                        </a>
                    </p>
                </div>
                <div className="col-xl-4 col-lg-4 col-md-4 col-sm-8 col-8">
                    <h4>OpenDC provides...</h4>
                    <ul>
                        <li>Collaborative online DC modeling</li>
                        <li>Diverse and effective DC simulation</li>
                        <li>Exploratory DC performance feedback</li>
                    </ul>
                </div>
            </div>
        </div>
    </section>
)

export default IntroSection
