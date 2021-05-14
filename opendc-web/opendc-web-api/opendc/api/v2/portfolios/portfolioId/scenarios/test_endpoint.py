from opendc.util.database import DB

test_id = 24 * '1'


def test_add_scenario_missing_parameter(client):
    assert '400' in client.post('/v2/portfolios/1/scenarios').status


def test_add_scenario_non_existing_portfolio(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.post(f'/v2/portfolios/{test_id}/scenarios',
                                json={
                                    'scenario': {
                                        'name': 'test',
                                        'trace': {
                                            'traceId': test_id,
                                            'loadSamplingFraction': 1.0,
                                        },
                                        'topology': {
                                            'topologyId': test_id,
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
                            '_id': test_id,
                            'projectId': test_id,
                            'portfolioId': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    assert '403' in client.post(f'/v2/portfolios/{test_id}/scenarios',
                                json={
                                    'scenario': {
                                        'name': 'test',
                                        'trace': {
                                            'traceId': test_id,
                                            'loadSamplingFraction': 1.0,
                                        },
                                        'topology': {
                                            'topologyId': test_id,
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
                            '_id': test_id,
                            'projectId': test_id,
                            'portfolioId': test_id,
                            'portfolioIds': [test_id],
                            'scenarioIds': [test_id],
                            'authorizations': [{
                                'userId': 'test',
                                'authorizationLevel': 'EDIT'
                            }],
                            'simulation': {
                                'state': 'QUEUED',
                            },
                        })
    mocker.patch.object(DB,
                        'insert',
                        return_value={
                            '_id': test_id,
                            'name': 'test',
                            'trace': {
                                'traceId': test_id,
                                'loadSamplingFraction': 1.0,
                            },
                            'topology': {
                                'topologyId': test_id,
                            },
                            'operational': {
                                'failuresEnabled': True,
                                'performanceInterferenceEnabled': False,
                                'schedulerName': 'DEFAULT',
                            },
                            'portfolioId': test_id,
                            'simulationState': {
                                'state': 'QUEUED',
                            },
                        })
    mocker.patch.object(DB, 'update', return_value=None)
    res = client.post(
        f'/v2/portfolios/{test_id}/scenarios',
        json={
            'scenario': {
                'name': 'test',
                'trace': {
                    'traceId': test_id,
                    'loadSamplingFraction': 1.0,
                },
                'topology': {
                    'topologyId': test_id,
                },
                'operational': {
                    'failuresEnabled': True,
                    'performanceInterferenceEnabled': False,
                    'schedulerName': 'DEFAULT',
                },
            }
        })
    assert 'portfolioId' in res.json['content']
    assert 'simulation' in res.json['content']
    assert '200' in res.status
