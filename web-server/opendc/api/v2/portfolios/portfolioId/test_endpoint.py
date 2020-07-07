from opendc.util.database import DB


def test_get_portfolio_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/portfolios/1').status


def test_get_portfolio_no_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'projectId': '1', 'authorizations': []})
    res = client.get('/api/v2/portfolios/1')
    assert '403' in res.status


def test_get_portfolio_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            'projectId': '1',
                            '_id': '1',
                            'authorizations': [{
                                'projectId': '2',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    res = client.get('/api/v2/portfolios/1')
    assert '403' in res.status


def test_get_portfolio(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            'projectId': '1',
                            '_id': '1',
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    res = client.get('/api/v2/portfolios/1')
    assert '200' in res.status


def test_update_portfolio_missing_parameter(client):
    assert '400' in client.put('/api/v2/portfolios/1').status


def test_update_portfolio_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put('/api/v2/portfolios/1', json={
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
                            '_id': '1',
                            'projectId': '1',
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    mocker.patch.object(DB, 'update', return_value={})
    assert '403' in client.put('/api/v2/portfolios/1', json={
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
                            '_id': '1',
                            'projectId': '1',
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

    res = client.put('/api/v2/portfolios/1', json={'portfolio': {
        'name': 'test',
        'targets': {
            'enabledMetrics': ['test'],
            'repeatsPerScenario': 2
        }
    }})
    assert '200' in res.status


def test_delete_project_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete('/api/v2/portfolios/1').status


def test_delete_project_different_user(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'projectId': '1',
                            'googleId': 'other_test',
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value=None)
    assert '403' in client.delete('/api/v2/portfolios/1').status


def test_delete_project(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'projectId': '1',
                            'googleId': 'test',
                            'portfolioIds': ['1'],
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'delete_one', return_value={})
    mocker.patch.object(DB, 'update', return_value=None)
    res = client.delete('/api/v2/portfolios/1')
    assert '200' in res.status
