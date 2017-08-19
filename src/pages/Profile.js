import React from 'react';
import {connect} from "react-redux";
import {openDeleteProfileModal} from "../actions/profile";
import Navbar from "../components/navigation/Navbar";
import Login from "../containers/auth/Login";
import DeleteProfileModal from "../containers/profile/DeleteProfileModal";
import "./Profile.css";

const ProfileContainer = ({onDelete}) => (
    <div className="full-height">
        <Navbar/>
        <div className="container profile-page-container full-height">
            <h2>Profile Settings</h2>
            <button className="btn btn-danger" onClick={onDelete}>Delete my account on OpenDC</button>
            <p>
                This does not delete your Google account, it simply disconnects it from the OpenDC app and deletes any
                simulation info that is associated with you (simulations you own, and any authorizations you may
                have on other projects).
            </p>
        </div>
        <DeleteProfileModal/>
        <Login visible={false}/>
    </div>
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
