package cz.sysnet.pdf.generator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PdfFactory {
	private static volatile PdfFactory instance;
	private static Object mutex = new Object();
	private static final Logger LOG = LogManager.getLogger(PdfFactory.class);
	
	public static final String GSON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	
	public static Gson gson = new GsonBuilder()
			.setDateFormat(GSON_DATE_FORMAT)
			.setPrettyPrinting()
			.create();
	public static JsonParser jsonParser  = new JsonParser();
	
	private Map<String, String> templateMap;

	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	public static final String TEMP_PATH = (System.getProperty("java.io.tmpdir") + FILE_SEPARATOR + "pdf_factory" + FILE_SEPARATOR).replace(FILE_SEPARATOR+FILE_SEPARATOR, FILE_SEPARATOR);
	public static final String WORK_PATH = (System.getProperty("user.dir") + FILE_SEPARATOR + "pdf_factory" + FILE_SEPARATOR).replace(FILE_SEPARATOR+FILE_SEPARATOR, FILE_SEPARATOR);
	public static final String DATA_PATH = (System.getProperty("user.home") + FILE_SEPARATOR + "pdf_factory" + FILE_SEPARATOR).replace(FILE_SEPARATOR+FILE_SEPARATOR, FILE_SEPARATOR);
	public static final String TEMPLATE_PATH = DATA_PATH + "templates"  + FILE_SEPARATOR;
	public static final String PDF_PATH = DATA_PATH + "pdf"  + FILE_SEPARATOR;
	
	
	private PdfFactory() {
		super();
		init();
		LOG.info("Factory created");
	}
	
	private void init() {
		gson = new GsonBuilder()
				.setDateFormat(GSON_DATE_FORMAT)
				.setPrettyPrinting()
				.create();
		jsonParser = new JsonParser();
		this.templateMap = new HashMap<String, String>();
		LOG.info("Init done");
	}
	
	public static PdfFactory getInstance() {
		PdfFactory result = instance;
		if (result == null) {
			synchronized (mutex) {
				result = instance;
				if (result == null) instance = new PdfFactory();
				result = instance;
			}
		}
		return result;
	}
	
	public static InputStream fileToInputStream(String filePath) {
		File file = new File(filePath);
		return fileToInputStream(file);
	}
	
	public static InputStream fileToInputStream(File file) {
		try {
			InputStream is = new FileInputStream(file);
			return is;
			
		} catch (Exception e) {
			LOG.error("PdfFactory.fileToInputStream: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	public static ByteArrayInputStream jsonFileToByteArrayInputStream(String jsonFilePath) {
		File jsonFile = new File(jsonFilePath);
		return jsonFileToByteArrayInputStream(jsonFile);
	}
	
	public static ByteArrayInputStream jsonFileToByteArrayInputStream(File jsonFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(jsonFile));
			JsonObject jo = jsonParser.parse(br).getAsJsonObject();
			String js = gson.toJson(jo);
			return stringToByteArrayInputStream(js);
			
		} catch (Exception e) {
			LOG.error("PdfFactory.jsonFileToByteArrayInputStream: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	public static ByteArrayInputStream stringToByteArrayInputStream(String string) {
		try {
			byte[] utf8JsonString = string.getBytes("UTF8");
			ByteArrayInputStream dataStream = new ByteArrayInputStream(utf8JsonString);
			return dataStream;
			
		} catch (Exception e) {
			LOG.error("PdfFactory.stringToByteArrayInputStream: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	public String addTemplate(String key, String sourceFile) {
		try {
			File file = new File(sourceFile);
			String targetFile = TEMPLATE_PATH + "key" + FILE_SEPARATOR + file.getName();
			Path sourceFilePath = Paths.get(sourceFile);
			Path targetFilePath = Paths.get(targetFile);
			Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
			this.templateMap.put(key, targetFile);
			return targetFilePath.toString();
			
		} catch (Exception e) {
			LOG.error("PdfFactory.addTemplate: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	public String addTemplateResource(String key, String sourceFile) {
		try {
			File file = new File(sourceFile);
			String targetFile = TEMPLATE_PATH + "key" + FILE_SEPARATOR + file.getName();
			Path sourceFilePath = Paths.get(sourceFile);
			Path targetFilePath = Paths.get(targetFile);
			Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
			return targetFilePath.toString();
			
		} catch (Exception e) {
			LOG.error("PdfFactory.addTemplateResource: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	public String addTemplateResource(String key, List<String> sourceFileList) {
		List<String> out = new ArrayList<String>(); 
		for (String sourceFile:sourceFileList) {
			out.add(this.addTemplateResource(key, sourceFile));
		}
		return gson.toJson(out);
	}
	
	
	public String createPdfFromJsonByKey(String key, ByteArrayInputStream dataStream, String pdfFileName) {
		try {
			if (this.templateMap == null) throw new PdfGeneratorException("Template map does not esist");
			if (this.templateMap.isEmpty()) throw new PdfGeneratorException("Template map is empty");
			if (!this.templateMap.containsKey(key)) throw new PdfGeneratorException("Template map does not contain template '"+key+"'");
			String templateFilePath = this.templateMap.get(key); 
			return createPdfFromJson(templateFilePath, dataStream, null, pdfFileName);
			
		} catch (Exception e) {
			LOG.error("PdfFactory.createPdfFromJsonByKey: " + e.getMessage());
			e.printStackTrace();
			return null;
		}		
	}
	
	
	public String createPdfFromJson(String templateFilePath, String dataString, String pdfFileName) {
		return createPdfFromJson(templateFilePath, dataString, null, pdfFileName); 		
	}
	
	public String createPdfFromJson(String templateFilePath, String dataString, Map<String, Object> parameters, String pdfFileName) {
		return createPdfFromJson(fileToInputStream(templateFilePath), stringToByteArrayInputStream(dataString), parameters, pdfFileName);
	}
	
	public String createPdfFromJson(File templateFile, String dataString, String pdfFileName) {
		return createPdfFromJson(templateFile, dataString, null, pdfFileName);
	}
	
	public String createPdfFromJson(File templateFile, String dataString, Map<String, Object> parameters, String pdfFileName) {
		return createPdfFromJson(fileToInputStream(templateFile), stringToByteArrayInputStream(dataString), parameters, pdfFileName);
	}
	
	public String createPdfFromJson(InputStream templateStream, String dataString, String pdfFileName) {
		return createPdfFromJson(templateStream, dataString, null, pdfFileName);
	}
	
	public String createPdfFromJson(InputStream templateStream, String dataString, Map<String, Object> parameters, String pdfFileName) {
		return createPdfFromJson(templateStream, stringToByteArrayInputStream(dataString), parameters, pdfFileName);
	}
	
	public String createPdfFromJson(String templateFilePath, ByteArrayInputStream dataStream, String pdfFileName) {
		return createPdfFromJson(templateFilePath, dataStream, null, pdfFileName);
	}
	
	public String createPdfFromJson(String templateFilePath, ByteArrayInputStream dataStream, Map<String, Object> parameters, String pdfFileName) {
		return createPdfFromJson(fileToInputStream(templateFilePath), dataStream, parameters, pdfFileName);
	}
	
	public String createPdfFromJson(File templateFile, ByteArrayInputStream dataStream, String pdfFileName) {
		return createPdfFromJson(templateFile, dataStream, null, pdfFileName);
	}
	
	public String createPdfFromJson(File templateFile, ByteArrayInputStream dataStream, Map<String, Object> parameters, String pdfFileName) {
		return createPdfFromJson(fileToInputStream(templateFile), dataStream, parameters, pdfFileName);
	}
	
	public String createPdfFromJson(InputStream templateStream, ByteArrayInputStream dataStream, String pdfFileName) {
		return createPdfFromJson(templateStream, dataStream, null, pdfFileName);
	}
	
	public String createPdfFromJson(InputStream templateStream, ByteArrayInputStream dataStream, Map<String, Object> parameters, String pdfFileName) {
		try {
			JasperDesign design = JRXmlLoader.load(templateStream);
			JasperReport report  = JasperCompileManager.compileReport(design);
		    JsonDataSource ds = new JsonDataSource(dataStream);
		    if(parameters == null) {
			    parameters = new HashMap<String, Object>();
			    parameters.put("title", "SYSNET PDF Generator");
		    }
		    JasperPrint jasperPrint = JasperFillManager.fillReport(report, parameters, ds);
		    String pdfPath = PDF_PATH + pdfFileName;
		    JasperExportManager.exportReportToPdfFile(jasperPrint, pdfPath);
		    LOG.info("PdfFactory.createPdfFromJson - PDF created: " +  pdfPath);
		    return pdfPath;

		} catch (Exception e) {
			LOG.error("PdfFactory.createPdfFromJson BASE: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	

	public Map<String, String> getTemplateMap() {
		return templateMap;
	}

	public void setTemplateMap(Map<String, String> templateMap) {
		this.templateMap = templateMap;
	}
	
	
	
	
	
}
