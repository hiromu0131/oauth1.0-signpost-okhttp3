import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import se.akerfeldt.okhttp.signpost.OkHttpOAuthConsumer;

@Controller
public class Main {

	private static final String CONTEXT = "http://localhost:8081";

	private static final String CONSUMER_KEY = "Your API key";
	private static final String CONSUMER_SECRET = "Your secret API key";
	private static final String CALLBACK_URL = "/result";
	private static final String REQUEST_TOKEN_URL = "https://www.hatena.com/oauth/initiate";
	private static final String AUTHORIZE_URL = "https://www.hatena.com/oauth/authorize";
	private static final String ACCESS_TOKEN_URL = "https://www.hatena.com/oauth/token";
	private static final String ATOM_ENTRY_URL = "https://blog.hatena.ne.jp/%s/%s/atom/entry";
	private String accountId = "Your accountID";
	private String domain = "Your blog domain";
	private Map<String, Integer> terms = new HashMap<String, Integer>();

	private OAuthConsumer consumer;
	private OAuthProvider provider;

	private String accessToken = "";
	private String accessTokenSecret = "";

	private static final String scope = "read_public%2Cread_private%2Cwrite_public%2Cwrite_private";


	@RequestMapping("/")
	public String index(Model model) {

	    consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
    	    provider = new CommonsHttpOAuthProvider(REQUEST_TOKEN_URL + "?scope=" + scope, ACCESS_TOKEN_URL, AUTHORIZE_URL);

	    String url = "";
	    try {
	        url = provider.retrieveRequestToken(consumer, CONTEXT + CALLBACK_URL);
	    } catch (OAuthMessageSignerException | OAuthNotAuthorizedException | OAuthExpectationFailedException | OAuthCommunicationException e) {
	        e.printStackTrace();
	    }

	    return "redirect:" + url;
	}

	@RequestMapping("/result")
	public String login(@RequestParam("oauth_verifier") String verifier, Model model) {

	    try {
		provider.retrieveAccessToken(consumer, verifier);
	    } catch (OAuthMessageSignerException | OAuthNotAuthorizedException | OAuthExpectationFailedException | OAuthCommunicationException e) {
	        e.printStackTrace();
	    }

            accessToken = consumer.getToken();
	    accessTokenSecret = consumer.getTokenSecret();
	    System.out.println(accessToken);

	    getInfo();

	    model.addAttribute("terms", terms);

	    return "result";
	}

	public void getInfo() {

	    OAuthConsumer cons = new OkHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
	    cons.setTokenWithSecret(accessToken, accessTokenSecret);

            try {
                Request signedRequest = (Request) cons.sign(
            		    new Request.Builder()
            		    .url(String.format(ATOM_ENTRY_URL, accountId, domain))
                            .build()).unwrap();

            OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
            Response response = okHttpClient.newCall(signedRequest).execute();

            System.out.println("responseCode: " + response.code());

            if (!response.isSuccessful()) {
                System.out.println("Response error");
            }
            if (response.body() != null) {
            	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(response.body().byteStream());
                Element bookList = document.getDocumentElement();
                NodeList categories = bookList.getElementsByTagName("category");

                for (int i = 0; i < categories.getLength(); i++) {
                    Element book = (Element) categories.item(i);

                    String term = book.getAttribute("term");
                    if (!terms.containsKey(term)) {
                        terms.put(term, 1);
                    } else {
                        terms.put(term, terms.get(term) + 1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
