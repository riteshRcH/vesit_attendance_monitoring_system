package edu.vesit.DBDataHelper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class LoginHandler
{	
	private HikariDataSource ds;
	//private Connection conn;
	//private Statement stmt;
	//private ResultSet rs;
	public boolean acceptLogins;
	
	Logger logger = LoggerFactory.getLogger(LoginHandler.class);

	public LoginHandler(String DBVendor, String DBServerIPPort, String dbName)
	{
		HikariConfig config = new HikariConfig();
		config.setMinimumPoolSize(22);
		config.setMaximumPoolSize(100);
		config.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
		config.addDataSourceProperty("url", "jdbc:"+DBVendor+"://"+DBServerIPPort);
		config.addDataSourceProperty("user", MainFrame.DBusername);
		config.addDataSourceProperty("password", MainFrame.DBpassword);

		ds = new HikariDataSource(config);
		
		/*Class.forName("com.mysql.jdbc.Driver");
		conn = DriverManager.getConnection("jdbc:"+DBVendor+"://"+DBServerIPPort, MainFrame.DBusername, MainFrame.DBpassword);
		conn.setReadOnly(true);
		stmt = conn.createStatement();*/
		//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.LoginHandler] Connection for login established, ready to accept logins ..");
		//logger.info("Connection for login established, ready to accept logins ..");
		logger.info("Data Source for login established, ready to accept logins ..");
		acceptLogins = true;
	}
	//removed synchronized modifier
	public boolean loginValidate(String username, String password, String dbName)
	{
		try
		{
			if(acceptLogins)
			{
				Connection conn = ds.getConnection();
				conn.setReadOnly(true);
				
				//password = sha1(password + md5(salt + password) + salt) .. this logic created in after insert trigger of DB and a random 16char salt is generated and stored per user
				ResultSet rs = conn.createStatement().executeQuery("select count(*) as 'usernamePwCount' from "+dbName+".registeredteachers where Username='"+username+"' and Password=sha1(concat('"+password+"', md5(concat(salt, '"+password+"')), salt))");
				if(rs.next())
					return rs.getInt("usernamePwCount")==1;
				else
					return false;
			}else
				return false;
		}catch (SQLException e) 
		{
			e.printStackTrace();
			return false;
		}	
	}
	public void toggleAcceptLogins()
	{
		this.acceptLogins = !this.acceptLogins;
		//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.LoginHandler] "+(this.acceptLogins?"":"Login Disabled, NOT ")+"Ready to accept logins ..");
		logger.info((this.acceptLogins?"":"Login Disabled, NOT ")+"Ready to accept logins ..");
	}
	public HikariDataSource getDataSource()
	{
		return ds;
	}
}
