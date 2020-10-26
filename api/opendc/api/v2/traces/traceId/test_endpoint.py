from opendc.util.database import DB

test_id = 24 * '1'


def test_get_trace_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get(f'/api/v2/traces/{test_id}').status


def test_get_trace(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'name': 'test trace'})
    res = client.get(f'/api/v2/traces/{test_id}')
    assert 'name' in res.json['content']
    assert '200' in res.status
