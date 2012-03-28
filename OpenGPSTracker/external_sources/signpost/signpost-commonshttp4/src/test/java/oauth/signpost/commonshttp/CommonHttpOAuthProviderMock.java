package oauth.signpost.commonshttp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import oauth.signpost.http.HttpRequest;
import oauth.signpost.mocks.OAuthProviderMock;

import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.ProtocolVersion;
import org.apache.ogt.http.StatusLine;
import org.apache.ogt.http.client.HttpClient;
import org.apache.ogt.http.client.methods.HttpUriRequest;
import org.apache.ogt.http.entity.InputStreamEntity;
import org.apache.ogt.http.message.BasicStatusLine;
import org.mockito.Mockito;

@SuppressWarnings("serial")
public class CommonHttpOAuthProviderMock extends CommonsHttpOAuthProvider implements
        OAuthProviderMock {

    private HttpClient httpClientMock;

    public CommonHttpOAuthProviderMock(String requestTokenUrl, String accessTokenUrl,
            String websiteUrl) {
        super(requestTokenUrl, accessTokenUrl, websiteUrl);
    }

    @Override
    protected oauth.signpost.http.HttpResponse sendRequest(HttpRequest request) throws Exception {
        HttpResponse resp = httpClientMock.execute((HttpUriRequest) request.unwrap());
        return new HttpResponseAdapter(resp);
    }

    public void mockConnection(String responseBody) throws Exception {
        HttpResponse response = mock(HttpResponse.class);
        this.httpClientMock = mock(HttpClient.class);
        InputStream is = new ByteArrayInputStream(responseBody.getBytes());
        InputStreamEntity entity = new InputStreamEntity(is, responseBody.length());
        StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK");

        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(entity);
        when(httpClientMock.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
    }
}
