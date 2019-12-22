package edu.vesit.DBDataHelper;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class SearchDB extends JFrame implements ActionListener
{	
	JComboBox<String> jcomboBoxSelectRelation;
	Vector<String> allRelations = new Vector<String>();
	JButton jbtnSearch;
	JLabel jlblHeading;
	JTable jtableResultSet;
	Connection conn;
	Statement stmt;
	ResultSet rs;
	JPanel jpOperationsMenu = new JPanel();
	
	Logger logger = LoggerFactory.getLogger(SearchDB.class);
	
	public SearchDB(String DBVendor, String DBServerIPPort, String dbName)
	{
		this.setLayout(new BorderLayout());
		jpOperationsMenu.setLayout(new FlowLayout());
		
		this.setSize(600, 200);
		this.setResizable(false);
		this.setTitle("Search DB");
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setVisible(true);
		this.setLocationRelativeTo(null);
		
		jpOperationsMenu.add(jlblHeading = new JLabel("Choose table to search in"));
		jlblHeading.setFont(new Font("Algerian", 10, 40));
		
		jpOperationsMenu.add(jcomboBoxSelectRelation = new JComboBox<String>(new String[]{}));
		jpOperationsMenu.add(jbtnSearch = new JButton("Search"));
		
		this.add(jpOperationsMenu, BorderLayout.CENTER);
		
		jbtnSearch.addActionListener(this);
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:"+DBVendor+"://"+DBServerIPPort+"/"+dbName, MainFrame.DBusername, MainFrame.DBpassword);
			//conn = DriverManager.getConnection("jdbc:odbc:MSSQLSampleFinanceInstitution");
			//System.out.println("[ "+AMSServer.logEventTimestampFormat.format(System.currentTimeMillis())+" ] DB Connection Success!");
			logger.info("DB Connection Success for DBSearch!");
			stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ResultSet rsAllTables = conn.getMetaData().getTables(null, null, "%", null);
			while(rsAllTables.next())
			{
				String s = rsAllTables.getString("TABLE_NAME");
				if(!allRelations.contains(s))
						allRelations.add(s);
			}
			rsAllTables.close();
		}catch(Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Exception occured while connecting to DB, retry", "Exception occured", JOptionPane.ERROR_MESSAGE);
			this.dispose();
		}
		for(String tableName:allRelations)
			jcomboBoxSelectRelation.addItem(tableName);
		
		this.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent we)
			{
				int chosenOption = JOptionPane.showConfirmDialog(SearchDB.this, "All searching related active DB connections would be closed .. proceed?", "Warning", JOptionPane.YES_NO_OPTION);
				if(chosenOption==JOptionPane.YES_OPTION)
				{
					try
					{
						conn.close();
						SearchDB.this.dispose();
					}catch (SQLException e)
					{
						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		if(ae.getSource().equals(jbtnSearch))
			new jframeFormulateSearchQuery(jcomboBoxSelectRelation.getSelectedItem().toString());
	}
	class jframeFormulateSearchQuery extends JFrame
	{
		String selectedTableName = new String();
		JComboBox<String> jcomboBoxOperators = new JComboBox<String>(new String[]{">", "<", "=", "!=", "<=", ">=", " like "}); 
		public jframeFormulateSearchQuery(String selectedTableName)
		{
			this.selectedTableName = selectedTableName;
			
			this.setLayout(new FlowLayout());
			this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			this.setSize(500, 400);
			this.setVisible(true);
			this.setTitle("Formulate Search Query for "+selectedTableName);
			
			
		}
	}
}