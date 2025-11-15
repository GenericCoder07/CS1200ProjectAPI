package user_database;
import java.security.SecureRandom;
import java.util.Base64;

public final class Sessions 
{
	private static final SecureRandom RNG = new SecureRandom();

	public static String newSessionId() 
	{
		byte[] buf = new byte[32];
		RNG.nextBytes(buf);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
	}
}