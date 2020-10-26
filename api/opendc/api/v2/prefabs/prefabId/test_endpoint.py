from opendc.util.database import DB
from unittest.mock import Mock

test_id = 24 * '1'
test_id_2 = 24 * '2'


def test_get_prefab_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get(f'/api/v2/prefabs/{test_id}').status


def test_get_private_prefab_not_authorized(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
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
    res = client.get(f'/api/v2/prefabs/{test_id}')
    assert '403' in res.status


def test_get_private_prefab(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
        '_id': test_id,
        'name': 'test prefab',
        'authorId': test_id,
        'visibility': 'private',
        'rack': {}
    },
        {
            '_id': test_id
        }
    ]
    res = client.get(f'/api/v2/prefabs/{test_id}')
    assert '200' in res.status


def test_get_public_prefab(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
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
    res = client.get(f'/api/v2/prefabs/{test_id}')
    assert '200' in res.status


def test_update_prefab_missing_parameter(client):
    assert '400' in client.put(f'/api/v2/prefabs/{test_id}').status


def test_update_prefab_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put(f'/api/v2/prefabs/{test_id}', json={'prefab': {'name': 'S'}}).status


def test_update_prefab_not_authorized(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
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
    mocker.patch.object(DB, 'update', return_value={})
    assert '403' in client.put(f'/api/v2/prefabs/{test_id}', json={'prefab': {'name': 'test prefab', 'rack': {}}}).status


def test_update_prefab(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
        '_id': test_id,
        'name': 'test prefab',
        'authorId': test_id,
        'visibility': 'private',
        'rack': {}
    },
        {
            '_id': test_id
        }
    ]
    mocker.patch.object(DB, 'update', return_value={})
    res = client.put(f'/api/v2/prefabs/{test_id}', json={'prefab': {'name': 'test prefab', 'rack': {}}})
    assert '200' in res.status


def test_delete_prefab_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete(f'/api/v2/prefabs/{test_id}').status


def test_delete_prefab_different_user(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
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
    mocker.patch.object(DB, 'delete_one', return_value=None)
    assert '403' in client.delete(f'/api/v2/prefabs/{test_id}').status


def test_delete_prefab(client, mocker):
    DB.fetch_one = Mock()
    DB.fetch_one.side_effect = [{
        '_id': test_id,
        'name': 'test prefab',
        'authorId': test_id,
        'visibility': 'private',
        'rack': {}
    },
        {
            '_id': test_id
        }
    ]
    mocker.patch.object(DB, 'delete_one', return_value={'prefab': {'name': 'name'}})
    res = client.delete(f'/api/v2/prefabs/{test_id}')
    assert '200' in res.status
