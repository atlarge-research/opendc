Below is the schema for the Scenario JSON file. This schema can be used to validate a scenario file.
A scenario file can be validated using a JSON schema validator, such as https://www.jsonschemavalidator.net/.

```json
{
    "$schema": "OpenDC/Scenario",
    "$defs": {
        "topology": {
            "type": "object",
            "properties": {
                "pathToFile": {
                    "type": "string"
                }
            },
            "required": [
                "pathToFile"
            ]
        },
        "workload": {
            "type": "object",
            "properties": {
                "pathToFile": {
                    "type": "string"
                },
                "type": {
                    "type": "string"
                }
            },
            "required": [
                "pathToFile",
                "type"
            ]
        },
        "allocationPolicy": {
            "type": "object",
            "properties": {
                "policyType": {
                    "type": "string"
                }
            },
            "required": [
                "policyType"
            ]
        }
    },
    "properties": {
        "name": {
            "type": "string"
        },
        "topologies": {
            "type": "array",
            "items": {
                "$ref": "#/$defs/topology"
            },
            "minItems": 1
        },
        "workloads": {
            "type": "array",
            "items": {
                "$ref": "#/$defs/workload"
            },
            "minItems": 1
        },
        "allocationPolicies": {
            "type": "array",
            "items": {
                "$ref": "#/$defs/allocationPolicy"
            },
            "minItems": 1
        },
        "runs": {
            "type": "integer"
        }
    },
    "required": [
        "topologies",
        "workloads",
        "allocationPolicies",
    ]
}
```
