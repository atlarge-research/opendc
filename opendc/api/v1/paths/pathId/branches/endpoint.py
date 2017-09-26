from datetime import datetime

from opendc.models.datacenter import Datacenter
from opendc.models.machine import Machine
from opendc.models.object import Object
from opendc.models.path import Path
from opendc.models.rack import Rack
from opendc.models.room import Room
from opendc.models.section import Section
from opendc.models.tile import Tile
from opendc.util import database, exceptions
from opendc.util.rest import Request, Response


def POST(request):
    """Create a new Path that branches off of this Path at the specified tick."""

    # Make sure required parameters are there

    try:
        request.check_required_parameters(
            path={
                'pathId': 'int'
            },
            body={
                'section': {
                    'startTick': 'int'
                }
            }
        )

    except exceptions.ParameterError as e:
        return Response(400, e.message)

    # Instantiate the current Path from the database

    current_path = Path.from_primary_key((request.params_path['pathId'],))

    # Make sure the current Path exists

    if not current_path.exists():
        return Response(404, '{} not found.'.format(current_path))

    # Make sure this user is authorized to branch off the current Path

    if not current_path.google_id_has_at_least(request.google_id, 'EDIT'):
        return Response(403, 'Forbidden from branching off {}.'.format(current_path))

    # Create the new Path

    new_path = Path(
        simulation_id=current_path.simulation_id,
        datetime_created=database.datetime_to_string(datetime.now())
    )

    new_path.insert()

    # Get the current Path's sections and add them to the new Path if they're before the branch

    current_sections = Section.query('path_id', current_path.id)
    last_section = None

    for current_section in current_sections:

        if current_section.start_tick < request.params_body['section']['startTick'] or current_section.start_tick == 0:
            new_section = Section(
                path_id=new_path.id,
                datacenter_id=current_section.datacenter_id,
                start_tick=current_section.start_tick
            )

            new_section.insert()

            last_section = current_section

    # Make a deep copy of the last section's datacenter, its rooms, their tiles, etc.

    path_parameters = {
        'simulationId': new_path.simulation_id
    }

    # Copy the Datacenter

    old_datacenter = Datacenter.from_primary_key((last_section.datacenter_id,))

    message = old_datacenter.generate_api_call(path_parameters, request.token)
    response = Request(message).process()

    path_parameters['datacenterId'] = response.content['id']

    # Create the new last Section, with the IDs of the new Path and new Datacenter

    if last_section.start_tick != 0:
        new_section = Section(
            path_id=new_path.id,
            datacenter_id=path_parameters['datacenterId'],
            start_tick=request.params_body['section']['startTick']
        )

        new_section.insert()

    else:
        last_section.datacenter_id = path_parameters['datacenterId']
        last_section.update()

    # Copy the rest of the Datacenter, starting with the Rooms...

    old_rooms = Room.query('datacenter_id', old_datacenter.id)

    for old_room in old_rooms:

        old_room.datacenter_id = path_parameters['datacenterId']

        if old_room.topology_id is None:
            old_room.topology_id = old_room.id

        message = old_room.generate_api_call(path_parameters, request.token)
        response = Request(message).process()

        path_parameters['roomId'] = response.content['id']

        # ... then the Tiles, ...

        old_tiles = Tile.query('room_id', old_room.id)

        for old_tile in old_tiles:

            old_tile.room_id = path_parameters['roomId']

            if old_tile.topology_id is None:
                old_tile.topology_id = old_tile.id

            message = old_tile.generate_api_call(path_parameters, request.token)
            response = Request(message).process()

            path_parameters['tileId'] = response.content['id']

            old_objects = Object.query('id', old_tile.object_id)

            # ... then the Tile's Rack, ...

            if len(old_objects) == 1 and old_objects[0].type == 'RACK':

                old_rack = Rack.query('id', old_objects[0].id)[0]

                if old_rack.topology_id is None:
                    old_rack.topology_id = old_rack.id

                message = old_rack.generate_api_call(path_parameters, request.token)
                response = Request(message).process()

                path_parameters['rackId'] = response.content['id']

                # ... then the Rack's Machines ...

                old_machines = Machine.query('rack_id', old_rack.id)

                for old_machine in old_machines:
                    old_machine.read()
                    old_machine.rack_id = path_parameters['rackId']

                    if old_machine.topology_id is None:
                        old_machine.topology_id = old_machine.id

                    message = old_machine.generate_api_call(path_parameters, request.token)
                    response = Request(message).process()

                    path_parameters['machineId'] = response.content['id']

    # Return the new Path

    return Response(
        200,
        'Successfully created {}.'.format(new_path),
        new_path.to_JSON()
    )
