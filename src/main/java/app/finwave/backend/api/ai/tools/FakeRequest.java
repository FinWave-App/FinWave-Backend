package app.finwave.backend.api.ai.tools;

import spark.QueryParamsMap;
import spark.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FakeRequest extends Request {
    protected Map<String, String> fakeParams;
    protected HashMap<String, Object> fakeAttributes = new HashMap<>();

    public FakeRequest(Map<String, String> params) {
        this.fakeParams = params;
    }

    @Override
    public void attribute(String attribute, Object value) {
        fakeAttributes.put(attribute, value);
    }

    @Override
    public <T> T attribute(String attribute) {
        return (T) fakeAttributes.get(attribute);
    }

    @Override
    public Set<String> attributes() {
        return fakeAttributes.keySet();
    }

    @Override
    public String queryParams(String queryParam) {
        return fakeParams.get(queryParam);
    }

    @Override
    public String queryParamsSafe(String queryParam) {
        return java.util.Base64.getUrlEncoder().encodeToString(queryParams(queryParam).getBytes());
    }

    @Override
    public String[] queryParamsValues(String queryParam) {
        return fakeParams.values().toArray(new String[0]);
    }

    @Override
    public Set<String> queryParams() {
        return fakeParams.keySet();
    }

    @Override
    public String queryString() {
        return fakeParams.toString(); // wrong imitation
    }

    @Override
    public QueryParamsMap queryMap() {
        return new FakeQueryParamsMap(
                fakeParams.entrySet()
                        .stream()
                        .map(e -> Map.entry(e.getKey(), new String[] {e.getValue()}))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    protected static class FakeQueryParamsMap extends QueryParamsMap {
        public FakeQueryParamsMap(Map<String, String[]> params) {
            super(params);
        }
    }
}
