from opendc.util.database import DB

test_id = 24 * '1'


def test_get_user_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get(f'/v2/users/{test_id}').status


def test_get_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'email': 'test@test.com'})
    res = client.get(f'/v2/users/{test_id}')
    assert 'email' in res.json['content']
    assert '200' in res.status


def test_update_user_missing_parameter(client):
    assert '400' in client.put(f'/v2/users/{test_id}').status


def test_update_user_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put(f'/v2/users/{test_id}', json={'user': {'givenName': 'A', 'familyName': 'B'}}).status


def test_update_user_different_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': test_id, 'googleId': 'other_test'})
    assert '403' in client.put(f'/v2/users/{test_id}', json={'user': {'givenName': 'A', 'familyName': 'B'}}).status


def test_update_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': test_id, 'googleId': 'test'})
    mocker.patch.object(DB, 'update', return_value={'givenName': 'A', 'familyName': 'B'})
    res = client.put(f'/v2/users/{test_id}', json={'user': {'givenName': 'A', 'familyName': 'B'}})
    assert 'givenName' in res.json['content']
    assert '200' in res.status


def test_delete_user_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete(f'/v2/users/{test_id}').status


def test_delete_user_different_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': test_id, 'googleId': 'other_test'})
    assert '403' in client.delete(f'/v2/users/{test_id}').status


def test_delete_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': test_id, 'googleId': 'test', 'authorizations': []})
    mocker.patch.object(DB, 'delete_one', return_value={'googleId': 'test'})
    res = client.delete(f'/v2/users/{test_id}', )

    assert 'googleId' in res.json['content']
    assert '200' in res.status
