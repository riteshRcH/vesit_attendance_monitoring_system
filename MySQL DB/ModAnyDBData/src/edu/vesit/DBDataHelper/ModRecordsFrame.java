package edu.vesit.DBDataHelper;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

@SuppressWarnings("serial")
public class ModRecordsFrame extends JFrame implements ActionListener
{
	JComboBox<String> jcomboBoxSelectRelation;
	Vector<String> allRelations = new Vector<String>();
	JButton jbtnViewRelation;
	JTable jtableResultSet;
	Connection conn;
	Statement stmt;
	ResultSet rs;
	
	public ModRecordsFrame(String dbName)
	{
		this.setLayout(new FlowLayout(FlowLayout.CENTER));
		this.setSize(new Dimension(320, 200));
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Select Table");
		this.setLocationRelativeTo(null);
		
		this.add(jcomboBoxSelectRelation = new JComboBox<String>(new String[]{}));
		this.add(jbtnViewRelation = new JButton("Modify Data for selected Table"));
		
		jbtnViewRelation.addActionListener(this);
		
		try
		{
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+dbName,"AMSServerDBBot", "VESIT9521AMSBot");
			System.out.println("DB Connection Success!");
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
			new MainFrame();
		}
		for(String tableName:allRelations)
			jcomboBoxSelectRelation.addItem(tableName);
	}
	public void actionPerformed(ActionEvent ae)
	{
		if(ae.getSource().equals(jbtnViewRelation))
			new OutputResultSet(jcomboBoxSelectRelation.getSelectedItem().toString());
	}
	public static void main(String args[])
	{
		new ModRecordsFrame("vesitams2011_2015");
	}
	class OutputResultSet extends JFrame implements ActionListener
	{
		JButton jbtnAddRecord, jbtnDelRecord, jbtnUpdateRecord;
		String[] columnNames, columnTypes;
		String selectedTableName;
		JPanel jpOperations = new JPanel(new GridLayout(0, 1));
		
		public OutputResultSet(String selectedTableName)
		{
			this.selectedTableName = selectedTableName;
			
			this.setLayout(new BorderLayout());
			this.setVisible(true);
			this.setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
			this.setLocationRelativeTo(null);
			this.setTitle(selectedTableName);
			this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			
			jpOperations.add(jbtnAddRecord = new JButton("Add new Record"));
			jpOperations.add(jbtnUpdateRecord = new JButton("Update Selected Record"));
			jpOperations.add(jbtnDelRecord = new JButton("Delete Selected Record"));
			this.add(jpOperations, BorderLayout.SOUTH);
			
			jbtnAddRecord.addActionListener(this);
			jbtnUpdateRecord.addActionListener(this);
			jbtnDelRecord.addActionListener(this);
			
			try
			{
				rs = stmt.executeQuery("select * from "+selectedTableName);
				ResultSetMetaData rsmd = rs.getMetaData();
				columnNames = new String[rsmd.getColumnCount()];
				columnTypes = new String[rsmd.getColumnCount()];
				for(int i=1;i<=rsmd.getColumnCount();i++)
				{
					columnNames[i-1] = rsmd.getColumnName(i);
					columnTypes[i-1] = rsmd.getColumnClassName(i);
					//System.out.println(columnTypes[i-1] = columnTypes[i-1].substring(columnTypes[i-1].lastIndexOf('.')+1));
					columnTypes[i-1] = columnTypes[i-1].substring(columnTypes[i-1].lastIndexOf('.')+1);
				}
				
				rs.last();
				String[][] data = new String[rs.getRow()][columnNames.length];
				rs.beforeFirst();
				
				int index = 0;
				while(rs.next())
				{
					for(int i=0;i<columnNames.length;i++)
					{
						Object o = rs.getObject(columnNames[i]);
						data[index][i] = o==null?"null":o.toString().trim();
					}
					index++;
				}
				
				this.add(new JScrollPane(jtableResultSet = new JTable(data, columnNames)), BorderLayout.CENTER);
				jtableResultSet.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			}catch (SQLException e)
			{
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Exception occured while retreiving data from DB, retry", "Exception occured", JOptionPane.ERROR_MESSAGE);
			}
		}
		public void actionPerformed(ActionEvent ae)
		{
			if(ae.getSource().equals(jbtnAddRecord))
			{
				InsertRecordDialog insertDialog = new InsertRecordDialog(ModRecordsFrame.this, "Insert new Record into: "+selectedTableName);
				insertDialog.setSize(400, 400);
				insertDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				insertDialog.setVisible(true);
				insertDialog.setLocationRelativeTo(null);
			}else if(ae.getSource().equals(jbtnUpdateRecord))
			{
				int selectedRowID = jtableResultSet.getSelectedRow(), index = 0;
				if(selectedRowID==-1)
					JOptionPane.showMessageDialog(ModRecordsFrame.this, "No Row has been selected!", "Select a row 1st", JOptionPane.ERROR_MESSAGE);
				else
				{
					Object selectedRowValues[] = new Object[jtableResultSet.getColumnCount()];
					for(int i=0;i<jtableResultSet.getColumnCount();i++)
						selectedRowValues[index++] = jtableResultSet.getValueAt(selectedRowID, i);
					
					UpdateRecordDialog updateDialog = new UpdateRecordDialog(ModRecordsFrame.this, "Update Record: "+selectedTableName, selectedRowValues);
					updateDialog.setSize(500, 400);
					updateDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					updateDialog.setVisible(true);
					updateDialog.setLocationRelativeTo(null);
				}
			}else if(ae.getSource().equals(jbtnDelRecord))
			{
				int selectedRowID = jtableResultSet.getSelectedRow(), index = 0;
				if(selectedRowID==-1)
					JOptionPane.showMessageDialog(ModRecordsFrame.this, "No Row has been selected!", "Select a row 1st", JOptionPane.ERROR_MESSAGE);
				else
				{
					Object selectedRowValues[] = new Object[jtableResultSet.getColumnCount()];
					for(int i=0;i<jtableResultSet.getColumnCount();i++)
						selectedRowValues[index++] = jtableResultSet.getValueAt(selectedRowID, i);
					
					DeleteRecordDialog delDialog = new DeleteRecordDialog(ModRecordsFrame.this, "Update Record: "+selectedTableName, selectedRowValues);
					delDialog.setSize(320, 320);
					delDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					delDialog.setVisible(true);
					delDialog.setLocationRelativeTo(null);				
				}
			}
		}
		class InsertRecordDialog extends JDialog implements ActionListener
		{
			private JButton jbtnInsertCmd, jbtnCancelCmd;
			JPanel jpOperations = new JPanel(new GridLayout(0, 2));
			JTextField[] jtxfNewRowColumnValues;

			public InsertRecordDialog(JFrame parent, String title)
			{
				super(parent, title, true);
				this.setLayout(new FlowLayout());
				
				jtxfNewRowColumnValues = new JTextField[columnNames.length];
				for(int i=0;i<jtxfNewRowColumnValues.length;i++)
				{
					if(!columnNames[i].equals("Age"))
					{
						jpOperations.add(new JLabel("<html>"+columnNames[i]+"("+columnTypes[i]+")<br />"+(columnTypes[i].equals("Timestamp")?"=> dd\\mm\\yyyy hh:mm:ss AM\\PM":"")+"</html>"));
						jpOperations.add(jtxfNewRowColumnValues[i] = new JTextField(11));
					}
				}
				this.add(jpOperations);
				this.add(jbtnInsertCmd = new JButton("Insert into DB"));
				this.add(jbtnCancelCmd = new JButton("Cancel"));
				
				jbtnInsertCmd.addActionListener(this);
				jbtnCancelCmd.addActionListener(this);
			}
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				if(ae.getSource().equals(jbtnInsertCmd))
				{
					String query = "insert into "+selectedTableName+"(";
					//EXCLUDE Age as its auto-computed value(pre-computed)
					for(int i=0;i<columnNames.length-1;i++)
						if(!columnNames[i].equals("Age"))
							query += columnNames[i]+", ";
					query += columnNames[columnNames.length-1]+") values(";
					try
					{
						for(int i=0;i<columnNames.length-1;i++)
						{
							if(!columnNames[i].equals("Age"))
							{
								if(columnTypes[i].equals("String"))
									query += "'"+jtxfNewRowColumnValues[i].getText().toString().trim()+"', ";
								else if(columnTypes[i].equals("Integer"))
									query += Integer.parseInt(jtxfNewRowColumnValues[i].getText().toString().trim())+", ";
								else if(columnTypes[i].equals("Long"))
									query += Long.parseLong(jtxfNewRowColumnValues[i].getText().toString().trim())+", ";
								else if(columnTypes[i].equals("Timestamp"))
								{
									SimpleDateFormat inputFormatter = new SimpleDateFormat("dd\\MM\\yyyy HH:mm:ss a");
									Date d = inputFormatter.parse(jtxfNewRowColumnValues[i].getText().toString().trim());
									SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");
									query += "'"+outputFormatter.format(d)+"', ";
									//query += new SimpleDateFormat().parse(jtxfNewRowColumnValues[i].getText().toString().trim())+", ";
								}else if(columnTypes[i].equals("Short"))
									query += Short.parseShort(jtxfNewRowColumnValues[i].getText().toString().trim())+", ";
							}
						}
						if(!columnNames[columnNames.length-1].equals("Age"))
						{
							if(columnTypes[columnTypes.length-1].equals("String"))
								query += "'"+jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim()+"')";
							else if(columnTypes[columnNames.length-1].equals("Integer"))
								query += Integer.parseInt(jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim())+")";
							else if(columnTypes[columnNames.length-1].equals("Long"))
								query += Long.parseLong(jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim())+")";
							else if(columnTypes[columnNames.length-1].equals("Timestamp"))
							{
								SimpleDateFormat inputFormatter = new SimpleDateFormat("dd\\MM\\yyyy HH:mm:ss a");
								Date d = inputFormatter.parse(jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim());
								SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");
								query += "'"+outputFormatter.format(d)+"') ";
								//query += new SimpleDateFormat().parse(jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim())+")";
							}else if(columnTypes[columnNames.length-1].equals("Short"))
								query += Short.parseShort(jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim())+")";
						}
						System.out.println("Insert Query: "+query);
						
						int numRowsAfftected;
						if((numRowsAfftected = stmt.executeUpdate(query))>0)
						{
							JOptionPane.showMessageDialog(ModRecordsFrame.this, numRowsAfftected+" number of rows added successfully");
							InsertRecordDialog.this.dispose();
							OutputResultSet.this.dispose();
							new OutputResultSet(selectedTableName);
						}else
							JOptionPane.showMessageDialog(ModRecordsFrame.this, "no rows added, retry!");
					}catch(Exception e)
					{
						e.printStackTrace();
						JOptionPane.showMessageDialog(ModRecordsFrame.this, "Exception Occured: "+e.toString(), "Exception occured!", JOptionPane.ERROR_MESSAGE);
					}
					
/***********NOTE: NOT USED PREPARED STATEMENT AS IT FILLS THE VARCHAR(MAX) DATA TYPE BY SPACES  TO FILL UP MAX VALUE***********************/
					
					/*String query = "insert into "+selectedTableName+" values(";
					for(int i=0;i<columnNames.length-1;i++)
						query += "?, ";
					query += "?)";
					PreparedStatement ps;
					try
					{
						ps = conn.prepareStatement(query);
						for(int i=0;i<columnNames.length;i++)
						{
							switch(columnTypes[i])
							{
								case "String":
									ps.setString(i+1, jtxfNewRowColumnValues[i].getText().toString().trim());
								break;
								
								case "Long":
									ps.setLong(i+1, Long.parseLong(jtxfNewRowColumnValues[i].getText().toString().trim()));
								break;
									
								case "Integer":
									ps.setInt(i+1, Integer.parseInt(jtxfNewRowColumnValues[i].getText().toString().trim()));
								break;
							}
						}
						int numRowsAfftected;
						if((numRowsAfftected = ps.executeUpdate())>0)
						{
							JOptionPane.showMessageDialog(SelectRecordsFrame.this, numRowsAfftected+" number of rows added successfully");
							InsertRecordDialog.this.dispose();
							OutputResultSet.this.dispose();
							new OutputResultSet(selectedTableName);
						}else
							JOptionPane.showMessageDialog(SelectRecordsFrame.this, "no rows added, retry!");
					}catch(SQLException e)
					{
						e.printStackTrace();
						JOptionPane.showMessageDialog(SelectRecordsFrame.this, "Exception Occured: "+e.toString(), "Exception occured!", JOptionPane.ERROR_MESSAGE);
					}*/
				}else if(ae.getSource().equals(jbtnCancelCmd))
				{
					InsertRecordDialog.this.setVisible(false);
					InsertRecordDialog.this.dispose();
				}
			}
		}
			class UpdateRecordDialog extends JDialog implements ActionListener
			{
				private JButton jbtnUpdateCmd, jbtnCancelCmd;
				JPanel jpOperations = new JPanel(new GridLayout(0, 3));
				JTextField[] jtxfNewRowColumnValues;
				Object[] oldValues;
				JCheckBox[] jchkboxWhrConditionConstantAttr;

				public UpdateRecordDialog(JFrame parent, String title, Object[] oldValues)
				{
					super(parent, title, true);
					this.setLayout(new FlowLayout());
					this.oldValues = oldValues;
					
					jtxfNewRowColumnValues = new JTextField[columnNames.length];
					jchkboxWhrConditionConstantAttr = new JCheckBox[columnNames.length];
					for(int i=0;i<jtxfNewRowColumnValues.length;i++)
					{
						if(!columnNames[i].equals("Age"))
						{
							jpOperations.add(jchkboxWhrConditionConstantAttr[i] = new JCheckBox());
							jpOperations.add(new JLabel(columnNames[i]+"("+columnTypes[i]+"): "));
							jpOperations.add(jtxfNewRowColumnValues[i] = new JTextField(11));
							if(columnTypes[i].equals("Timestamp"))
							{
								SimpleDateFormat outputFormatter = new SimpleDateFormat("dd\\MM\\yyyy HH:mm:ss a");
								SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
								try
								{
									jtxfNewRowColumnValues[i].setText(outputFormatter.format(inputFormatter.parse(oldValues[i].toString().substring(0, oldValues[i].toString().lastIndexOf('.')))));
								}catch(Exception e)
								{
									this.dispose();
									JOptionPane.showMessageDialog(ModRecordsFrame.this, "Exception occured"+e.toString()+", retry!");
								}
							}else
								jtxfNewRowColumnValues[i].setText(oldValues[i].toString().trim());
						}
					}
					this.add(jpOperations);
					this.add(new JLabel("Where condition Checkboxes"));
					this.add(jbtnUpdateCmd = new JButton("Update DB"));
					this.add(jbtnCancelCmd = new JButton("Cancel"));
					
					jbtnUpdateCmd.addActionListener(this);
					jbtnCancelCmd.addActionListener(this);
				}
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					if(ae.getSource().equals(jbtnUpdateCmd))
					{
						String updateWhereCondition = "where ";
						for(int i=0;i<jchkboxWhrConditionConstantAttr.length;i++)
							if(!columnNames[i].equals("Age") && jchkboxWhrConditionConstantAttr[i].isSelected())
								updateWhereCondition += columnNames[i]+"="+oldValues[i]+" and ";
						
						if(updateWhereCondition.endsWith(" and "))
							updateWhereCondition = updateWhereCondition.substring(0, updateWhereCondition.length()-5);
						
						String updateQuery = "update "+selectedTableName+" set ";
						try
						{
							for(int i=0;i<columnNames.length-1;i++)
							{
								if(!columnNames[i].equals("Age"))	
								{
									if(!jtxfNewRowColumnValues[i].getText().toString().equals(oldValues[i]))//only if values have been updated
									{
										if(columnTypes[i].equals("String"))
											updateQuery += columnNames[i]+"='"+jtxfNewRowColumnValues[i].getText().toString().trim()+"', ";
										else if(columnTypes[i].equals("Integer"))
											updateQuery += columnNames[i]+"="+Integer.parseInt(jtxfNewRowColumnValues[i].getText().toString().trim())+", ";
										else if(columnTypes[i].equals("Long"))
											updateQuery += columnNames[i]+"="+Long.parseLong(jtxfNewRowColumnValues[i].getText().toString().trim())+", ";
										else if(columnTypes[i].equals("Timestamp"))
										{
											SimpleDateFormat inputFormatter = new SimpleDateFormat("dd\\MM\\yyyy HH:mm:ss a");
											Date d = inputFormatter.parse(jtxfNewRowColumnValues[i].getText().toString().trim());
											SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");
											updateQuery += columnNames[i]+"='"+outputFormatter.format(d)+"', ";
											//query += new SimpleDateFormat().parse(jtxfNewRowColumnValues[i].getText().toString().trim())+", ";
										}else if(columnTypes[i].equals("Short"))
											updateQuery += columnNames[i]+"="+Short.parseShort(jtxfNewRowColumnValues[i].getText().toString().trim())+", ";
									}
								}
							}
							if(!columnNames[columnNames.length-1].equals("Age"))
							{
								if(!jtxfNewRowColumnValues[columnNames.length-1].getText().toString().equals(oldValues[columnNames.length-1]))//only if values have been updated
								{
									if(columnTypes[columnTypes.length-1].equals("String"))
										updateQuery += columnNames[columnNames.length-1]+"='"+jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim()+"' ";
									else if(columnTypes[columnNames.length-1].equals("Integer"))
										updateQuery += columnNames[columnNames.length-1]+"="+Integer.parseInt(jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim())+" ";
									else if(columnTypes[columnNames.length-1].equals("Long"))
										updateQuery += columnNames[columnNames.length-1]+"="+Long.parseLong(jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim())+" ";
									else if(columnTypes[columnNames.length-1].equals("Timestamp"))
									{
										SimpleDateFormat inputFormatter = new SimpleDateFormat("dd\\MM\\yyyy HH:mm:ss a");
										Date d = inputFormatter.parse(jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim());
										SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");
										updateQuery += columnNames[columnNames.length-1]+"='"+outputFormatter.format(d)+"' ";
										//query += new SimpleDateFormat().parse(jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim())+")";
									}else if(columnTypes[columnNames.length-1].equals("Short"))
										updateQuery += columnNames[columnNames.length-1]+"="+Short.parseShort(jtxfNewRowColumnValues[columnNames.length-1].getText().toString().trim())+" ";
								}
							}
							if(updateQuery.endsWith(", "))
								updateQuery = updateQuery.substring(0, updateQuery.length()-2);
							System.out.println("Update Query: "+(updateQuery+=updateWhereCondition));
							
							int numRowsAfftected;
							if((numRowsAfftected = stmt.executeUpdate(updateQuery))>0)
							{
								JOptionPane.showMessageDialog(ModRecordsFrame.this, numRowsAfftected+" number of rows updated successfully");
								UpdateRecordDialog.this.dispose();
								OutputResultSet.this.dispose();
								new OutputResultSet(selectedTableName);
							}else
								JOptionPane.showMessageDialog(ModRecordsFrame.this, "row not updated, retry!");
							
						}catch(Exception e)
						{
							e.printStackTrace();
							JOptionPane.showMessageDialog(ModRecordsFrame.this, "Exception occured"+e.toString()+", retry!");
						}
					}else if(ae.getSource().equals(jbtnCancelCmd))
					{
						UpdateRecordDialog.this.setVisible(false);
						UpdateRecordDialog.this.dispose();
					}
				}
			}
			class DeleteRecordDialog extends JDialog implements ActionListener
			{
				private JButton jbtnDeleteCmd, jbtnCancelCmd;
				JPanel jpOperations = new JPanel(new GridLayout(0, 2));
				Object[] oldValues;

				public DeleteRecordDialog(JFrame parent, String title, Object[] oldValues)
				{
					super(parent, title, true);
					this.setLayout(new FlowLayout());
					this.oldValues = oldValues; 
					
					for(int i=0;i<oldValues.length;i++)
					{
						jpOperations.add(new JLabel(columnNames[i]+"("+columnTypes[i]+"): "));
						jpOperations.add(new JLabel(oldValues[i].toString()));
					}
					this.add(jpOperations);
					this.add(jbtnDeleteCmd = new JButton("Delete selected Record"));
					this.add(jbtnCancelCmd = new JButton("Cancel"));
					
					jbtnDeleteCmd.addActionListener(this);
					jbtnCancelCmd.addActionListener(this);
				}
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					if(ae.getSource().equals(jbtnDeleteCmd))
					{
						/*String delQuery = "delete from "+selectedTableName+" where ";
						for(int i=0;i<columnNames.length-1;i++)
							delQuery += columnNames[i]+"=? and ";
						delQuery += columnNames[columnNames.length-1]+"=?";
						
						PreparedStatement ps;
						try
						{
							ps = conn.prepareStatement(delQuery);
							for(int i=0;i<columnNames.length;i++)
							{
								switch(columnTypes[i])
								{
									case "String":
										ps.setString(i+1, oldValues[i].toString().trim());
									break;
									
									case "Long":
										ps.setLong(i+1, Long.parseLong(oldValues[i].toString().trim()));
									break;
										
									case "Integer":
										ps.setInt(i+1, Integer.parseInt(oldValues[i].toString().trim()));
									break;
								}
							}
							int numRowsAfftected;
							if((numRowsAfftected = ps.executeUpdate())>0)
							{
								JOptionPane.showMessageDialog(SelectRecordsFrame.this, numRowsAfftected+" number of rows deleted successfully");
								DeleteRecordDialog.this.dispose();
								OutputResultSet.this.dispose();
								new OutputResultSet(selectedTableName);
							}else
								JOptionPane.showMessageDialog(SelectRecordsFrame.this, "no rows deleted, retry!");
						}catch(SQLException e)
						{
							e.printStackTrace();
							JOptionPane.showMessageDialog(SelectRecordsFrame.this, "Exception Occured: "+e.toString(), "Exception occured!", JOptionPane.ERROR_MESSAGE);
						}*/
						String delQuery = "delete from "+selectedTableName+" where ";
						int i=0;
						for(;i<jtableResultSet.getColumnCount()-1;i++)
						{
							if(columnTypes[i].equals("String"))
								delQuery += columnNames[i]+"='"+oldValues[i].toString()+"' and ";
							else if(columnTypes[i].equals("Long"))
								delQuery += columnNames[i]+"="+Long.parseLong(oldValues[i].toString())+" and ";
							else if(columnTypes[i].equals("Integer"))
									delQuery += columnNames[i]+"="+oldValues[i].toString()+" and "; 
						}
						if(columnTypes[i].equals("String"))
							delQuery += columnNames[i]+"='"+oldValues[i].toString()+"'";
						else if(columnTypes[i].equals("Long"))
							delQuery += columnNames[i]+"="+Long.parseLong(oldValues[i].toString());
						else if(columnTypes[i].equals("Integer"))
								delQuery += columnNames[i]+"="+oldValues[i].toString();
						
						int numRowsAfftected;
						try
						{
							if((numRowsAfftected = stmt.executeUpdate(delQuery))>0)
							{
								JOptionPane.showMessageDialog(ModRecordsFrame.this, numRowsAfftected+" row deleted successfully");
								DeleteRecordDialog.this.dispose();
								OutputResultSet.this.dispose();
								new OutputResultSet(selectedTableName);
							}else
								JOptionPane.showMessageDialog(ModRecordsFrame.this, "no row deleted, retry!");
						}catch(Exception e)
						{
							e.printStackTrace();
							JOptionPane.showMessageDialog(ModRecordsFrame.this, "Exception occured"+e.toString()+", retry!");
						}
					}else if(ae.getSource().equals(jbtnCancelCmd))
					{
						DeleteRecordDialog.this.setVisible(false);
						DeleteRecordDialog.this.dispose();
					}
				}
			}
	}
}
