package cz.sysnet.pdf.generator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.repo.Resource;

public class MyGenerator {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("NAZDÁREK");
		
		try {
			
		JsonParser parser = new JsonParser();
		Gson gson = new Gson();			
		
		String jrxmlFileName = "src/test/resources/templates/picture_rl.jrxml";
		String jasperFileName = "src/test/resources/templates/picture_rl.jasper";
		String pdfFileName = "src/test/resources/templates/picture_rl.pdf";
		String jsonFile = "src/test/resources/resources/picture_rl_data.json";
		
		InputStream is = new FileInputStream(jrxmlFileName);
		JasperDesign design = JRXmlLoader.load(is);
		JasperReport report  = JasperCompileManager.compileReport(design);
		
		BufferedReader br = new BufferedReader(new FileReader(jsonFile));
		JsonObject jo = parser.parse(br).getAsJsonObject();
		String rawJsonData = jo.toString();
		
		String js = gson.toJson(jo);
		byte[] utf8JsonString = js.getBytes("UTF8");
		
		//ByteArrayInputStream jsonDataStream = new ByteArrayInputStream(rawJsonData.getBytes());
		ByteArrayInputStream jsonDataStream = new ByteArrayInputStream(utf8JsonString);
		
	    JsonDataSource ds = new JsonDataSource(jsonDataStream);
	    
	    Map<String, Object> parameters = new HashMap<String, Object>();
	    parameters.put("title", "Jasper PDF Example");
	    JasperPrint jasperPrint = JasperFillManager.fillReport(report, parameters, ds);
	    
	    JasperExportManager.exportReportToPdfFile(jasperPrint,pdfFileName);
	    
		
		
		} catch (Exception e) {
			System.out.println("MyGenerator.main() ERROR: " + e.getMessage());
			e.printStackTrace();
		}
		 

	}

}
