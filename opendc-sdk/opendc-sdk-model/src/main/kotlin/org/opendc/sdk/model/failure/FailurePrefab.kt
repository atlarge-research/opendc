/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.failure

import kotlinx.serialization.Serializable

/**
 * A named, built-in failure model derived from an empirical availability trace fitted to a
 * statistical distribution. The prefix identifies the source dataset and the suffix the fitted
 * distribution family (Exp, Wbl, LogN, Gam).
 */
@Serializable
public enum class FailurePrefab {
    G5k06Exp,
    G5k06Wbl,
    G5k06LogN,
    G5k06Gam,
    Lanl05Exp,
    Lanl05Wbl,
    Lanl05LogN,
    Lanl05Gam,
    Ldns04Exp,
    Ldns04Wbl,
    Ldns04LogN,
    Ldns04Gam,
    Microsoft99Exp,
    Microsoft99Wbl,
    Microsoft99LogN,
    Microsoft99Gam,
    Nd07cpuExp,
    Nd07cpuWbl,
    Nd07cpuLogN,
    Nd07cpuGam,
    Overnet03Exp,
    Overnet03Wbl,
    Overnet03LogN,
    Overnet03Gam,
    Pl05Exp,
    Pl05Wbl,
    Pl05LogN,
    Pl05Gam,
    Skype06Exp,
    Skype06Wbl,
    Skype06LogN,
    Skype06Gam,
    Websites02Exp,
    Websites02Wbl,
    Websites02LogN,
    Websites02Gam,
}
