package edu.dartmouth.cs.funbewell;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

public class HttpClientLooseSSL extends DefaultHttpClient {

    @Override
    protected ClientConnectionManager createClientConnectionManager() {
	SchemeRegistry registry = new SchemeRegistry();
	KeyStore trustStore = null;
	try {
	    trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
	} catch (KeyStoreException e) {
	    e.printStackTrace();
	}
	try {
	    trustStore.load(null, null);
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (NoSuchAlgorithmException e) {
	    e.printStackTrace();
	} catch (CertificateException e) {
	    e.printStackTrace();
	}
	registry.register(new Scheme("http", PlainSocketFactory
		.getSocketFactory(), 80));
	// Register for port 443 our SSLSocketFactory with our keystore
	// to the ConnectionManager
	try {
	    registry.register(new Scheme("https", new MySSLSocketFactory(
		    trustStore), 443));
	} catch (NoSuchAlgorithmException e) {
	    e.printStackTrace();
	} catch (KeyManagementException e) {
	    e.printStackTrace();
	} catch (KeyStoreException e) {
	    e.printStackTrace();
	} catch (UnrecoverableKeyException e) {
	    e.printStackTrace();
	}
	return new SingleClientConnManager(getParams(), registry);
    }
}
