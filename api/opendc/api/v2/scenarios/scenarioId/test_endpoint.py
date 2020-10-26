from opendc.util.database import DB

test_id = 24 * '1'
test_id_2 = 24 * '2'


def test_get_scenario_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get(f'/api/v2/scenarios/{test_id}').status


def test_get_scenario_no_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={
        'portfolioId': '1',
        'authorizations': []
    })
    res = client.get(f'/api/v2/scenarios/{test_id}')
    assert '403' in res.status


def test_get_scenario_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            'projectId': test_id,
                            'portfolioId': test_id,
                            '_id': test_id,
                            'authorizations': [{
                                'projectId': test_id_2,
                                'authorizationLevel': 'OWN'
                            }]
                        })
    res = client.get(f'/api/v2/scenarios/{test_id}')
    assert '403' in res.status


def test_get_scenario(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            'projectId': test_id,
                            'portfolioId': test_id,
                            '_id': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    res = client.get(f'/api/v2/scenarios/{test_id}')
    assert '200' in res.status


def test_update_scenario_missing_parameter(client):
    assert '400' in client.put(f'/api/v2/scenarios/{test_id}').status


def test_update_scenario_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put(f'/api/v2/scenarios/{test_id}', json={
        'scenario': {
            'name': 'test',
        }
    }).status


def test_update_scenario_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'portfolioId': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    mocker.patch.object(DB, 'update', return_value={})
    assert '403' in client.put(f'/api/v2/scenarios/{test_id}', json={
        'scenario': {
            'name': 'test',
        }
    }).status


def test_update_scenario(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'portfolioId': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'OWN'
                            }],
                            'targets': {
                                'enabledMetrics': [],
                                'repeatsPerScenario': 1
                            }
                        })
    mocker.patch.object(DB, 'update', return_value={})

    res = client.put(f'/api/v2/scenarios/{test_id}', json={'scenario': {
        'name': 'test',
    }})
    assert '200' in res.status


def test_delete_project_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete(f'/api/v2/scenarios/{test_id}').status


def test_delete_project_different_user(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'portfolioId': test_id,
                            'googleId': 'other_test',
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value=None)
    assert '403' in client.delete(f'/api/v2/scenarios/{test_id}').status


def test_delete_project(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'portfolioId': test_id,
                            'googleId': 'test',
                            'scenarioIds': [test_id],
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value={})
    mocker.patch.object(DB, 'update', return_value=None)
    res = client.delete(f'/api/v2/scenarios/{test_id}')
    assert '200' in res.status
