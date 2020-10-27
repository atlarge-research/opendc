def test_get_schedulers(client):
    assert '200' in client.get('/v2/schedulers').status
