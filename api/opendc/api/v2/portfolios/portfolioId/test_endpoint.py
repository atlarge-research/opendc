from opendc.util.database import DB

test_id = 24 * '1'
test_id_2 = 24 * '2'


def test_get_portfolio_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get(f'/api/v2/portfolios/{test_id}').status


def test_get_portfolio_no_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'projectId': test_id, 'authorizations': []})
    res = client.get(f'/api/v2/portfolios/{test_id}')
    assert '403' in res.status


def test_get_portfolio_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            'projectId': test_id,
                            '_id': test_id,
                            'authorizations': [{
                                'projectId': test_id_2,
                                'authorizationLevel': 'OWN'
                            }]
                        })
    res = client.get(f'/api/v2/portfolios/{test_id}')
    assert '403' in res.status


def test_get_portfolio(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            'projectId': test_id,
                            '_id': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    res = client.get(f'/api/v2/portfolios/{test_id}')
    assert '200' in res.status


def test_update_portfolio_missing_parameter(client):
    assert '400' in client.put(f'/api/v2/portfolios/{test_id}').status


def test_update_portfolio_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put(f'/api/v2/portfolios/{test_id}', json={
        'portfolio': {
            'name': 'test',
            'targets': {
                'enabledMetrics': ['test'],
                'repeatsPerScenario': 2
            }
        }
    }).status


def test_update_portfolio_not_authorized(client, mocker):
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
    assert '403' in client.put(f'/api/v2/portfolios/{test_id}', json={
        'portfolio': {
            'name': 'test',
            'targets': {
                'enabledMetrics': ['test'],
                'repeatsPerScenario': 2
            }
        }
    }).status


def test_update_portfolio(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
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

    res = client.put(f'/api/v2/portfolios/{test_id}', json={'portfolio': {
        'name': 'test',
        'targets': {
            'enabledMetrics': ['test'],
            'repeatsPerScenario': 2
        }
    }})
    assert '200' in res.status


def test_delete_project_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete(f'/api/v2/portfolios/{test_id}').status


def test_delete_project_different_user(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'googleId': 'other_test',
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value=None)
    assert '403' in client.delete(f'/api/v2/portfolios/{test_id}').status


def test_delete_project(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'googleId': 'test',
                            'portfolioIds': [test_id],
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value={})
    mocker.patch.object(DB, 'update', return_value=None)
    res = client.delete(f'/api/v2/portfolios/{test_id}')
    assert '200' in res.status
