package password_hash_raymond;

public interface Hasher
{
	public String hashPassword(String password);
	public String verifyHash(String passwordHash);
}
