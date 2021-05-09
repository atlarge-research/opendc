from opendc.util.database import DB


def test_get_traces(client, mocker):
    mocker.patch.object(DB, 'fetch_all', return_value=[])
    assert '200' in client.get('/v2/traces').status
