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

import { MutationObserver } from 'react-query'
import { getContext, call } from 'redux-saga/effects'

/**
 * Fetch the query with the specified key.
 */
export function* fetchQuery(key, options) {
    const queryClient = yield getContext('queryClient')
    return yield call([queryClient, queryClient.fetchQuery], key, options)
}

/**
 * Perform a mutation with the specified key.
 */
export function* mutate(key, object, options) {
    const queryClient = yield getContext('queryClient')
    const mutationObserver = new MutationObserver(queryClient, { mutationKey: key })
    return yield call([mutationObserver, mutationObserver.mutate], object, options)
}
