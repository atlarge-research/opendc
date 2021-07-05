from marshmallow import Schema, fields

from opendc.models.model import Model


class TraceSchema(Schema):
    """Schema for a Trace."""
    _id = fields.String(dump_only=True)
    name = fields.String()
    type = fields.String()


class Trace(Model):
    """Model representing a Trace."""

    collection_name = 'traces'
