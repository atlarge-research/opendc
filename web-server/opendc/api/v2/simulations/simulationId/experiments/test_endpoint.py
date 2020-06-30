from opendc.util.database import DB


def test_add_experiment_missing_parameter(client):
    assert '400' in client.post('/api/v2/simulations/1/experiments').status


def test_add_experiment_non_existing_simulation(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.post('/api/v2/simulations/1/experiments',
                                json={
                                    'experiment': {
                                        'topologyId': '1',
                                        'traceId': '1',
                                        'schedulerName': 'default',
                                        'name': 'test',
                                    }
                                }).status


def test_add_experiment_not_authorized(client, mocker):
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
    assert '403' in client.post('/api/v2/simulations/1/experiments',
                                json={
                                    'experiment': {
                                        'topologyId': '1',
                                        'traceId': '1',
                                        'schedulerName': 'default',
                                        'name': 'test',
                                    }
                                }).status


def test_add_experiment(client, mocker):
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
    mocker.patch.object(DB,
                        'insert',
                        return_value={
                            'topologyId': '1',
                            'traceId': '1',
                            'schedulerName': 'default',
                            'name': 'test',
                            'state': 'QUEUED',
                            'lastSimulatedTick': 0,
                        })
    res = client.post(
        '/api/v2/simulations/1/experiments',
        json={'experiment': {
            'topologyId': '1',
            'traceId': '1',
            'schedulerName': 'default',
            'name': 'test',
        }})
    assert 'topologyId' in res.json['content']
    assert 'state' in res.json['content']
    assert 'lastSimulatedTick' in res.json['content']
    assert '200' in res.status
