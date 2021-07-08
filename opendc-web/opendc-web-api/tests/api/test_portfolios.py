#  Copyright (c) 2021 AtLarge Research
#
#  Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files (the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions:
#
#  The above copyright notice and this permission notice shall be included in all
#  copies or substantial portions of the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
#  SOFTWARE.

from opendc.exts import db

test_id = 24 * '1'
test_id_2 = 24 * '2'


def test_get_portfolio_non_existing(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.get(f'/portfolios/{test_id}').status


def test_get_portfolio_no_authorizations(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={'projectId': test_id, 'authorizations': []})
    res = client.get(f'/portfolios/{test_id}')
    assert '403' in res.status


def test_get_portfolio_not_authorized(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            'projectId': test_id,
                            '_id': test_id,
                            'authorizations': []
                        })
    res = client.get(f'/portfolios/{test_id}')
    assert '403' in res.status


def test_get_portfolio(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            'projectId': test_id,
                            '_id': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'EDIT'
                            }]
                        })
    res = client.get(f'/portfolios/{test_id}')
    assert '200' in res.status


def test_update_portfolio_missing_parameter(client):
    assert '400' in client.put(f'/portfolios/{test_id}').status


def test_update_portfolio_non_existing(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.put(f'/portfolios/{test_id}', json={
        'portfolio': {
            'name': 'test',
            'targets': {
                'enabledMetrics': ['test'],
                'repeatsPerScenario': 2
            }
        }
    }).status


def test_update_portfolio_not_authorized(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'VIEW'
                            }]
                        })
    mocker.patch.object(db, 'update', return_value={})
    assert '403' in client.put(f'/portfolios/{test_id}', json={
        'portfolio': {
            'name': 'test',
            'targets': {
                'enabledMetrics': ['test'],
                'repeatsPerScenario': 2
            }
        }
    }).status


def test_update_portfolio(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'OWN'
                            }],
                            'targets': {
                                'enabledMetrics': [],
                                'repeatsPerScenario': 1
                            }
                        })
    mocker.patch.object(db, 'update', return_value={})

    res = client.put(f'/portfolios/{test_id}', json={'portfolio': {
        'name': 'test',
        'targets': {
            'enabledMetrics': ['test'],
            'repeatsPerScenario': 2
        }
    }})
    assert '200' in res.status


def test_delete_project_non_existing(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.delete(f'/portfolios/{test_id}').status


def test_delete_project_different_user(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'googleId': 'other_test',
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'VIEW'
                            }]
                        })
    mocker.patch.object(db, 'delete_one', return_value=None)
    assert '403' in client.delete(f'/portfolios/{test_id}').status


def test_delete_project(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'googleId': 'test',
                            'portfolioIds': [test_id],
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'OWN'
                            }]
                        })
    mocker.patch.object(db, 'delete_one', return_value={})
    mocker.patch.object(db, 'update', return_value=None)
    res = client.delete(f'/portfolios/{test_id}')
    assert '200' in res.status


def test_add_topology_missing_parameter(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'googleId': 'test',
                            'portfolioIds': [test_id],
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'OWN'
                            }]
                        })
    assert '400' in client.post(f'/projects/{test_id}/topologies').status


def test_add_topology(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'OWN'
                            }],
                            'topologyIds': []
                        })
    mocker.patch.object(db,
                        'insert',
                        return_value={
                            '_id': test_id,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'topologyIds': []
                        })
    mocker.patch.object(db, 'update', return_value={})
    res = client.post(f'/projects/{test_id}/topologies', json={'topology': {'name': 'test project', 'rooms': []}})
    assert 'rooms' in res.json['data']
    assert '200' in res.status


def test_add_topology_not_authorized(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'VIEW'
                            }]
                        })
    assert '403' in client.post(f'/projects/{test_id}/topologies',
                                json={
                                    'topology': {
                                        'name': 'test_topology',
                                        'rooms': []
                                    }
                                }).status


def test_add_portfolio_missing_parameter(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'googleId': 'test',
                            'portfolioIds': [test_id],
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'OWN'
                            }]
                        })
    assert '400' in client.post(f'/projects/{test_id}/portfolios').status


def test_add_portfolio_non_existing_project(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.post(f'/projects/{test_id}/portfolios',
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
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'VIEW'
                            }]
                        })
    assert '403' in client.post(f'/projects/{test_id}/portfolios',
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
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'portfolioIds': [test_id],
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'EDIT'
                            }]
                        })
    mocker.patch.object(db,
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
    mocker.patch.object(db, 'update', return_value=None)
    res = client.post(
        f'/projects/{test_id}/portfolios',
        json={
            'portfolio': {
                'name': 'test',
                'targets': {
                    'enabledMetrics': ['test'],
                    'repeatsPerScenario': 2
                }
            }
        })
    assert 'projectId' in res.json['data']
    assert 'scenarioIds' in res.json['data']
    assert '200' in res.status


def test_get_portfolio_scenarios(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            'projectId': test_id,
                            '_id': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'EDIT'
                            }]
                        })
    mocker.patch.object(db, 'fetch_all', return_value=[{'_id': test_id}])
    res = client.get(f'/portfolios/{test_id}/scenarios')
    assert '200' in res.status
