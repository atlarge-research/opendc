from opendc.util.database import DB


def test_add_portfolio_missing_parameter(client):
    assert '400' in client.post('/api/v2/projects/1/portfolios').status


def test_add_portfolio_non_existing_project(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.post('/api/v2/projects/1/portfolios',
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
                            '_id': '1',
                            'projectId': '1',
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    assert '403' in client.post('/api/v2/projects/1/portfolios',
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
                            '_id': '1',
                            'projectId': '1',
                            'portfolioIds': ['1'],
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    mocker.patch.object(DB,
                        'insert',
                        return_value={
                            '_id': '1',
                            'name': 'test',
                            'targets': {
                                'enabledMetrics': ['test'],
                                'repeatsPerScenario': 2
                            },
                            'projectId': '1',
                            'scenarioIds': [],
                            'baseScenarioId': '-1',
                        })
    mocker.patch.object(DB, 'update', return_value=None)
    res = client.post(
        '/api/v2/projects/1/portfolios',
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
    assert 'baseScenarioId' in res.json['content']
    assert '200' in res.status
