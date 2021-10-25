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


def test_get_user_projects(client, mocker):
    mocker.patch.object(db, 'fetch_all', return_value={'_id': test_id, 'authorizations': [{'userId': 'test',
                                                                                           'level': 'OWN'}]})
    res = client.get('/projects/')
    assert '200' in res.status


def test_get_user_topologies(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'EDIT'
                            }]
                        })
    mocker.patch.object(db, 'fetch_all', return_value=[{'_id': test_id}])
    res = client.get(f'/projects/{test_id}/topologies')
    assert '200' in res.status


def test_get_user_portfolios(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'EDIT'
                            }]
                        })
    mocker.patch.object(db, 'fetch_all', return_value=[{'_id': test_id}])
    res = client.get(f'/projects/{test_id}/portfolios')
    assert '200' in res.status


def test_add_project_missing_parameter(client):
    assert '400' in client.post('/projects/').status


def test_add_project(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={'_id': test_id, 'authorizations': []})
    mocker.patch.object(db,
                        'insert',
                        return_value={
                            '_id': test_id,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'topologyIds': []
                        })
    mocker.patch.object(db, 'update', return_value={})
    res = client.post('/projects/', json={'project': {'name': 'test project'}})
    assert 'datetimeCreated' in res.json['data']
    assert 'datetimeLastEdited' in res.json['data']
    assert 'topologyIds' in res.json['data']
    assert '200' in res.status


def test_get_project_non_existing(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.get(f'/projects/{test_id}').status


def test_get_project_no_authorizations(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={'authorizations': []})
    res = client.get(f'/projects/{test_id}')
    assert '403' in res.status


def test_get_project_not_authorized(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': []
                        })
    res = client.get(f'/projects/{test_id}')
    assert '403' in res.status


def test_get_project(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'EDIT'
                            }]
                        })
    res = client.get(f'/projects/{test_id}')
    assert '200' in res.status


def test_update_project_missing_parameter(client):
    assert '400' in client.put(f'/projects/{test_id}').status


def test_update_project_non_existing(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.put(f'/projects/{test_id}', json={'project': {'name': 'S'}}).status


def test_update_project_not_authorized(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'VIEW'
                            }]
                        })
    mocker.patch.object(db, 'update', return_value={})
    assert '403' in client.put(f'/projects/{test_id}', json={'project': {'name': 'S'}}).status


def test_update_project(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'OWN'
                            }]
                        })
    mocker.patch.object(db, 'update', return_value={})

    res = client.put(f'/projects/{test_id}', json={'project': {'name': 'S'}})
    assert '200' in res.status


def test_delete_project_non_existing(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.delete(f'/projects/{test_id}').status


def test_delete_project_different_user(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'googleId': 'other_test',
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'VIEW'
                            }],
                            'topologyIds': []
                        })
    mocker.patch.object(db, 'delete_one', return_value=None)
    assert '403' in client.delete(f'/projects/{test_id}').status


def test_delete_project(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'googleId': 'test',
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'OWN'
                            }],
                            'topologyIds': [],
                            'portfolioIds': [],
                        })
    mocker.patch.object(db, 'update', return_value=None)
    mocker.patch.object(db, 'delete_one', return_value={'googleId': 'test'})
    res = client.delete(f'/projects/{test_id}')
    assert '200' in res.status
