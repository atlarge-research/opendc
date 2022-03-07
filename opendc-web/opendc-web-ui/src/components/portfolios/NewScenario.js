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
import { PlusIcon } from '@patternfly/react-icons'
import { Button } from '@patternfly/react-core'
import { useState } from 'react'
import { useNewScenario } from '../../data/project'
import NewScenarioModal from './NewScenarioModal'

function NewScenario({ projectId, portfolioId }) {
    const [isVisible, setVisible] = useState(false)
    const { mutate: addScenario } = useNewScenario()

    const onSubmit = (projectId, portfolioNumber, data) => {
        addScenario({ projectId, portfolioNumber, data })
        setVisible(false)
    }

    return (
        <>
            <Button icon={<PlusIcon />} isSmall onClick={() => setVisible(true)}>
                New Scenario
            </Button>
            <NewScenarioModal
                projectId={projectId}
                portfolioId={portfolioId}
                isOpen={isVisible}
                onSubmit={onSubmit}
                onCancel={() => setVisible(false)}
            />
        </>
    )
}

NewScenario.propTypes = {
    projectId: PropTypes.number,
    portfolioId: PropTypes.number,
}

export default NewScenario
