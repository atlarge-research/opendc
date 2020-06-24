from opendc.util.database import DB


def test_get_user_missing_parameter(client):
    assert '400' in client.get('/api/v2/users').status


def test_get_user_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/users?email=test@test.com').status


def test_get_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'email': 'test@test.com'})
    assert '200' in client.get('/api/v2/users?email=test@test.com').status


def test_add_user_missing_parameter(client):
    assert '400' in client.post('/api/v2/users').status


def test_add_user_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'email': 'test@test.com'})
    assert '409' in client.post('/api/v2/users', json={'user': {'email': 'test@test.com'}}).status


def test_add_user(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    mocker.patch.object(DB, 'insert', return_value={'email': 'test@test.com'})
    assert '200' in client.post('/api/v2/users', json={'user': {'email': 'test@test.com'}}).status
