from opendc.util.database import DB


def test_get_topology(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'simulationId': '1',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    res = client.get('/api/v2/topologies/1')
    assert '200' in res.status


def test_get_topology_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/topologies/1').status


def test_get_topology_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'simulationId': '1',
                            'authorizations': [{
                                'simulationId': '2',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    res = client.get('/api/v2/topologies/1')
    assert '403' in res.status


def test_get_topology_no_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'simulationId': '1', 'authorizations': []})
    res = client.get('/api/v2/topologies/1')
    assert '403' in res.status


def test_update_topology_missing_parameter(client):
    assert '400' in client.put('/api/v2/topologies/1').status


def test_update_topology_non_existent(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put('/api/v2/topologies/1', json={'topology': {'name': 'test_topology', 'rooms': {}}}).status


def test_update_topology_not_authorized(client, mocker):
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
    mocker.patch.object(DB, 'update', return_value={})
    assert '403' in client.put('/api/v2/topologies/1', json={
        'topology': {
            'name': 'updated_topology',
            'rooms': {}
        }
    }).status


def test_update_topology(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'simulationId': '1',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'update', return_value={})

    assert '200' in client.put('/api/v2/topologies/1', json={
        'topology': {
            'name': 'updated_topology',
            'rooms': {}
        }
    }).status


def test_delete_topology(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'simulationId': '1',
                            'googleId': 'test',
                            'topologyIds': ['1'],
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value={})
    mocker.patch.object(DB, 'update', return_value=None)
    res = client.delete('/api/v2/topologies/1')
    assert '200' in res.status


def test_delete_nonexistent_topology(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete('/api/v2/topologies/1').status
