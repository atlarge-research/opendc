Below is the schema for the Topology JSON file. This schema can be used to validate a topology file. 
A topology file can be validated using using a JSON schema validator, such as https://www.jsonschemavalidator.net/.

```json
{
  "$schema": "OpenDC/Topology",
  "$defs": {
    "cpu": {
      "description": "definition of a cpu",
      "type": "object",
      "properties": {
        "vendor": {
          "type": "string",
          "default": "unknown"
        },
        "modelName": {
          "type": "string",
          "default": "unknown"
        },
        "arch": {
          "type": "string",
          "default": "unknown"
        },
        "coreCount": {
          "type": "integer"
        },
        "coreSpeed": {
          "description": "The core speed of the cpu in Mhz",
          "type": "number"
        },
        "count": {
          "description": "The amount CPUs of this type present in the cluster",
          "type": "integer"
        }
      },
      "required": [
        "coreCount",
        "coreSpeed"
      ]
    },
    "memory": {
      "type": "object",
      "properties": {
        "vendor": {
          "type": "string",
          "default": "unknown"
        },
        "modelName": {
          "type": "string",
          "default": "unknown"
        },
        "arch": {
          "type": "string",
          "default": "unknown"
        },
        "memorySize": {
          "description": "The amount of the memory in B",
          "type": "integer"
        },
        "memorySpeed": {
          "description": "The speed of the memory in Mhz. Note: currently, this does nothing",
          "type": "number",
          "default": -1
        }
      },
      "required": [
        "memorySize"
      ]
    },
    "powerModel": {
      "type": "object",
      "properties": {
        "modelType": {
          "description": "The type of model used to determine power draw",
          "type": "string"
        },
        "power": {
          "description": "The constant power draw when using the 'constant' power model type in Watt",
          "type": "number",
          "default": 400
        },
        "maxPower": {
          "description": "The power draw of a host when idle in Watt",
          "type": "number"
        },
        "idlePower": {
          "description": "The power draw of a host when using max capacity in Watt",
          "type": "number"
        }
      },
      "required": [
        "modelType",
        "maxPower",
        "idlePower"
      ]
    },
    "host": {
      "type": "object",
      "properties": {
        "name": {
          "type": "string",
          "default": "Host"
        },
        "count": {
          "description": "The amount hosts of this type present in the cluster",
          "type": "integer",
          "default": 1
        },
        "cpus": {
          "type": "array",
          "items": {
            "$ref": "#/$defs/cpu"
          },
          "minItems": 1
        },
        "memory": {
          "$ref": "#/$defs/memory"
        }
      },
      "required": [
        "cpus",
        "memory"
      ]
    },
    "cluster": {
      "type": "object",
      "properties": {
        "name": {
          "type": "string",
          "default": "Cluster"
        },
        "count": {
          "description": "The amount clusters of this type present in the Data center",
          "type": "integer",
          "default": 1
        },
        "hosts": {
          "type": "array",
          "items": {
            "$ref": "#/$defs/host"
          },
          "minItems": 1
        }
      },
      "required": [
        "hosts"
      ]
    }
  },
  "properties": {
    "clusters": {
      "description": "Clusters present in the data center",
      "type": "array",
      "items": {
        "$ref": "#/$defs/cluster"
      },
      "minItems": 1
    }
  },
  "required": [
    "clusters"
  ]
}
```
