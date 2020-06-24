def test_get_user_missing_parameter(client):
    print(client.get('/api/v2/users'))
