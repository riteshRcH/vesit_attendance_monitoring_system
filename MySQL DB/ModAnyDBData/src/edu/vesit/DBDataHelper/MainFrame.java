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
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class MainFrame extends JFrame implements ActionListener
{
	JButton searchDBBtn, jbtnModRecordsFrame, XMLDBBtn, FragmentationBtn;
	JLabel jlblProjName;
	JPanel jpOperationsMenu = new JPanel();
	JTextField jtxfDBNamePrefix = new JTextField("vesitams"), jtxfBatchStartYear = new JTextField("2011"), jtxfBatchEndYear = new JTextField("2015");
	
	public MainFrame()
	{
		setLayout(new FlowLayout(FlowLayout.CENTER));
		this.setSize(600, 400);
		this.setResizable(false);
		this.setTitle("VESIT AMS DB Admin Console");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		
		GridLayout gridLayout = new GridLayout(0, 1);
		gridLayout.setVgap(22);
		gridLayout.setHgap(22);
		jpOperationsMenu.setLayout(gridLayout);
		
		this.add(jlblProjName = new JLabel("Any DB Data Helper"));
		jlblProjName.setFont(new Font("Algerian", 10, 50));
		jpOperationsMenu.add(jtxfDBNamePrefix);
		jpOperationsMenu.add(jtxfBatchStartYear);
		jpOperationsMenu.add(jtxfBatchEndYear);
		jpOperationsMenu.add(jbtnModRecordsFrame = new JButton("View/Insert/Update/Delete DB Data"));
		jpOperationsMenu.add(searchDBBtn = new JButton("Search DB"));
		jpOperationsMenu.add(XMLDBBtn = new JButton("DB to XML"));
		this.add(jpOperationsMenu);
	
		jbtnModRecordsFrame.addActionListener(this);
		searchDBBtn.addActionListener(this);
	}
	public static void main(String[] args)
	{
		new MainFrame();
	}
	public void actionPerformed(ActionEvent ae)
	{	
		try
		{
			String dbName = jtxfDBNamePrefix.getText().toString().trim()+Integer.parseInt(jtxfBatchStartYear.getText().toString().trim())+"_"+Integer.parseInt(jtxfBatchEndYear.getText().toString().trim());
			this.dispose();
			if(ae.getSource().equals(searchDBBtn))
				new SearchDB();
			else if(ae.getSource().equals(jbtnModRecordsFrame))
				new ModRecordsFrame(dbName);
		}catch(Exception e)
		{
			JOptionPane.showMessageDialog(MainFrame.this, "An error occured: "+e.getMessage()+" .. retry!");
		}
	}
}
