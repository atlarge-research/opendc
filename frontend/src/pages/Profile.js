import React from 'react'
import DocumentTitle from 'react-document-title'
import { connect } from 'react-redux'
import { openDeleteProfileModal } from '../actions/modals/profile'
import DeleteProfileModal from '../containers/modals/DeleteProfileModal'
import AppNavbarContainer from '../containers/navigation/AppNavbarContainer'

const ProfileContainer = ({ onDelete }) => (
    <DocumentTitle title="My Profile - OpenDC">
        <div className="full-height">
            <AppNavbarContainer fullWidth={false}/>
            <div className="container text-page-container full-height">
                <button className="btn btn-danger mb-2 ml-auto mr-auto" style={{ maxWidth: 300 }} onClick={onDelete}>
                    Delete my account on OpenDC
                </button>
                <p className="text-muted text-center">
                    This does not delete your Google account, but simply disconnects it from the OpenDC platform and
                    deletes any project info that is associated with you (projects you own and any authorizations
                    you may have on other projects).
                </p>
            </div>
            <DeleteProfileModal/>
        </div>
    </DocumentTitle>
)

const mapDispatchToProps = (dispatch) => {
    return {
        onDelete: () => dispatch(openDeleteProfileModal()),
    }
}

const Profile = connect(undefined, mapDispatchToProps)(ProfileContainer)

export default Profile
