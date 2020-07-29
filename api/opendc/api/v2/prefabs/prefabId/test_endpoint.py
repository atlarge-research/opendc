from opendc.util.database import DB
from unittest.mock import Mock


def test_get_prefab_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/prefabs/1').status

def test_get_private_prefab_not_authorized(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
                            '_id': '1',
                            'name': 'test prefab',
                            'authorId': '2',
                            'visibility': 'private',
                            'rack': {}
                        },
                        {
                            '_id': '1'
                        }
                        ]
    res = client.get('/api/v2/prefabs/1')
    assert '403' in res.status


def test_get_private_prefab(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
                            '_id': '1',
                            'name': 'test prefab',
                            'authorId': '1',
                            'visibility': 'private',
                            'rack': {}
                        },
                        {
                            '_id': '1'
                        }
                        ]
    res = client.get('/api/v2/prefabs/1')
    assert '200' in res.status

def test_get_public_prefab(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
                            '_id': '1',
                            'name': 'test prefab',
                            'authorId': '2',
                            'visibility': 'public',
                            'rack': {}
                        },
                        {
                            '_id': '1'
                        }
                        ]
    res = client.get('/api/v2/prefabs/1')
    assert '200' in res.status


def test_update_prefab_missing_parameter(client):
    assert '400' in client.put('/api/v2/prefabs/1').status


def test_update_prefab_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put('/api/v2/prefabs/1', json={'prefab': {'name': 'S'}}).status


def test_update_prefab_not_authorized(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
                            '_id': '1',
                            'name': 'test prefab',
                            'authorId': '2',
                            'visibility': 'private',
                            'rack': {}
                        },
                        {
                            '_id': '1'
                        }
                        ]
    mocker.patch.object(DB, 'update', return_value={})
    assert '403' in client.put('/api/v2/prefabs/1', json={'prefab': {'name': 'test prefab', 'rack' : {}}}).status


def test_update_prefab(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
                            '_id': '1',
                            'name': 'test prefab',
                            'authorId': '1',
                            'visibility': 'private',
                            'rack': {}
                        },
                        {
                            '_id': '1'
                        }
                        ]
    mocker.patch.object(DB, 'update', return_value={})
    res = client.put('/api/v2/prefabs/1', json={'prefab': {'name': 'test prefab', 'rack' : {}}})
    assert '200' in res.status


def test_delete_prefab_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete('/api/v2/prefabs/1').status


def test_delete_prefab_different_user(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
                            '_id': '1',
                            'name': 'test prefab',
                            'authorId': '2',
                            'visibility': 'private',
                            'rack': {}
                        },
                        {
                            '_id': '1'
                        }
                        ]
    mocker.patch.object(DB, 'delete_one', return_value=None)
    assert '403' in client.delete('/api/v2/prefabs/1').status


def test_delete_prefab(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
                            '_id': '1',
                            'name': 'test prefab',
                            'authorId': '1',
                            'visibility': 'private',
                            'rack': {}
                        },
                        {
                            '_id': '1'
                        }
                        ]
    mocker.patch.object(DB, 'delete_one', return_value={'prefab': {'name': 'name'}})
    res = client.delete('/api/v2/prefabs/1')
    assert '200' in res.status
