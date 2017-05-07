package bugapp.utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Class that allows to ignore SSL certificates.
 *
 * @author Alexander Kuleshov
 */
public class DummySSLFactory {
    /**
     * Method that generates SSL context with empty trust manager
     */
    public SSLContext getSSLContext() throws GeneralSecurityException {
        SSLContext sslContext = SSLContext.getInstance("TLSv1");
        sslContext.init(null, new TrustManager[] {trm}, new java.security.SecureRandom());
        return sslContext;
    }

    TrustManager trm = new X509TrustManager() {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {
            // dummy implementation of trust manager
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {
            // dummy implementation of trust manager
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };
}
