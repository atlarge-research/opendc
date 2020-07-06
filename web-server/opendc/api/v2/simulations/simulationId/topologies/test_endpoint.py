from opendc.util.database import DB


def test_add_topology_missing_parameter(client):
    assert '400' in client.post('/api/v2/simulations/1/topologies').status


def test_add_topology(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'OWN'
                            }],
                            'topologyIds': []
                        })
    mocker.patch.object(DB,
                        'insert',
                        return_value={
                            '_id': '1',
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'topologyIds': []
                        })
    mocker.patch.object(DB, 'update', return_value={})
    res = client.post('/api/v2/simulations/1/topologies', json={'topology': {'name': 'test simulation', 'rooms': []}})
    assert 'rooms' in res.json['content']
    assert '200' in res.status


def test_add_topology_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'simulationId': '1',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    assert '403' in client.post('/api/v2/simulations/1/topologies',
                                json={
                                    'topology': {
                                        'name': 'test_topology',
                                        'rooms': {}
                                    }
                                }).status
