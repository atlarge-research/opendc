from opendc.util.database import DB

test_id = 24 * '1'
test_id_2 = 24 * '2'


def test_get_topology(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    res = client.get(f'/api/v2/topologies/{test_id}')
    assert '200' in res.status


def test_get_topology_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/topologies/1').status


def test_get_topology_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'projectId': test_id_2,
                                'authorizationLevel': 'OWN'
                            }]
                        })
    res = client.get(f'/api/v2/topologies/{test_id}')
    assert '403' in res.status


def test_get_topology_no_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'projectId': test_id, 'authorizations': []})
    res = client.get(f'/api/v2/topologies/{test_id}')
    assert '403' in res.status


def test_update_topology_missing_parameter(client):
    assert '400' in client.put(f'/api/v2/topologies/{test_id}').status


def test_update_topology_non_existent(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put(f'/api/v2/topologies/{test_id}', json={'topology': {'name': 'test_topology', 'rooms': {}}}).status


def test_update_topology_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    mocker.patch.object(DB, 'update', return_value={})
    assert '403' in client.put(f'/api/v2/topologies/{test_id}', json={
        'topology': {
            'name': 'updated_topology',
            'rooms': {}
        }
    }).status


def test_update_topology(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'update', return_value={})

    assert '200' in client.put(f'/api/v2/topologies/{test_id}', json={
        'topology': {
            'name': 'updated_topology',
            'rooms': {}
        }
    }).status


def test_delete_topology(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'googleId': 'test',
                            'topologyIds': [test_id],
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value={})
    mocker.patch.object(DB, 'update', return_value=None)
    res = client.delete(f'/api/v2/topologies/{test_id}')
    assert '200' in res.status


def test_delete_nonexistent_topology(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete(f'/api/v2/topologies/{test_id}').status
