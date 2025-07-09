OpenDC offers to model the overhead of virtualization in a GPU. Overhead is modelled as decrease of supply.
Three different models are available to represent the performance overhead of virtualization in a GPU as described below. 
It is not required to define a virtualization overhead model for a GPU, in which case the GPU is assumed to be used directly by the VM without any overhead.


## No Overhead

This model assumes that there is no overhead when using a GPU.
This type of overhead occurs when GPU pass-through or Para or Full virtualization is used.
In this case, the GPU is used directly by the VM and there is no overhead.

```` json
"virtualizationOverHeadModel": {
    "type": "NONE"
}
````

## Constant Overhead

The constant overhead model assumes that there is a fixed percentage of performance overhead when using a GPU. 
The overhead can be customized by setting the `percentageOverhead` parameter.

``` json
"virtualizationOverHeadModel": {
    "type": "CONSTANT",
    "percentageOverhead": 0.25
}
```

## Share Based Overhead

The share based overhead model assumes that the performance overhead is based on the number of VMs sharing the GPU. 
This is based on the insights of the paper by Garg et al. [Empirical Analysis of Hardware-Assisted GPU Virtualization](https://doi.org/10.1109/HiPC.2019.00054).

``` json
"virtualizationOverHeadModel": {
    "type": "SHARE_BASED"
}
```
