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

import PropTypes from 'prop-types'
import React, { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import TrashIcon from '@patternfly/react-icons/dist/js/icons/trash-icon'
import { Button } from '@patternfly/react-core'
import ConfirmationModal from '../../../util/modals/ConfirmationModal'
import { deleteRack } from '../../../../redux/actions/topology/rack'

function DeleteRackContainer({ tileId }) {
    const dispatch = useDispatch()
    const [isVisible, setVisible] = useState(false)
    const rackId = useSelector((state) => state.topology.tiles[tileId].rack)
    const callback = (isConfirmed) => {
        if (isConfirmed) {
            dispatch(deleteRack(tileId, rackId))
        }
        setVisible(false)
    }
    return (
        <>
            <Button variant="danger" icon={<TrashIcon />} isBlock onClick={() => setVisible(true)}>
                Delete this rack
            </Button>
            <ConfirmationModal
                title="Delete this rack"
                message="Are you sure you want to delete this rack?"
                isOpen={isVisible}
                callback={callback}
            />
        </>
    )
}

DeleteRackContainer.propTypes = {
    tileId: PropTypes.string.isRequired,
}

export default DeleteRackContainer
