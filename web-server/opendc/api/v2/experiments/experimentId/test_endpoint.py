from opendc.util.database import DB


def test_get_experiment_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/experiments/1').status


def test_get_experiment_no_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'simulationId': '1', 'authorizations': []})
    res = client.get('/api/v2/experiments/1')
    assert '403' in res.status


def test_get_experiment_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            'simulationId': '1',
                            '_id': '1',
                            'authorizations': [{
                                'simulationId': '2',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    res = client.get('/api/v2/experiments/1')
    assert '403' in res.status


def test_get_experiment(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            'simulationId': '1',
                            '_id': '1',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    res = client.get('/api/v2/experiments/1')
    assert '200' in res.status


def test_update_experiment_missing_parameter(client):
    assert '400' in client.put('/api/v2/experiments/1').status


def test_update_experiment_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put('/api/v2/experiments/1', json={'experiment': {
        'topologyId': '1',
        'traceId': '1',
        'schedulerName': 'default',
        'name': 'test',
    }}).status


def test_update_experiment_not_authorized(client, mocker):
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
    assert '403' in client.put('/api/v2/experiments/1', json={'experiment': {
        'topologyId': '1',
        'traceId': '1',
        'schedulerName': 'default',
        'name': 'test',
    }}).status


def test_update_experiment(client, mocker):
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

    res = client.put('/api/v2/experiments/1', json={'experiment': {
        'topologyId': '1',
        'traceId': '1',
        'schedulerName': 'default',
        'name': 'test',
    }})
    assert '200' in res.status


def test_delete_simulation_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete('/api/v2/experiments/1').status


def test_delete_simulation_different_user(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'simulationId': '1',
                            'googleId': 'other_test',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value=None)
    assert '403' in client.delete('/api/v2/experiments/1').status


def test_delete_simulation(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'simulationId': '1',
                            'googleId': 'test',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value={})
    res = client.delete('/api/v2/experiments/1')
    assert '200' in res.status
