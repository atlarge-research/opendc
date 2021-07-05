import PropTypes from 'prop-types'
import React from 'react'
import { Project } from '../../shapes'
import ProjectRow from './ProjectRow'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faQuestionCircle } from '@fortawesome/free-solid-svg-icons'

const ProjectList = ({ projects }) => {
    return (
        <div className="vertically-expanding-container">
            {projects.length === 0 ? (
                <div className="alert alert-info">
                    <FontAwesomeIcon icon={faQuestionCircle} className="info-icon mr-2" />
                    <strong>No projects here yet...</strong> Add some with the &lsquo;New Project&rsquo; button!
                </div>
            ) : (
                <table className="table table-striped">
                    <thead>
                        <tr>
                            <th>Project name</th>
                            <th>Last edited</th>
                            <th>Access rights</th>
                            <th />
                        </tr>
                    </thead>
                    <tbody>
                        {projects.map((project) => (
                            <ProjectRow project={project} key={project._id} />
                        ))}
                    </tbody>
                </table>
            )}
        </div>
    )
}

ProjectList.propTypes = {
    projects: PropTypes.arrayOf(Project).isRequired,
}

export default ProjectList
