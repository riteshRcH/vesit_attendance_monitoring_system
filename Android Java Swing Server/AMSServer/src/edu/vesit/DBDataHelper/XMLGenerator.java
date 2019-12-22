package edu.vesit.DBDataHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.vesit.ams.AMSServer;

public class XMLGenerator
{
	Connection conn;
	Statement stmt;
	ResultSet rs;
	
	ArrayList<String> DBNames = new ArrayList<String>();
	
	String DBVendor, DBServerIPPort;
	DatabaseMetaData dbmd;
	
	ArrayList<String> excludeForClassficationXMLCreation = new ArrayList<String>();
	
	Logger logger = LoggerFactory.getLogger(XMLGenerator.class);
	String tempLogMsg;
	
	public XMLGenerator(String DBVendor, String DBServerIPPort)
	{
		this.DBVendor = DBVendor;
		this.DBServerIPPort = DBServerIPPort;
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:"+DBVendor+"://"+DBServerIPPort, MainFrame.DBusername, MainFrame.DBpassword);
			conn.setReadOnly(true);
			stmt = conn.createStatement();
			//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.LoginHandler] DB Connection for Presets created ..");
			logger.info("DB Connection for Presets created!");
			dbmd = conn.getMetaData();
			rs = dbmd.getCatalogs();
			while(rs.next())
			{
				String DBName = rs.getString(1);
				if(DBName.startsWith("vesitams"))
					DBNames.add(DBName);
			}
			rs.close();
		}catch(SQLException e)
		{
			e.printStackTrace();
		}catch(ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		excludeForClassficationXMLCreation.add("attendance_");
		excludeForClassficationXMLCreation.add("registeredteachers");
		excludeForClassficationXMLCreation.add("presetsallotment");
	}
	public boolean createPresetsXMLForAllDB()			//returns true on success else false
	{
		try
		{	
			AMSServer.presetsDataDir.mkdirs();
			for(String DBName:DBNames)
			{
				PrintWriter pw = new PrintWriter(new FileWriter(new File(AMSServer.presetsDataDir, DBName+"_presets_allotment.xml")));
				pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
				pw.println("<"+DBName+">");
				
				//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.LoginHandler] DB: "+DBName+".attendance_* qualified names ONLY is being converted to its equivalent XML representaion to export to application ..");
				tempLogMsg = "DB: "+DBName+".attendance_* qualified names ONLY is being converted to its equivalent XML representaion to export to application ..";
				
				rs = dbmd.getTables(DBName, null, "attendance_%", null);
				
				//XML File writing
				while(rs.next())
				{
					pw.println("<Preset>");
					String presetTableName = rs.getString("TABLE_NAME");
					pw.println("<presetTableName>"+presetTableName+"</presetTableName>");
					pw.println("<DurationHours>"+getnumHoursDurationForSubject(DBName, presetTableName)+"</DurationHours>");
					
					String teachersUsernamePWSaltNumStudents[] = getTeachersUsernamePasswordSaltNumStudents(DBName, presetTableName);
					//need this because teacher username and pw would be provided by teacher while logging in and that will be used further to filter presets so presets xml file needs username and pw
					pw.println("<TeacherUsername>"+teachersUsernamePWSaltNumStudents[0]+"</TeacherUsername>");
					pw.println("<TeacherHashedPW>"+teachersUsernamePWSaltNumStudents[1]+"</TeacherHashedPW>");
					pw.println("<TeacherSalt>"+teachersUsernamePWSaltNumStudents[2]+"</TeacherSalt>");
					pw.println("<numStudents>"+teachersUsernamePWSaltNumStudents[3]+"</numStudents>");
					pw.println("</Preset>");
				}
				pw.println("</"+DBName+">");
				//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.LoginHandler] DB: "+DBName+".attendance_* qualified names converted Successfully!");
				logger.info(tempLogMsg += "DB: "+DBName+".attendance_* qualified names converted Successfully!");
				pw.close();
				rs.close();
			}
			
			//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.LoginHandler] XML File creation done!");
			logger.info("XML File creation done!");
			
			//EXI File
			//edu.vesit.AMS.EncodeXMLToEXI.encodeXMLToEXI(xmlFile.getAbsolutePath().trim(), new File(xmlFile.getParent(), "presets_allotment.exi").getAbsolutePath());
			
			//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.LoginHandler] Successfully created EXI for qualified Presets Name!");
			
			return true;
		}catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	public boolean createClassficationXMLs()
	{
		try
		{
			AMSServer.classificationDataDir.mkdirs();
			for(String DBName:DBNames)
			{	
				File DBDataDir = new File(AMSServer.classificationDataDir, DBName);
				if(!DBDataDir.exists())
					DBDataDir.mkdirs();
				
				rs = dbmd.getTables(DBName, null, "%", new String[]{"TABLE"});
				while(rs.next())
				{
					String tableName = rs.getString("TABLE_NAME");
					if(isTableNeeded(tableName))
					{
						//System.out.print("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.LoginHandler] Relation: "+tableName+" is being converted to its equivalent XML representaion ..");
						tempLogMsg = "Relation: "+tableName+" is being converted to its equivalent XML representaion ..";
						
						ResultSet allTuples = stmt.executeQuery("select * from "+DBName+"."+tableName);
						
						ResultSetMetaData rsmd = allTuples.getMetaData();
						ArrayList<String> allColumnNames = new ArrayList<String>();
						for(int i=1;i<=rsmd.getColumnCount();i++)
							allColumnNames.add(rsmd.getColumnName(i));
						
						//XML File
						PrintWriter pw = new PrintWriter(new FileWriter(new File(DBDataDir, tableName+".xml")));
						pw.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
						pw.println("<?xml-stylesheet type=\"text/xsl\" href=\""+tableName+".xsl\"?>");
						pw.println("<Root"+tableName+">");
						while(allTuples.next())
						{
							pw.println("<"+tableName+">");
							for(String column:allColumnNames)
							{
								Object o = allTuples.getObject(column);
								pw.println("<"+column+">"+(o==null?"null":o.toString())+"</"+column+">");
							}
							pw.println("</"+tableName+">");
						}
						pw.println("</Root"+tableName+">");
						pw.close();
						logger.info(tempLogMsg += "Converted Successfully!");
						
						//XSL File
						/*System.out.print("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" - AMSDBDataHelper.LoginHandler] Relation: "+tableName+" is being converted to its equivalent XSL representaion ..");
						pw = new PrintWriter(new FileWriter(new File(DBDataDir, tableName+".xsl")));
						pw.println("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n"+
									"<xsl:template match=\"/\">\n"+
									"<html>\n"+
									"<body>\n"+
									"<table border=\"1\">\n"+
									"<tr>");
						for(String col:allColumnNames)
							pw.println("<th>"+col+"</th>");
						pw.println("</tr>");
						pw.println("<xsl:for-each select=\"Root"+tableName+"/"+tableName+"\">\n<tr>	");
						for(String col:allColumnNames)
							pw.println("<td><xsl:value-of select=\""+col+"\" /></td>");
						pw.println("</tr>\n"+
								"</xsl:for-each>\n"+
								"</table>\n"+
								"</body>\n"+
								"</html>\n"+
								"</xsl:template>\n"+
								"</xsl:stylesheet>");
						pw.close();
						System.out.println("Converted Successfully!");*/
					}
				}
				rs.close();
				stmt.close();
				conn.close();
			}	
			return true;
		}catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	private boolean isTableNeeded(String tableName)
	{
		for(String s:excludeForClassficationXMLCreation)
			if(tableName.startsWith(s) || tableName.equals(s))
				return false;
		return true;
	}
	private String getnumHoursDurationForSubject(String DBName, String presetTableName)
	{
		String subjectCode = presetTableName.substring(0, presetTableName.lastIndexOf("_"));
		String lectureType = presetTableName.substring(presetTableName.lastIndexOf("_")+1);
		subjectCode = subjectCode.substring(subjectCode.lastIndexOf('_')+1);
		
		try
		{
			ResultSet resultSet = stmt.executeQuery("select DurationHours from "+DBName+".lectures where LOWER(subjectCode) = LOWER('"+subjectCode+"') and LOWER(LectureType)=LOWER('"+lectureType+"')");
			resultSet.next();
			return Integer.toString(resultSet.getInt(1));
		}catch (SQLException e)
		{
			e.printStackTrace();
		}
		return "1";
	}
	private String[] getTeachersUsernamePasswordSaltNumStudents(String DBName, String presetTableName)
	{
		String teacherName = presetTableName.substring(presetTableName.indexOf('_')+1);
		teacherName = teacherName.substring(0, teacherName.indexOf('_'));
		
		String divisionName = presetTableName.substring(0, presetTableName.lastIndexOf('_'));
		divisionName = divisionName.substring(0, divisionName.lastIndexOf('_'));
		divisionName = divisionName.substring(divisionName.lastIndexOf('_')+1);
		
		try
		{
			ResultSet resultSet = stmt.executeQuery("select Username, Password, salt from "+DBName+".registeredteachers where TeacherName='"+teacherName+"'");
			resultSet.next();
			String toReturnArr[] = new String[4];
			toReturnArr[0] = resultSet.getString("Username");
			toReturnArr[1] = resultSet.getString("Password");
			toReturnArr[2] = resultSet.getString("salt");
			
			resultSet.close();
			resultSet = stmt.executeQuery("select numStudents from "+DBName+".divisions where LOWER(DivName)=LOWER('"+divisionName+"')");
			resultSet.next();
			toReturnArr[3] = Integer.toString(resultSet.getInt("numStudents"));
			resultSet.close();
			
			return toReturnArr;
		}catch(SQLException sqle)
		{
			sqle.printStackTrace();
			return new String[]{null, null};
		}
	}
}