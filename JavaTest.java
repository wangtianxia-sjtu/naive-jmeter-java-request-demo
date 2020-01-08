import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

public class JavaTest extends AbstractJavaSamplerClient implements Serializable {
	
	private static final long serialVersionUID = 241L;
	
	private static ConcurrentMap<String, String> short2long = new ConcurrentHashMap<>();
	
	private static final String URL_ADDRESS = "Url_Address";
	private static final String DEFAULT_URL_ADDRESS = "http://202.120.40.8:30364/";
	private String urlAddress;
	
	private static final String READ_TIMES = "Read_Times";
	private static final int DEFAULT_READ_TIMES = 100;
	private int readTimes;
	
	private static final String WRITE_TIMES = "Write_Times";
	private static final int DEFAULT_WRITE = 10;
	private int writeTimes;
	
	/**
	 * [low, high)
	 */
	private static int getRandomNumber(int low, int high) {
		return (int)(low + Math.random()*(high-low));
	}
	

    /**
     * Utility method to set up all the values
     */
    private void setupValues(JavaSamplerContext context) {
    	urlAddress = context.getParameter(URL_ADDRESS, DEFAULT_URL_ADDRESS);
    	readTimes = context.getIntParameter(READ_TIMES, DEFAULT_READ_TIMES);
    	writeTimes = context.getIntParameter(WRITE_TIMES, DEFAULT_WRITE);
    }

    /**
     * Do any initialization required by this client.
     *
     * There is none, as it is done in runTest() in order to be able to vary the
     * data for each sample.
     */
    @Override
    public void setupTest(JavaSamplerContext context) {
        
    }
    
    private String getLongLink(InputStreamReader inputStreamReader) throws IOException {
    	BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
    	String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = bufferedReader.readLine()) != null) {
            content.append(inputLine);
        }
        return content.toString();
    }

    /**
     * Provide a list of parameters which this test supports. Any parameter
     * names and associated values returned by this method will appear in the
     * GUI by default so the user doesn't have to remember the exact names. The
     * user can add other parameters which are not listed here. If this method
     * returns null then no parameters will be listed. If the value for some
     * parameter is null then that parameter will be listed in the GUI with an
     * empty value.
     *
     * @return a specification of the parameters used by this test which should
     *         be listed in the GUI, or null if no parameters should be listed.
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments params = new Arguments();
        params.addArgument(URL_ADDRESS, DEFAULT_URL_ADDRESS);
        params.addArgument(READ_TIMES, String.valueOf(DEFAULT_READ_TIMES));
        params.addArgument(WRITE_TIMES, String.valueOf(DEFAULT_WRITE));
        return params;
    }

    /**
     * Perform a single sample.<br>
     * In this case, this method will simply sleep for some amount of time.
     *
     * This method returns a <code>SampleResult</code> object.
     *
     * <pre>
     *
     *  The following fields are always set:
     *  - responseCode (default &quot;&quot;)
     *  - responseMessage (default &quot;&quot;)
     *  - label (set from LABEL_NAME parameter if it exists, else element name)
     *  - success (default true)
     *
     * </pre>
     *
     * The following fields are set from the user-defined parameters, if
     * supplied:
     *
     * <pre>
     * -samplerData - responseData
     * </pre>
     *
     * @see org.apache.jmeter.samplers.SampleResult#sampleStart()
     * @see org.apache.jmeter.samplers.SampleResult#sampleEnd()
     * @see org.apache.jmeter.samplers.SampleResult#setSuccessful(boolean)
     * @see org.apache.jmeter.samplers.SampleResult#setSampleLabel(String)
     * @see org.apache.jmeter.samplers.SampleResult#setResponseCode(String)
     * @see org.apache.jmeter.samplers.SampleResult#setResponseMessage(String)
     * @see org.apache.jmeter.samplers.SampleResult#setResponseData(byte[])
     * @see org.apache.jmeter.samplers.SampleResult#setDataType(String)
     *
     * @param context
     *            the context to run with. This provides access to
     *            initialization parameters.
     *
     * @return a SampleResult giving the results of this sample.
     */
    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        setupValues(context);
        SampleResult results = new SampleResult();
        results.sampleStart();
        HttpURLConnection connection = null;
        BufferedReader br = null;
        results.setSuccessful(true);
        results.setResponseMessage("OK");
        try {
        	for (int i = 0; i < writeTimes; ++i) {
        		String uuid = UUID.randomUUID().toString().replaceAll("-","");
        		URL url = new URL(urlAddress + "long?long_link=" + uuid);
        		connection = (HttpURLConnection) url.openConnection();
        		connection.setRequestMethod("POST");
                connection.setConnectTimeout(150000);
                connection.setReadTimeout(600000);
                br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                String temp = null;
                temp = br.readLine();
                short2long.put(temp, uuid);
                connection.disconnect();
        	}
        	
        	Object[] s = short2long.keySet().toArray();
        	int len = s.length;
        	for (int i = 0; i < readTimes; ++i) {
        		String shortLink = (String) s[getRandomNumber(0, len)];
        		URL url = new URL(urlAddress + "short?short_link=" + shortLink);
        		connection = (HttpURLConnection) url.openConnection();
        		connection.setRequestMethod("GET");
        		connection.setConnectTimeout(150000);
        		connection.setReadTimeout(600000);
        		connection.setInstanceFollowRedirects(false);
                int reponseCode = connection.getResponseCode();
                if (reponseCode != 302)
                    throw new Exception("Fail to redirect. " + shortLink + " got code " + reponseCode);
        		String longLink = connection.getHeaderField("Location");
        		// String longLink = getLongLink(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        		if (longLink == null)
        			throw new Exception("Fail to redirect. Expected: " + shortLink + " to " + short2long.get(shortLink));
                String expected = short2long.get(shortLink);
        		if (!longLink.substring(1).equals(expected) && !longLink.equals(expected))
        			throw new Exception("Expected: " + expected + " Output: " + longLink);
                connection.disconnect();
        	}
        }
        catch (Exception e) {
        	results.setResponseMessage(e.getMessage());
        	results.setSuccessful(false);
        	connection.disconnect();
        }
        results.sampleEnd();
        return results;
    }
}