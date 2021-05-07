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

package org.opendc.experiments.tf20.keras.activations

/**
 * Neural network hyper-parameter, activation function of a node defines the output of that node given an input or
 * set of inputs.
 */
public enum class Activation {
    /**
     * Linear unit. Returns unmodified input.
     *
     * NOTE: Doing nothing useful. Returns to ancient times of linear perceptron.
     */
    Linear,

    /**
     * Sigmoid activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * sigmoid(x) = 1 / (1 + exp(-x))
     * ```
     *
     * For small values (<-5), `sigmoid` returns a value close to zero, and for large values (>5)
     * the result of the function gets close to 1.
     *
     * NOTE: Sigmoid is equivalent to a 2-element ActivationLayer, where the second element is
     * assumed to be zero. The sigmoid function always returns a value between 0 and 1.
     */
    Sigmoid,

    /**
     * Hyperbolic tangent activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * tanh(x) = sinh(x)/cosh(x) = ((exp(x) - exp(-x))/(exp(x) + exp(-x)))
     * ```
     */
    Tanh,

    /**
     * Rectified linear unit (ReLU).
     *
     * With default values, this returns the standard ReLU activation:
     * `max(x, 0)`, the element-wise maximum of 0 and the input tensor.
     */
    Relu,

    /**
     * Computes Rectified Linear 6:
     * ```
     * min(max(features, 0), 6)
     * ```
     * @see <a href="http://www.cs.utoronto.ca/~kriz/conv-cifar10-aug2010.pdf">
     *     Convolutional Deep Belief Networks on CIFAR-10. A. Krizhevsky</a>
     */
    Relu6,

    /**
     * Exponential Linear Unit.
     *
     * The exponential linear unit (ELU) with `alpha > 0` is:
     * `x` if `x > 0` and `alpha * (exp(x) - 1)` if `x < 0`
     *
     * For this implementations alpha is equal to 1.0.
     *
     * The ELU hyperparameter `alpha` controls the value to which an
     * ELU saturates for negative net inputs. ELUs diminish the
     * vanishing gradient effect.
     *
     * ELUs have negative values which pushes the mean of the activations closer to zero.
     *
     * Mean activations that are closer to zero enable faster learning as they
     * bring the gradient closer to the natural gradient.
     *
     * ELUs saturate to a negative value when the argument gets smaller.
     * Saturation means a small derivative which decreases the variation
     * and the information that is propagated to the next layer.
     *
     * @see <a href="https://arxiv.org/abs/1511.07289">Fast and Accurate Deep Network Learning by Exponential Linear Units
     * (ELUs) (Clevert et al, 2016)</a>
     */
    Elu,

    /**
     * Scaled Exponential Linear Unit (SELU).
     *
     * The Scaled Exponential Linear Unit (SELU) activation function is defined as:
     * ```
     *  if x > 0: return scale * x
     *  if x < 0: return scale * alpha * (exp(x) - 1)
     * ```
     * where `alpha` and `scale` are pre-defined constants (`alpha=1.67326324` and `scale=1.05070098`).
     *
     * Basically, the SELU activation function multiplies `scale` (> 1) with the
     * output of the `tf.keras.activations.elu` function to ensure a slope larger
     * than one for positive inputs.
     *
     * @see <a href="https://arxiv.org/abs/1706.02515">Klambauer et al., 2017</a>
     */
    Selu,

    /**
     * ActivationLayer converts a real vector to a vector of categorical probabilities.
     * The elements of the output vector are in range (0, 1) and sum to 1.
     *
     * ActivationLayer is often used as the activation for the last
     * layer of a classification network because the result could be interpreted as
     * a probability distribution.
     */
    Softmax,

    /**
     *
     */
    LogSoftmax,

    /**
     * Exponential activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * exp(x)
     * ```
     */
    Exponential,

    /**
     * Softplus activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * softplus(x) = log(exp(x) + 1)
     * ```
     */
    SoftPlus,

    /***
     * Softsign activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * softsign(x) = x / (abs(x) + 1)
     * ```
     */
    SoftSign,

    /**
     * Hard sigmoid activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * if x < -2.5: return 0
     * if x > 2.5: return 1
     * if -2.5 <= x <= 2.5: return 0.2 * x + 0.5
     * ```
     * A faster approximation of the sigmoid activation.
     */
    HardSigmoid,

    /**
     * Swish activation function.
     *
     * Transforms input 'x' according formula:
     * ```
     * swish(x) = x * sigmoid(x)
     * ```
     *
     * It is a smooth, non-monotonic function that consistently matches
     * or outperforms ReLU on deep networks, it is unbounded above and
     * bounded below.
     *
     * @see <a href="https://arxiv.org/abs/1710.05941">Ramachandran et al., 2017</a>
     */
    Swish;
}
