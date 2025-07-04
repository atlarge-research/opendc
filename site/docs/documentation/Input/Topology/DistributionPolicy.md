The Distribution Policy is used to determine how resources are distributed at a host, across multiple Virtual Machines (VMs).
OpenDC supports multiple distribution policies, describe as below, each extending a FlowDistributor class. 


## Best Effort Distribution Policy

The BestEffortFlowDistributor implements a sophisticated time-sliced round-robin approach designed to maximize resource utilization while maintaining reasonable fairness over time. 
The distributor uses a configurable time interval to advance its round-robin index, ensuring that allocation priority rotates among consumers.
It prioritizes already active suppliers to reduce activation overhead and implements a two-phase allocation strategy: first satisfying demands in round-robin order, then optimizing remaining capacity distribution.
The update interval length parameter defines how long each round in the round-robin cycle lasts.
This policy is heavily inspired by the best effort GPU scheduling policy used in NVIDIA's vGPU scheduler.

Example
``` json
"gpuDistributionPolicy": {
    "type": "BEST_EFFORT",
    "updateIntervalLength": 60000
}
```

## Equal Share Distribution Policy

The EqualShareFlowDistributor implements the simplest distribution strategy by dividing total capacity equally among all consumers,
completely ignoring individual demand levels. This distributor calculates a fixed equal share (totalSupply / numberOfConsumers) and allocates this amount to every consumer regardless of their actual needs.
The algorithm is deterministic and predictable, making it ideal for scenarios where uniform resource access is more important than efficiency.
This policy is heavily inspired by the equal share GPU scheduling policy used in NVIDIA's vGPU scheduler.

Example:
``` json
"gpuDistributionPolicy": {
  "type": "EQUAL_SHARE"
}
```

## First Fit Distribution Policy
This distributor allocates resources to consumers based on the first available supply that meets their demand.
It does not attempt to balance loads or optimize resource usage beyond the first fit principle.
It tries to place demands on already existing supplies before involving another supplier.
It assumes that the resource can be partitioned, if one supplier cannot satisfy the demand, it will try to combine multiple suppliers.


Example:
``` json
"gpuDistributionPolicy": {
"type": "FIRST_FIT"
}
```

## Fixed Share Distribution Policy

This distributor allocates a dedicated, consistent portion of the requested resources to each VM (consumer), 
ensuring predictable availability and stable performance. Each active consumer receives a fixed share of the total resource capacity, regardless of their individual demand or the demand of other consumers.
Key characteristics:
- Each consumer gets a fixed percentage of total resource capacity when active
- Unused shares (from inactive consumers) remain unallocated, not redistributed
- Performance remains consistent and predictable for each consumer
- Share allocation is based on maximum supported consumers, not currently active ones  

The share ratio is defined as a percentage of the total resource capacity, and it is applied uniformly across all active suppliers.
This policy is heavily inspired by the fixed share GPU scheduling policy used in NVIDIA's vGPU scheduler.

Example:
``` json
"gpuDistributionPolicy": {
    "type": "FIXED_SHARE",
    "shareRatio": 0.5
}
```

## Max Min Fairness Distribution Policy

The MaxMinFairnessFlowDistributor implements the max-min fairness algorithm,
which prioritizes satisfying smaller demands first to ensure no consumer is completely starved of resources.
The algorithm sorts consumers by their demand levels and allocates resources incrementally, ensuring that the minimum allocation received by any consumer is maximized. 
It maintains an overload state to track when total demand exceeds available supply and applies different distribution strategies accordingly.
**This is the default policy in OpenDC**.

Example:
``` json
"gpuDistributionPolicy": {
  "type": "MAX_MIN_FAIRNESS"
}
```
