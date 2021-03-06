package com.floreantpos.inventory.report;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.view.JRViewer;

import org.jdesktop.swingx.JXDatePicker;

import com.floreantpos.bo.ui.BackOfficeWindow;
import com.floreantpos.bo.ui.explorer.ListTableModel;
import com.floreantpos.report.ReportUtil;
import com.floreantpos.swing.TransparentPanel;
import com.floreantpos.ui.dialog.POSMessageDialog;
import com.floreantpos.ui.util.UiUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

/**
 * @author SOMYA
 * 
 */
public abstract class BaseReportView extends TransparentPanel {
	private JButton btnGo;
	private JXDatePicker fromDatePicker;
	private JXDatePicker toDatePicker;
	private JTextField reportName = new JTextField(30);
	private JPanel reportPanel;
	protected JPanel contentPane;
	private JLabel label1;
	private JLabel label2;

	public BaseReportView() {
		setLayout(new BorderLayout());
		add(contentPane);
		btnGo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				viewReport();
			}
		});
	}

	protected abstract ListTableModel getData(Date toDate, Date fromDate, HashMap properties);

	protected abstract String getReportName();

	protected void setAdditionalProperties(HashMap properties) {

	}

	public void showDateFields(boolean hide) {
		this.label1.setVisible(hide);
		this.label2.setVisible(hide);
		this.fromDatePicker.setVisible(hide);
		this.toDatePicker.setVisible(hide);
	}

	private void viewReport() {
		Date fromDate = fromDatePicker.getDate();
		Date toDate = toDatePicker.getDate();
		String name = reportName.getText();

		if (fromDate.after(toDate)) {
			POSMessageDialog.showError(BackOfficeWindow.getInstance(), com.floreantpos.POSConstants.FROM_DATE_CANNOT_BE_GREATER_THAN_TO_DATE_);
			return;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.clear();

		Calendar calendar2 = Calendar.getInstance();
		calendar2.setTime(fromDate);

		calendar.set(Calendar.YEAR, calendar2.get(Calendar.YEAR));
		calendar.set(Calendar.MONTH, calendar2.get(Calendar.MONTH));
		calendar.set(Calendar.DATE, calendar2.get(Calendar.DATE));
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		fromDate = calendar.getTime();

		calendar.clear();
		calendar2.setTime(toDate);
		calendar.set(Calendar.YEAR, calendar2.get(Calendar.YEAR));
		calendar.set(Calendar.MONTH, calendar2.get(Calendar.MONTH));
		calendar.set(Calendar.DATE, calendar2.get(Calendar.DATE));
		calendar.set(Calendar.HOUR, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		toDate = calendar.getTime();

		try {
			JasperReport report = ReportUtil.getReport(getReportName());
			HashMap properties = new HashMap();
			properties.put("fromDate", fromDate);
			properties.put("toDate", toDate);
			properties.put("reportDate", new Date());
			properties.put("reportName", name);

			setAdditionalProperties(properties);

			JasperPrint print = JasperFillManager.fillReport(report, properties, new JRTableModelDataSource(getData(toDate, fromDate, properties)));

			JRViewer viewer = new JRViewer(print);
			reportPanel.removeAll();
			reportPanel.add(viewer);
			reportPanel.revalidate();
		} catch (JRException e) {
			e.printStackTrace();
		}
	}

	{
		$$$setupUI$$$();
	}

	private void $$$setupUI$$$() {
		contentPane = new JPanel();
		contentPane.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setName("panel1");
		panel1.setLayout(new GridLayoutManager(8, 7, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
		label1 = new JLabel();
		label1.setText(com.floreantpos.POSConstants.FROM + ":");
		panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
				null, 0, false));
		fromDatePicker = UiUtil.getCurrentMonthStart();
		panel1.add(fromDatePicker, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(147, 24), null, 0, false));

		label2 = new JLabel();
		label2.setText(com.floreantpos.POSConstants.TO + ":");
		panel1.add(label2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
				null, 0, false));
		toDatePicker = UiUtil.getCurrentMonthEnd();
		panel1.add(toDatePicker, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(147, 24), null, 0, false));

		final JLabel label3 = new JLabel();
		label3.setText("Report Name: ");
		panel1.add(label3, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null,
				null, 0, false));
		panel1.add(reportName, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
				GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(147, 24), null, 0, false));

		btnGo = new JButton();
		btnGo.setText(com.floreantpos.POSConstants.GO);
		panel1.add(btnGo, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(147, 23), null, 0, false));

		final JSeparator separator1 = new JSeparator();
		panel1.add(separator1, new GridConstraints(7, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW,
				null, null, null, 0, false));

		reportPanel = new JPanel();
		reportPanel.setLayout(new BorderLayout(0, 0));
		contentPane.add(reportPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK
				| GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
	}

	protected void addExtraParams() {

	}

	public JComponent $$$getRootComponent$$$() {
		return contentPane;
	}
}
