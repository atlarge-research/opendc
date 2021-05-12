import React, { useState } from 'react'
import Head from 'next/head'
import { useDispatch } from 'react-redux'
import AppNavbarContainer from '../containers/navigation/AppNavbarContainer'
import ConfirmationModal from '../components/modals/ConfirmationModal'
import { deleteCurrentUser } from '../actions/users'
import { useRequireAuth } from '../auth/hook'

const Profile = () => {
    useRequireAuth()

    const dispatch = useDispatch()

    const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
    const onDelete = (isConfirmed) => {
        if (isConfirmed) {
            dispatch(deleteCurrentUser())
        }
        setDeleteModalOpen(false)
    }

    return (
        <>
            <Head>
                <title>My Profile - OpenDC</title>
            </Head>
            <div className="full-height">
                <AppNavbarContainer fullWidth={false} />
                <div className="container text-page-container full-height">
                    <button
                        className="btn btn-danger mb-2 ml-auto mr-auto"
                        style={{ maxWidth: 300 }}
                        onClick={() => setDeleteModalOpen(true)}
                    >
                        Delete my account on OpenDC
                    </button>
                    <p className="text-muted text-center">
                        This does not delete your Google account, but simply disconnects it from the OpenDC platform and
                        deletes any project info that is associated with you (projects you own and any authorizations
                        you may have on other projects).
                    </p>
                </div>
                <ConfirmationModal
                    title="Delete my account"
                    message="Are you sure you want to delete your OpenDC account?"
                    show={isDeleteModalOpen}
                    callback={onDelete}
                />
            </div>
        </>
    )
}

export default Profile
