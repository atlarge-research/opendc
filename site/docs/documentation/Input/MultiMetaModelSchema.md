Below is the schema for the MultiMetaModel JSON file. This schema can be used to validate a MultiMetaModel setup file.
A setup file can be validated using a JSON schema validator, such as https://www.jsonschemavalidator.net/.

```json
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
        "multimodel": {
            "type": "boolean",
            "default": true
        },
        "metamodel": {
            "type": "boolean",
            "default": true
        },
        "metric": {
            "type": "string"
        },
        "window_size": {
            "type": "integer",
            "default": 1,
            "minimum": 1
        },
        "aggregation_function": {
            "type": "string",
            "default": "mean",
            "enum": ["mean", "median"]
        },
        "metamodel_function": {
            "type": "string",
            "default": "mean",
            "enum": ["mean", "median"]
        },
        "samples_per_minute": {
            "type": "number",
            "default": 1,
            "minimum": 0.0001
        },
        "plot_type": {
            "type": "string",
            "default": "time_series",
            "enum": ["time_series", "cumulative_total", "cumulative_time_series"]
        },
        "plot_title": {
            "type": "string",
            "default": ""
        },
        "x_label": {
            "type": "string",
            "default": "Time"
        },
        "y_label": {
            "type": "string",
            "default": "Metric Unit"
        },
        "y_min": {
            "type": "number"
        },
        "y_max": {
            "type": "number"
        }
    },
    "required": [
        "metric"
    ]
}
```
