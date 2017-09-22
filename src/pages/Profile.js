import React from 'react';
import DocumentTitle from "react-document-title";
import {connect} from "react-redux";
import {openDeleteProfileModal} from "../actions/modals/profile";
import AppNavbar from "../components/navigation/AppNavbar";
import DeleteProfileModal from "../containers/modals/DeleteProfileModal";

const ProfileContainer = ({onDelete}) => (
    <DocumentTitle title="My Profile - OpenDC">
        <div className="full-height">
            <AppNavbar inSimulation={false}/>
            <div className="container text-page-container full-height">
                <button className="btn btn-danger" onClick={onDelete}>Delete my account on OpenDC</button>
                <p>
                    This does not delete your Google account, it simply disconnects it from the OpenDC app and deletes
                    any
                    simulation info that is associated with you (simulations you own, and any authorizations you may
                    have on other projects).
                </p>
            </div>
            <DeleteProfileModal/>
        </div>
    </DocumentTitle>
);

const mapDispatchToProps = dispatch => {
    return {
        onDelete: () => dispatch(openDeleteProfileModal()),
    };
};

const Profile = connect(
    undefined,
    mapDispatchToProps
)(ProfileContainer);

export default Profile;
