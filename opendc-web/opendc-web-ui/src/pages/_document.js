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

import Document, { Html, Head, Main, NextScript } from 'next/document'

class OpenDCDocument extends Document {
    render() {
        return (
            <Html lang="en">
                <Head>
                    <meta charSet="utf-8" />
                    <meta name="theme-color" content="#00A6D6" />
                    <meta
                        name="description"
                        content="Collaborative Datacenter Simulation and Exploration for Everybody"
                    />
                    <meta name="author" content="@Large Research" />
                    <meta
                        name="keywords"
                        content="OpenDC, Datacenter, Simulation, Simulator, Collaborative, Distributed, Cluster"
                    />
                    <link rel="manifest" href="/manifest.json" />
                    <link rel="shortcut icon" href="/favicon.ico" />

                    {/* Twitter Card data */}
                    <meta name="twitter:card" content="summary" />
                    <meta name="twitter:site" content="@LargeResearch" />
                    <meta name="twitter:title" content="OpenDC" />
                    <meta
                        name="twitter:description"
                        content="Collaborative Datacenter Simulation and Exploration for Everybody"
                    />
                    <meta name="twitter:creator" content="@LargeResearch" />
                    <meta name="twitter:image" content="http://opendc.org/img/logo.png" />

                    {/* OpenGraph meta tags */}
                    <meta property="og:title" content="OpenDC" />
                    <meta property="og:site_name" content="OpenDC" />
                    <meta property="og:type" content="website" />
                    <meta property="og:image" content="http://opendc.org/img/logo.png" />
                    <meta property="og:url" content="http://opendc.org/" />
                    <meta
                        property="og:description"
                        content="OpenDC provides collaborative online datacenter modeling, diverse and effective datacenter simulation, and exploratory datacenter performance feedback."
                    />
                    <meta property="og:locale" content="en_US" />
                </Head>
                <body>
                    <Main />
                    <NextScript />
                </body>
            </Html>
        )
    }
}

export default OpenDCDocument
