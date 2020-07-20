from opendc.util.database import DB


def test_get_trace_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/traces/1').status


def test_get_trace(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'name': 'test trace'})
    res = client.get('/api/v2/traces/1')
    assert 'name' in res.json['content']
    assert '200' in res.status
