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
import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { finishRoomEdit, startRoomEdit } from '../../../../redux/actions/topology/building'
import CheckIcon from '@patternfly/react-icons/dist/js/icons/check-icon'
import PencilAltIcon from '@patternfly/react-icons/dist/js/icons/pencil-alt-icon'
import { Button } from '@patternfly/react-core'

function EditRoomContainer({ roomId }) {
    const isEditing = useSelector((state) => state.construction.currentRoomInConstruction !== '-1')
    const isInRackConstructionMode = useSelector((state) => state.construction.inRackConstructionMode)

    const dispatch = useDispatch()
    const onEdit = () => dispatch(startRoomEdit(roomId))
    const onFinish = () => dispatch(finishRoomEdit())

    return isEditing ? (
        <Button variant="tertiary" icon={<CheckIcon />} isBlock onClick={onFinish} className="pf-u-mb-sm">
            Finish editing room
        </Button>
    ) : (
        <Button
            variant="tertiary"
            icon={<PencilAltIcon />}
            isBlock
            disabled={isInRackConstructionMode}
            onClick={() => (isInRackConstructionMode ? undefined : onEdit())}
            className="pf-u-mb-sm"
        >
            Edit the tiles of this room
        </Button>
    )
}

EditRoomContainer.propTypes = {
    roomId: PropTypes.string.isRequired,
}

export default EditRoomContainer
