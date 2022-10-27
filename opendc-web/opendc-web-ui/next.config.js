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

// PatternFly 4 uses global CSS imports in its distribution files. Therefore,
// we need to transpile the modules before we can use them.
const { withGlobalCss } = require('next-global-css')
const { PHASE_DEVELOPMENT_SERVER } = require("next/constants");
const withConfig = withGlobalCss()

module.exports = (phase) => withConfig({
    basePath: process.env.NEXT_BASE_PATH && '/' + process.env.NEXT_BASE_PATH,
    reactStrictMode: true,
    distDir: phase === PHASE_DEVELOPMENT_SERVER ? 'build/next-dev' : 'build/next',
    async redirects() {
        return [
            {
                source: '/',
                destination: '/projects',
                permanent: false
            }
        ]
    },
    images: { unoptimized: true }
})
