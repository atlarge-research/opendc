from opendc.util.database import DB


def test_get_user_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/users/1').status


def test_get_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'email': 'test@test.com'})
    res = client.get('/api/v2/users/1')
    assert 'email' in res.json['content']
    assert '200' in res.status


def test_update_user_missing_parameter(client):
    assert '400' in client.put('/api/v2/users/1').status


def test_update_user_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put('/api/v2/users/1', json={'user': {'givenName': 'A', 'familyName': 'B'}}).status


def test_update_user_different_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'googleId': 'other_test'})
    assert '403' in client.put('/api/v2/users/1',
                               json={
                                   'user': {
                                       'givenName': 'A',
                                       'familyName': 'B'
                                   }
                               }).status


def test_update_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'googleId': 'test'})
    mocker.patch.object(DB, 'update', return_value=None)
    res = client.put('/api/v2/users/1', json={'user': {'givenName': 'A', 'familyName': 'B'}})
    assert 'givenName' in res.json['content']
    assert '200' in res.status


def test_delete_user_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete('/api/v2/users/1').status


def test_delete_user_different_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'googleId': 'other_test'})
    assert '403' in client.delete('/api/v2/users/1').status


def test_delete_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'googleId': 'test'})
    mocker.patch.object(DB, 'delete_one', return_value=None)
    res = client.delete('/api/v2/users/1')
    assert 'googleId' in res.json['content']
    assert '200' in res.status
