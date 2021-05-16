/*
 * Copyright (c) 2021 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import React, { useState } from 'react'
import Head from 'next/head'
import { useDispatch } from 'react-redux'
import AppNavbarContainer from '../containers/navigation/AppNavbarContainer'
import ConfirmationModal from '../components/modals/ConfirmationModal'
import { deleteCurrentUser } from '../redux/actions/users'
import { useRequireAuth } from '../auth'

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
