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
import PropTypes from 'prop-types'
import { UploadIcon } from '@patternfly/react-icons'
import { Button } from '@patternfly/react-core'
import { useNewTopology, useTopologies } from '../../data/topology'
import ImportTopologyModal from './ImportTopologyModal'

function ImportTopology({ projectId }) {
    const [isVisible, setVisible] = useState(false)
    const { mutate: addTopology } = useNewTopology()
    const { data: topologies = [] } = useTopologies(projectId, { enabled: isVisible })

    const onSubmit = (topology) => {
        addTopology({ projectId, ...topology })
        setVisible(false)
    }

    return (
        <>
            <Button variant="secondary" icon={<UploadIcon />} isSmall onClick={() => setVisible(true)}>
                Import Topology
            </Button>
            <ImportTopologyModal
                isOpen={isVisible}
                onCancel={() => setVisible(false)}
                onSubmit={onSubmit}
                topologies={topologies}
            />
        </>
    )
}

ImportTopology.propTypes = {
    projectId: PropTypes.number,
}

export default ImportTopology
