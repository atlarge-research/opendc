from opendc.util.database import DB


def test_get_simulation_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/simulations/1').status


def test_get_simulation_no_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'authorizations': []})
    res = client.get('/api/v2/simulations/1')
    assert '403' in res.status


def test_get_simulation_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'authorizations': [{
                                'simulationId': '2',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    res = client.get('/api/v2/simulations/1')
    assert '403' in res.status


def test_get_simulation(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    res = client.get('/api/v2/simulations/1')
    assert '200' in res.status


def test_update_simulation_missing_parameter(client):
    assert '400' in client.put('/api/v2/simulations/1').status


def test_update_simulation_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put('/api/v2/simulations/1', json={'simulation': {'name': 'S'}}).status


def test_update_simulation_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    mocker.patch.object(DB, 'update', return_value={})
    assert '403' in client.put('/api/v2/simulations/1', json={'simulation': {'name': 'S'}}).status


def test_update_simulation(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'update', return_value={})

    res = client.put('/api/v2/simulations/1', json={'simulation': {'name': 'S'}})
    assert '200' in res.status
