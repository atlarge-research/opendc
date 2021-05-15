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

from unittest.mock import Mock
from opendc.exts import db

test_id = 24 * '1'
test_id_2 = 24 * '2'


def test_add_prefab_missing_parameter(client):
    assert '400' in client.post('/prefabs/').status


def test_add_prefab(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={'_id': test_id, 'authorizations': []})
    mocker.patch.object(db,
                        'insert',
                        return_value={
                            '_id': test_id,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': test_id
                        })
    res = client.post('/prefabs/', json={'prefab': {'name': 'test prefab'}})
    assert 'datetimeCreated' in res.json['data']
    assert 'datetimeLastEdited' in res.json['data']
    assert 'authorId' in res.json['data']
    assert '200' in res.status


def test_get_prefabs(client, mocker):
    db.fetch_all = Mock()
    mocker.patch.object(db, 'fetch_one', return_value={'_id': test_id})
    db.fetch_all.side_effect = [
        [{
            '_id': test_id,
            'datetimeCreated': '000',
            'datetimeLastEdited': '000',
            'authorId': test_id,
            'visibility' : 'private'
        },
            {
                '_id': '2' * 24,
                'datetimeCreated': '000',
                'datetimeLastEdited': '000',
                'authorId': test_id,
                'visibility' : 'private'
            },
            {
                '_id': '3' * 24,
                'datetimeCreated': '000',
                'datetimeLastEdited': '000',
                'authorId': test_id,
                'visibility' : 'public'
            },
            {
                '_id': '4' * 24,
                'datetimeCreated': '000',
                'datetimeLastEdited': '000',
                'authorId': test_id,
                'visibility' : 'public'
            }],
        [{
            '_id': '5' * 24,
            'datetimeCreated': '000',
            'datetimeLastEdited': '000',
            'authorId': '2' * 24,
            'visibility' : 'public'
        },
            {
                '_id': '6' * 24,
                'datetimeCreated': '000',
                'datetimeLastEdited': '000',
                'authorId': '2' * 24,
                'visibility' : 'public'
            },
            {
                '_id': '7' * 24,
                'datetimeCreated': '000',
                'datetimeLastEdited': '000',
                'authorId': '2' * 24,
                'visibility' : 'public'
            },
            {
                '_id': '8' * 24,
                'datetimeCreated': '000',
                'datetimeLastEdited': '000',
                'authorId': '2' * 24,
                'visibility' : 'public'
            }]
    ]
    mocker.patch.object(db, 'fetch_one', return_value={'_id': test_id})
    res = client.get('/prefabs/')
    assert '200' in res.status


def test_get_prefab_non_existing(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.get(f'/prefabs/{test_id}').status


def test_get_private_prefab_not_authorized(client, mocker):
    db.fetch_one = Mock()
    db.fetch_one.side_effect = [{
        '_id': test_id,
        'name': 'test prefab',
        'authorId': test_id_2,
        'visibility': 'private',
        'rack': {}
    },
        {
            '_id': test_id
        }
    ]
    res = client.get(f'/prefabs/{test_id}')
    assert '403' in res.status


def test_get_private_prefab(client, mocker):
    db.fetch_one = Mock()
    db.fetch_one.side_effect = [{
        '_id': test_id,
        'name': 'test prefab',
        'authorId': 'test',
        'visibility': 'private',
        'rack': {}
    },
        {
            '_id': test_id
        }
    ]
    res = client.get(f'/prefabs/{test_id}')
    assert '200' in res.status


def test_get_public_prefab(client, mocker):
    db.fetch_one = Mock()
    db.fetch_one.side_effect = [{
        '_id': test_id,
        'name': 'test prefab',
        'authorId': test_id_2,
        'visibility': 'public',
        'rack': {}
    },
        {
            '_id': test_id
        }
    ]
    res = client.get(f'/prefabs/{test_id}')
    assert '200' in res.status


def test_update_prefab_missing_parameter(client):
    assert '400' in client.put(f'/prefabs/{test_id}').status


def test_update_prefab_non_existing(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.put(f'/prefabs/{test_id}', json={'prefab': {'name': 'S'}}).status


def test_update_prefab_not_authorized(client, mocker):
    db.fetch_one = Mock()
    db.fetch_one.side_effect = [{
        '_id': test_id,
        'name': 'test prefab',
        'authorId': test_id_2,
        'visibility': 'private',
        'rack': {}
    },
        {
            '_id': test_id
        }
    ]
    mocker.patch.object(db, 'update', return_value={})
    assert '403' in client.put(f'/prefabs/{test_id}', json={'prefab': {'name': 'test prefab', 'rack': {}}}).status


def test_update_prefab(client, mocker):
    db.fetch_one = Mock()
    db.fetch_one.side_effect = [{
        '_id': test_id,
        'name': 'test prefab',
        'authorId': 'test',
        'visibility': 'private',
        'rack': {}
    },
        {
            '_id': test_id
        }
    ]
    mocker.patch.object(db, 'update', return_value={})
    res = client.put(f'/prefabs/{test_id}', json={'prefab': {'name': 'test prefab', 'rack': {}}})
    assert '200' in res.status


def test_delete_prefab_non_existing(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.delete(f'/prefabs/{test_id}').status


def test_delete_prefab_different_user(client, mocker):
    db.fetch_one = Mock()
    db.fetch_one.side_effect = [{
        '_id': test_id,
        'name': 'test prefab',
        'authorId': test_id_2,
        'visibility': 'private',
        'rack': {}
    },
        {
            '_id': test_id
        }
    ]
    mocker.patch.object(db, 'delete_one', return_value=None)
    assert '403' in client.delete(f'/prefabs/{test_id}').status


def test_delete_prefab(client, mocker):
    db.fetch_one = Mock()
    db.fetch_one.side_effect = [{
        '_id': test_id,
        'name': 'test prefab',
        'authorId': 'test',
        'visibility': 'private',
        'rack': {}
    },
        {
            '_id': test_id
        }
    ]
    mocker.patch.object(db, 'delete_one', return_value={'prefab': {'name': 'name'}})
    res = client.delete(f'/prefabs/{test_id}')
    assert '200' in res.status
