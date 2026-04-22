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
import NameComponent from '../NameComponent'
import { editDatacenterName } from '../../../../redux/actions/topology/datacenter'

function DatacenterName({ datacenterId }) {
    const { name, id } = useSelector((state) => state.topology.datacenters[datacenterId])
    const allDatacenters = useSelector((state) => Object.values(state.topology.datacenters ?? {}))
    const dispatch = useDispatch()
    const callback = (newName) => {
        const trimmed = newName?.trim()
        if (!trimmed) return
        const isDuplicate = allDatacenters.some((dc) => dc.id !== id && dc.name === trimmed)
        if (!isDuplicate) {
            dispatch(editDatacenterName(id, trimmed))
        }
    }
    return <NameComponent name={name} onEdit={callback} />
}

DatacenterName.propTypes = {
    datacenterId: PropTypes.string.isRequired,
}

export default DatacenterName
