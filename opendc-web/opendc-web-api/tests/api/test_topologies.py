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


def test_get_topology(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'EDIT'
                            }]
                        })
    res = client.get(f'/topologies/{test_id}')
    assert '200' in res.status


def test_get_topology_non_existing(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.get('/topologies/1').status


def test_get_topology_not_authorized(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': []
                        })
    res = client.get(f'/topologies/{test_id}')
    assert '403' in res.status


def test_get_topology_no_authorizations(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={'projectId': test_id, 'authorizations': []})
    res = client.get(f'/topologies/{test_id}')
    assert '403' in res.status


def test_update_topology_missing_parameter(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': []
                        })
    assert '400' in client.put(f'/topologies/{test_id}').status


def test_update_topology_non_existent(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.put(f'/topologies/{test_id}', json={'topology': {'name': 'test_topology', 'rooms': []}}).status


def test_update_topology_not_authorized(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': []
                        })
    mocker.patch.object(db, 'update', return_value={})
    assert '403' in client.put(f'/topologies/{test_id}', json={
        'topology': {
            'name': 'updated_topology',
            'rooms': []
        }
    }).status


def test_update_topology(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'OWN'
                            }]
                        })
    mocker.patch.object(db, 'update', return_value={})

    assert '200' in client.put(f'/topologies/{test_id}', json={
        'topology': {
            'name': 'updated_topology',
            'rooms': []
        }
    }).status


def test_delete_topology(client, mocker):
    mocker.patch.object(db,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'googleId': 'test',
                            'topologyIds': [test_id],
                            'authorizations': [{
                                'userId': 'test',
                                'level': 'OWN'
                            }]
                        })
    mocker.patch.object(db, 'delete_one', return_value={})
    mocker.patch.object(db, 'update', return_value=None)
    res = client.delete(f'/topologies/{test_id}')
    assert '200' in res.status


def test_delete_nonexistent_topology(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.delete(f'/topologies/{test_id}').status
