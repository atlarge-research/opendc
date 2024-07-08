Below is the schema for the MultiMetaModel JSON file. This schema can be used to validate a MultiMetaModel setup file.
A setup file can be validated using a JSON schema validator, such as https://www.jsonschemavalidator.net/.

```json
{
    "$schema": "OpenDC/MultiMetaModel",
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
            "type": "string",
            "required": true
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
            "type": "number",
            "minimum": 0
        },
        "y_max": {
            "type": "number",
            "minimum": 0
        }
    }
}

```
