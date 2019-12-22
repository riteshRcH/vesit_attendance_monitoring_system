package edu.vesit.DBDataHelper;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
public class MainFrame extends JFrame implements ActionListener
{
	JButton searchDBBtn, jbtnModRecordsFrame, XMLDBBtn, jbtnCreateAttendanceTableForPresets;
	JLabel jlblProjName;
	JPanel jpOperationsMenu = new JPanel();
	
	static String dbName, DBServerIPPort, DBVendor;
	public static String DBusername = "AMSServerDBBot", DBpassword = "VESIT9521AMSBot";
	
	public MainFrame(String DBVendor, String DBServerIPPort, String dbName)
	{
		setLayout(new FlowLayout(FlowLayout.CENTER));
		this.setSize(600, 500);
		this.setResizable(false);
		this.setTitle("VESIT AMS DB Admin Console");
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		
		MainFrame.dbName = dbName;
		MainFrame.DBServerIPPort = DBServerIPPort;
		MainFrame.DBVendor = DBVendor;
		
		GridLayout gridLayout = new GridLayout(0, 1);
		gridLayout.setVgap(22);
		gridLayout.setHgap(22);
		jpOperationsMenu.setLayout(gridLayout);
		
		this.add(jlblProjName = new JLabel("Any DB Helper"));
		jlblProjName.setFont(new Font("Algerian", 10, 50));
		jpOperationsMenu.add(jbtnModRecordsFrame = new JButton("View/Insert/Update/Delete DB Data"));
		jpOperationsMenu.add(searchDBBtn = new JButton("Search DB"));
		jpOperationsMenu.add(XMLDBBtn = new JButton("DB to XML"));
		jpOperationsMenu.add(new JSeparator(SwingConstants.HORIZONTAL));
		jpOperationsMenu.add(jbtnCreateAttendanceTableForPresets = new JButton("Create Attendance Table for new Presets"));
		jpOperationsMenu.add(new JLabel("<html>JDBC Connection Stirng: jdbc:"+DBVendor+"://"+DBServerIPPort+"/"+dbName+"<br />Note: Username=AMSServerDBBot, contact System developer/admin for more details<html>"));
		this.add(jpOperationsMenu);
		
		jbtnModRecordsFrame.setToolTipText("View/Modify DB");
		searchDBBtn.setToolTipText("Search DB");
		XMLDBBtn.setToolTipText("Generate XML for any DB Table");
		jbtnCreateAttendanceTableForPresets.setToolTipText("New presets's attendance table will be created that tracks attendance for that preset for the academic year");
	
		jbtnModRecordsFrame.addActionListener(this);
		searchDBBtn.addActionListener(this);
		XMLDBBtn.addActionListener(this);
		jbtnCreateAttendanceTableForPresets.addActionListener(this);
	}
	public void actionPerformed(ActionEvent ae)
	{	
		try
		{
			if(ae.getSource().equals(searchDBBtn))
				new SearchDB(DBVendor, DBServerIPPort, dbName);
			else if(ae.getSource().equals(jbtnModRecordsFrame))
				new ModRecordsFrame(DBVendor, DBServerIPPort, dbName);
			else if(ae.getSource().equals(jbtnCreateAttendanceTableForPresets))
				new CreatePresetsTablesFrame(DBVendor, DBServerIPPort, dbName);
		}catch(Exception e)
		{
			JOptionPane.showMessageDialog(MainFrame.this, "An error occured: "+e.getMessage()+" .. retry!");
		}
	}
}
