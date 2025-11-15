package password_hash_raymond;

public class Password4jHasher implements Hasher
{
	public String hashPassword(String password)
	{
		return password;
	}

	public String verifyHash(String passwordHash)
	{
		return "";
	}

}
