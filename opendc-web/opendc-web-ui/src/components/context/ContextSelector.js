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
import { ContextSelector as PFContextSelector, ContextSelectorItem } from '@patternfly/react-core'
import { useMemo, useState } from 'react'
import { contextSelector } from './ContextSelector.module.scss'

function ContextSelector({ activeItem, items, onSelect, onToggle, isOpen, label }) {
    const [searchValue, setSearchValue] = useState('')
    const filteredItems = useMemo(
        () => items.filter(({ name }) => name.toLowerCase().indexOf(searchValue.toLowerCase()) !== -1) || items,
        [items, searchValue]
    )

    return (
        <PFContextSelector
            className={contextSelector}
            toggleText={activeItem ? `${label}: ${activeItem.name}` : label}
            onSearchInputChange={(value) => setSearchValue(value)}
            searchInputValue={searchValue}
            isOpen={isOpen}
            onToggle={(_, isOpen) => onToggle(isOpen)}
            onSelect={(event) => {
                const targetId = +event.target.value
                const target = items.find((item) => item.id === targetId)

                onSelect(target)
                onToggle(!isOpen)
            }}
        >
            {filteredItems.map((item) => (
                <ContextSelectorItem key={item.id} value={item.id}>
                    {item.name}
                </ContextSelectorItem>
            ))}
        </PFContextSelector>
    )
}

const Item = PropTypes.shape({
    id: PropTypes.any.isRequired,
    name: PropTypes.string.isRequired,
})

ContextSelector.propTypes = {
    activeItem: Item,
    items: PropTypes.arrayOf(Item).isRequired,
    onSelect: PropTypes.func.isRequired,
    onToggle: PropTypes.func.isRequired,
    isOpen: PropTypes.bool,
    label: PropTypes.string,
}

export default ContextSelector
