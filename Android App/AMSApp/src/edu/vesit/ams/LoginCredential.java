package edu.vesit.ams;

public class LoginCredential
{
	private String username, password, dbName;
	private boolean lastUsed = false;

	String getUsername()
	{
		return username;
	}
	void setUsername(String username)
	{
		this.username = username;
	}
	String getPassword()
	{
		return password;
	}
	void setPassword(String password)
	{
		this.password = password;
	}
	boolean getLastUsed()
	{
		return lastUsed;
	}
	void setLastUsed(boolean lastUsed)
	{
		this.lastUsed = lastUsed;
	}
	String getDbName()
	{
		return dbName;
	}
	void setDbName(String dbName)
	{
		this.dbName = dbName;
	}
}