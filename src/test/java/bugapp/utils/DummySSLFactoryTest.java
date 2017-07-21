package bugapp.utils;

import junit.framework.TestCase;

import java.security.GeneralSecurityException;

/**
 * @author Alexander Kuleshov
 */
public class DummySSLFactoryTest extends TestCase {
    public void testCreateSslContext() throws GeneralSecurityException {
        DummySSLFactory sslFactory = new DummySSLFactory();
        assertNotNull(sslFactory.getSSLContext());
    }
}
