from bson import ObjectId
from marshmallow import Schema, fields

from opendc.exts import db
from opendc.models.project import Project
from opendc.models.model import Model


class MemorySchema(Schema):
    """
    Schema representing a memory unit.
    """
    _id = fields.String()
    name = fields.String()
    speedMbPerS = fields.Integer()
    sizeMb = fields.Integer()
    energyConsumptionW = fields.Integer()


class PuSchema(Schema):
    """
    Schema representing a processing unit.
    """
    _id = fields.String()
    name = fields.String()
    clockRateMhz = fields.Integer()
    numberOfCores = fields.Integer()
    energyConsumptionW = fields.Integer()


class MachineSchema(Schema):
    """
    Schema representing a machine.
    """
    _id = fields.String()
    position = fields.Integer()
    cpus = fields.List(fields.Nested(PuSchema))
    gpus = fields.List(fields.Nested(PuSchema))
    memories = fields.List(fields.Nested(MemorySchema))
    storages = fields.List(fields.Nested(MemorySchema))


class ObjectSchema(Schema):
    """
    Schema representing a room object.
    """
    _id = fields.String()
    name = fields.String()
    capacity = fields.Integer()
    powerCapacityW = fields.Integer()
    machines = fields.List(fields.Nested(MachineSchema))


class TileSchema(Schema):
    """
    Schema representing a room tile.
    """
    _id = fields.String()
    positionX = fields.Integer()
    positionY = fields.Integer()
    rack = fields.Nested(ObjectSchema)


class RoomSchema(Schema):
    """
    Schema representing a room.
    """
    _id = fields.String()
    name = fields.String(required=True)
    tiles = fields.List(fields.Nested(TileSchema), required=True)


class TopologySchema(Schema):
    """
    Schema representing a datacenter topology.
    """
    _id = fields.String(dump_only=True)
    projectId = fields.String(dump_only=True)
    name = fields.String(required=True)
    rooms = fields.List(fields.Nested(RoomSchema), required=True)


class Topology(Model):
    """Model representing a Project."""

    collection_name = 'topologies'

    def check_user_access(self, user_id, edit_access):
        """Raises an error if the user with given [user_id] has insufficient access.

        Checks access on the parent project.

        :param user_id: The User ID of the user.
        :param edit_access: True when edit access should be checked, otherwise view access.
        """
        project = Project.from_id(self.obj['projectId'])
        project.check_user_access(user_id, edit_access)

    @classmethod
    def get_for_project(cls, project_id):
        """Get all topologies for the specified project id."""
        return db.fetch_all({'projectId': ObjectId(project_id)}, cls.collection_name)
