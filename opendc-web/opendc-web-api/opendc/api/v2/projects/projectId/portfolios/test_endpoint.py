from opendc.util.database import DB

test_id = 24 * '1'


def test_add_portfolio_missing_parameter(client):
    assert '400' in client.post(f'/v2/projects/{test_id}/portfolios').status


def test_add_portfolio_non_existing_project(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.post(f'/v2/projects/{test_id}/portfolios',
                                json={
                                    'portfolio': {
                                        'name': 'test',
                                        'targets': {
                                            'enabledMetrics': ['test'],
                                            'repeatsPerScenario': 2
                                        }
                                    }
                                }).status


def test_add_portfolio_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    assert '403' in client.post(f'/v2/projects/{test_id}/portfolios',
                                json={
                                    'portfolio': {
                                        'name': 'test',
                                        'targets': {
                                            'enabledMetrics': ['test'],
                                            'repeatsPerScenario': 2
                                        }
                                    }
                                }).status


def test_add_portfolio(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'portfolioIds': [test_id],
                            'authorizations': [{
                                'userId': 'test',
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    mocker.patch.object(DB,
                        'insert',
                        return_value={
                            '_id': test_id,
                            'name': 'test',
                            'targets': {
                                'enabledMetrics': ['test'],
                                'repeatsPerScenario': 2
                            },
                            'projectId': test_id,
                            'scenarioIds': [],
                        })
    mocker.patch.object(DB, 'update', return_value=None)
    res = client.post(
        f'/v2/projects/{test_id}/portfolios',
        json={
            'portfolio': {
                'name': 'test',
                'targets': {
                    'enabledMetrics': ['test'],
                    'repeatsPerScenario': 2
                }
            }
        })
    assert 'projectId' in res.json['content']
    assert 'scenarioIds' in res.json['content']
    assert '200' in res.status
