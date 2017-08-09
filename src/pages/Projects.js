import React from 'react';
import {connect} from "react-redux";
import {addProject, openNewProjectModal} from "../actions/projects";
import Navbar from "../components/navigation/Navbar";
import ProjectFilterPanel from "../components/projects/FilterPanel";
import NewProjectButton from "../components/projects/NewProjectButton";
import NewProjectModal from "../containers/projects/NewProjectModal";
import VisibleProjectList from "../containers/projects/VisibleProjectAuthList";
import "./Projects.css";

class Projects extends React.Component {
    componentDidMount() {
        // TODO perform initial fetch
    }

    onInputSubmission(text) {
        this.props.dispatch(addProject(text));
    }

    render() {
        return (
            <div className="full-height">
                <Navbar/>
                <div className="container project-page-container full-height">
                    <ProjectFilterPanel/>
                    <VisibleProjectList/>
                    <NewProjectButton onClick={() => {this.props.dispatch(openNewProjectModal())}}/>
                </div>
                <NewProjectModal/>
            </div>
        );
    }
}

export default connect()(Projects);
