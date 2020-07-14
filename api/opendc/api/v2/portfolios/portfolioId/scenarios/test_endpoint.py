from opendc.util.database import DB


def test_add_scenario_missing_parameter(client):
    assert '400' in client.post('/api/v2/portfolios/1/scenarios').status


def test_add_scenario_non_existing_portfolio(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.post('/api/v2/portfolios/1/scenarios',
                                json={
                                    'scenario': {
                                        'name': 'test',
                                        'trace': {
                                            'traceId': '1',
                                            'loadSamplingFraction': 1.0,
                                        },
                                        'topology': {
                                            'topologyId': '1',
                                        },
                                        'operational': {
                                            'failuresEnabled': True,
                                            'performanceInterferenceEnabled': False,
                                            'schedulerName': 'DEFAULT',
                                        },
                                    }
                                }).status


def test_add_scenario_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'projectId': '1',
                            'portfolioId': '1',
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    assert '403' in client.post('/api/v2/portfolios/1/scenarios',
                                json={
                                    'scenario': {
                                        'name': 'test',
                                        'trace': {
                                            'traceId': '1',
                                            'loadSamplingFraction': 1.0,
                                        },
                                        'topology': {
                                            'topologyId': '1',
                                        },
                                        'operational': {
                                            'failuresEnabled': True,
                                            'performanceInterferenceEnabled': False,
                                            'schedulerName': 'DEFAULT',
                                        },
                                    }
                                }).status


def test_add_scenario(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'projectId': '1',
                            'portfolioId': '1',
                            'portfolioIds': ['1'],
                            'scenarioIds': ['1'],
                            'authorizations': [{
                                'projectId': '1',
                                'authorizationLevel': 'EDIT'
                            }],
                            'simulationState': 'QUEUED',
                        })
    mocker.patch.object(DB,
                        'insert',
                        return_value={
                            '_id': '1',
                            'name': 'test',
                            'trace': {
                                'traceId': '1',
                                'loadSamplingFraction': 1.0,
                            },
                            'topology': {
                                'topologyId': '1',
                            },
                            'operational': {
                                'failuresEnabled': True,
                                'performanceInterferenceEnabled': False,
                                'schedulerName': 'DEFAULT',
                            },
                            'portfolioId': '1',
                            'simulationState': 'QUEUED',
                        })
    mocker.patch.object(DB, 'update', return_value=None)
    res = client.post(
        '/api/v2/portfolios/1/scenarios',
        json={
            'scenario': {
                'name': 'test',
                'trace': {
                    'traceId': '1',
                    'loadSamplingFraction': 1.0,
                },
                'topology': {
                    'topologyId': '1',
                },
                'operational': {
                    'failuresEnabled': True,
                    'performanceInterferenceEnabled': False,
                    'schedulerName': 'DEFAULT',
                },
            }
        })
    assert 'portfolioId' in res.json['content']
    assert 'simulationState' in res.json['content']
    assert '200' in res.status
