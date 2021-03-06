package com.floreantpos.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.floreantpos.model.Restaurant;
import com.floreantpos.model.dao.RestaurantDAO;

public class ReportUtil {
	private static Log logger = LogFactory.getLog(ReportUtil.class);

	private final static String USER_REPORT_DIR = "/com/floreantpos/report/template/";
	private final static String DEFAULT_REPORT_DIR = "/com/floreantpos/report/template/";

	public static void populateRestaurantProperties(Map map) {
		RestaurantDAO dao = new RestaurantDAO();
		Restaurant restaurant = dao.get(Integer.valueOf(1));
		map.put("restaurantName", restaurant.getName());
		map.put("addressLine1", restaurant.getAddressLine1());
		map.put("addressLine2", restaurant.getAddressLine2());
		map.put("addressLine3", restaurant.getAddressLine3());
		map.put("phone", restaurant.getTelephone());
	}

	public static JasperReport getReport(String reportName) {
		return getReport(reportName,USER_REPORT_DIR);
	}
	
	public static JasperReport getReport(String reportName, String path, Class claz){
		InputStream resource = null;

		try {
			resource = claz.getResourceAsStream(path + reportName + ".jasper");
			if (resource == null) {
				return compileReport(reportName);
			} else {
				return (JasperReport) JRLoader.loadObject(resource);
			}
		} catch (Exception e) {
			logger.error("Could not load report " + reportName + " from user directory, loading default report");
			return getDefaultReport(reportName,path);

		} finally {
			IOUtils.closeQuietly(resource);
		}
	}
	
	public static JasperReport getReport(String reportName, String path) {
		return getReport(reportName,  path, ReceiptPrintService.class);

	}

	private static JasperReport compileReport(String reportName) throws Exception {
		return compileReport(reportName,USER_REPORT_DIR);
	}
	
	private static JasperReport compileReport(String reportName, String path, Class claz) throws Exception {
		InputStream in = null;
		InputStream in2 = null;
		FileOutputStream out = null;
		File jasperFile = null;

		try {
			File jrxmlFile = new File(ReceiptPrintService.class.getResource(path + reportName + ".jrxml").getFile());
			File dir = jrxmlFile.getParentFile();
			jasperFile = new File(dir, reportName + ".jasper");

			in = claz.getResourceAsStream(path + reportName + ".jrxml");
			out = new FileOutputStream(jasperFile);
			JasperCompileManager.compileReportToStream(in, out);

			in2 = claz.getResourceAsStream(path + reportName + ".jasper");
			return (JasperReport) JRLoader.loadObject(in2);

		} catch (Exception e) {
			IOUtils.closeQuietly(out);
			if (jasperFile != null) {
				jasperFile.delete();
			}

			throw e;
		}

		finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(in2);
			IOUtils.closeQuietly(out);
		}
	}
	
	private static JasperReport compileReport(String reportName, String path) throws Exception {
		return compileReport( reportName,  path,ReceiptPrintService.class);
	}
	
	private static JasperReport getDefaultReport(String reportName, String path) {
		InputStream resource = null;

		try {

			resource = ReceiptPrintService.class.getResourceAsStream(path + reportName + ".jasper");
			return (JasperReport) JRLoader.loadObject(resource);

		} catch (Exception e) {
			logger.error(e);
			return null;

		} finally {
			IOUtils.closeQuietly(resource);
		}
	}
	
	public static String toCamelCase(final String init) {
	    if (init==null)
	        return null;

	    final StringBuilder ret = new StringBuilder(init.length());

	    for (final String word : init.split(" ")) {
	        if (!word.isEmpty()) {
	            ret.append(Character.toUpperCase(word.charAt(0)));
	            ret.append(word.substring(1).toLowerCase());
	        }
	        if (!(ret.length()==init.length()))
	            ret.append(" ");
	    }

	    return ret.toString();
	}

	public static void main(String[] args) {
		try {
			compileReport("ticket-receipt");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
