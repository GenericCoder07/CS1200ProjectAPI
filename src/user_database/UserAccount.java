package user_database;
public class UserAccount
{
	public String username;
	public String password;
	public String email;
	public String session_id;
	public long session_timestamp;
	public boolean isAdmin;
	public boolean isVerified;

	public UserAccount(String username, String password, String email, boolean isAdmin)
	{
		this.username = username;
		this.password = password;
		this.email = email;
		this.isAdmin = isAdmin;
	}

	protected void setSystemVars(String session_id, long session_timestamp, boolean isVerified)
	{
		this.session_id = session_id;
		this.session_timestamp = session_timestamp;
		this.isVerified = isVerified;
	}
}