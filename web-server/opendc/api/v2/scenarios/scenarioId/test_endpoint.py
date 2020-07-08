from opendc.util.database import DB


def test_get_scenario_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/scenarios/1').status


def test_get_scenario_no_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={
        'portfolioId': '1',
        'authorizations': []
    })
    res = client.get('/api/v2/scenarios/1')
    assert '403' in res.status


def test_get_scenario_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            'portfolioId': '1',
                            '_id': '1',
                            'authorizations': [{
                                'projectId': '2',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    res = client.get('/api/v2/scenarios/1')
    assert '403' in res.status


def test_get_scenario(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            'portfolioId': '1',
                            '_id': '1',
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    res = client.get('/api/v2/scenarios/1')
    assert '200' in res.status


def test_update_scenario_missing_parameter(client):
    assert '400' in client.put('/api/v2/scenarios/1').status


def test_update_scenario_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put('/api/v2/scenarios/1', json={
        'scenario': {
            'name': 'test',
        }
    }).status


def test_update_scenario_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'portfolioId': '1',
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    mocker.patch.object(DB, 'update', return_value={})
    assert '403' in client.put('/api/v2/scenarios/1', json={
        'scenario': {
            'name': 'test',
        }
    }).status


def test_update_scenario(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'portfolioId': '1',
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'OWN'
                            }],
                            'targets': {
                                'enabledMetrics': [],
                                'repeatsPerScenario': 1
                            }
                        })
    mocker.patch.object(DB, 'update', return_value={})

    res = client.put('/api/v2/scenarios/1', json={'scenario': {
        'name': 'test',
    }})
    assert '200' in res.status


def test_delete_project_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete('/api/v2/scenarios/1').status


def test_delete_project_different_user(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'portfolioId': '1',
                            'googleId': 'other_test',
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value=None)
    assert '403' in client.delete('/api/v2/scenarios/1').status


def test_delete_project(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'portfolioId': '1',
                            'googleId': 'test',
                            'scenarioIds': ['1'],
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value={})
    mocker.patch.object(DB, 'update', return_value=None)
    res = client.delete('/api/v2/scenarios/1')
    assert '200' in res.status
