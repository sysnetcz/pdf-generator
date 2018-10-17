package cz.sysnet.pdf.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

/**
 * PdfFactory je singleton, ktery slouzi ke generovani PDF dokumentu z dodanych dat pomoci reportovaci sablony Jaser Reports
 * Obsahuje radu produkcnich a pomocnych metod, se kterymi tvori uceleny balik.
 * 
 * @author 		Radim Jäger
 * @version		1.0.0
 * 
 */
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
	public static final String PATH_TEMP = (System.getProperty("java.io.tmpdir") + FILE_SEPARATOR + "pdf_factory" + FILE_SEPARATOR).replace(FILE_SEPARATOR+FILE_SEPARATOR, FILE_SEPARATOR);
	public static final String PATH_WORK = (System.getProperty("user.dir") + FILE_SEPARATOR + "pdf_factory" + FILE_SEPARATOR).replace(FILE_SEPARATOR+FILE_SEPARATOR, FILE_SEPARATOR);
	public static final String PATH_DATA = (System.getProperty("user.home") + FILE_SEPARATOR + "pdf_factory" + FILE_SEPARATOR).replace(FILE_SEPARATOR+FILE_SEPARATOR, FILE_SEPARATOR);
	public static final String PATH_TEMPLATE = PATH_DATA + "templates"  + FILE_SEPARATOR;
	public static final String PATH_PDF = PATH_DATA + "pdf"  + FILE_SEPARATOR;
	public static final String PATH_CONFIG = PATH_DATA;
	public static final String FILE_CONFIG_TEMPLATES = PATH_CONFIG + "templates.json";
	
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
		
		this.initDirectories();
		
		this.templateMap = new HashMap<String, String>();
		
		Path cp = Paths.get(FILE_CONFIG_TEMPLATES);
		if (Files.exists(cp)) this.loadTemplateMap();
		LOG.info("Init done");
	}
	
	private void initDirectories() {
		this.createDirectoryTree(PATH_CONFIG);
		this.createDirectoryTree(PATH_DATA);
		this.createDirectoryTree(PATH_PDF);
		this.createDirectoryTree(PATH_TEMP);
		this.createDirectoryTree(PATH_TEMPLATE);
		this.createDirectoryTree(PATH_WORK);
	}
	
	private void createDirectoryTree(String pathString) {
		try {
			Path path = Paths.get(pathString);
			if (!Files.exists(path)) {
				Files.createDirectories(path);
				LOG.info("Directory created: " + path.toString());
			}
		} catch (Exception e) {
			LOG.error("PdfFactory.createDirectoryTree: " + e.getMessage());
			e.printStackTrace();
		}
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
	
	
	/**
	 * Pomocná metoda. Slouží k převodu souboru do vstupního streamu
	 * 
	 * @param filePath	Úplná cesta k souboru 
	 * @return			Vrací InputStream, v případě chyby null 
	 */
	public static InputStream fileToInputStream(String filePath) {
		File file = new File(filePath);
		return fileToInputStream(file);
	}
	
	/**
	 * Pomocná metoda. Slouží k převodu souboru do vstupního streamu
	 * 
	 * @param file		Vstupní soubor 
	 * @return			Vrací InputStream, v případě chyby null 
	 */
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
	
	/**
	 * Pomocná metoda. Slouží k převodu souboru JSON do vstupního streamu bytového pole. 
	 *  
	 * @param jsonFilePath	Úplná cesta k souboru JSON
	 * @return				Vrací ByteArrayInputStream, v případě chyby null
	 */
	public static ByteArrayInputStream jsonFileToByteArrayInputStream(String jsonFilePath) {
		File jsonFile = new File(jsonFilePath);
		return jsonFileToByteArrayInputStream(jsonFile);
	}
	
	/**
	 * Pomocná metoda. Slouží k převodu souboru JSON do vstupního streamu bytového pole. 
	 *  
	 * @param jsonFile		Soubor JSON
	 * @return				Vrací ByteArrayInputStream, v případě chyby null
	 */
	public static ByteArrayInputStream jsonFileToByteArrayInputStream(File jsonFile) {
		try {
			if (jsonFile == null) throw new PdfGeneratorException("JSON file == null");
			if (!jsonFile.exists()) throw new PdfGeneratorException("JSON file does nots exist: " + jsonFile.getCanonicalPath());
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
	
	/**
	 * Pomocná metoda. Slouží k převodu dat JSON do vstupního streamu bytového pole. 
	 *  
	 * @param string		Data JSON
	 * @return				Vrací ByteArrayInputStream, v případě chyby null
	 */
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
	
	/**
	 * Do mapy šablon přidá šablonu. Šablona se překopíruje ze zdrojové cesty do standardního umístení šablon
	 *  
	 * @param key			klíč šablony
	 * @param sourceFile	Úplná cesta ke zdrojovému souboru šablony
	 * @param removeSource	Odstraní zdrojový soubor
	 * @return				Vrací cestu, kam byla šablona umístěna
	 */
	public String addTemplate(String key, String sourceFile, boolean removeSource) {
		try {
			File file = new File(sourceFile);
			String targetFile = PATH_TEMPLATE + key + FILE_SEPARATOR + file.getName();
			Path sourceFilePath = Paths.get(sourceFile);
			if (!Files.exists(sourceFilePath)) throw new PdfGeneratorException("Zdrojový soubor neexistuje: " + sourceFilePath.toString());
			Path targetFilePath = Paths.get(targetFile);
			if (!Files.exists(targetFilePath)) {
				Path td = Files.createDirectories(targetFilePath);
				LOG.info("Directory created: " + td.toString());
			}
			Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
			this.templateMap.put(key, targetFile);
			if (removeSource) Files.deleteIfExists(sourceFilePath);
			LOG.info("Template added: " + key +": " + targetFilePath.toString());
			return targetFilePath.toString();
			
		} catch (Exception e) {
			LOG.error("PdfFactory.addTemplate: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Přidá jeden soubor k šabloně s klíčem (například obrázek). Soubor se překopíruje ze zdrojové cesty do standardního umístení šablon 
	 * 
	 * @param key			klíč šablony
	 * @param sourceFile	Úplná cesta ke zdrojovému souboru
	 * @param removeSource	Odstraní zdrojový soubor
	 * @return				Vrací cestu, kam byl soubor umístěn
	 */
	public String addTemplateResource(String key, String sourceFile, boolean removeSource) {
		try {
			File file = new File(sourceFile);
			String targetFile = PATH_TEMPLATE + key + FILE_SEPARATOR + file.getName();
			Path sourceFilePath = Paths.get(sourceFile);
			if (!Files.exists(sourceFilePath)) throw new PdfGeneratorException("Zdrojový soubor neexistuje: " + sourceFilePath.toString());
			Path targetFilePath = Paths.get(targetFile);
			if (!Files.exists(targetFilePath)) {
				Path td = Files.createDirectories(targetFilePath);
				LOG.info("Directory created: " + td.toString());
			}
			Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
			if (removeSource) Files.deleteIfExists(sourceFilePath);
			LOG.info("Template resource added: " + key +": " + targetFilePath.toString());
			return targetFilePath.toString();
			
		} catch (Exception e) {
			LOG.error("PdfFactory.addTemplateResource: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Přidá seznam souborů k šabloně s klíčem (například obrázky). Soubory se překopírují ze zdrojové cesty do standardního umístění šablon 
	 * 
	 * @param key			klíč šablony
	 * @param sourceFileList	Seznam úplných cest ke zdrojovým souborům
	 * @param removeSource	Odstraní zdrojové soubory
	 * @return				Vrací JSON seznam cest, kam byly soubory umístěny
	 */
	public String addTemplateResource(String key, List<String> sourceFileList, boolean removeSource) {
		List<String> out = new ArrayList<String>(); 
		for (String sourceFile:sourceFileList) {
			out.add(this.addTemplateResource(key, sourceFile, removeSource));
		}
		return gson.toJson(out);
	}
	
	/**
	 * Vytvoří PDF dokument implicitního jména na základě šablony dané klíčem a vstupního datového streamu
	 * 
	 * @param key			klíč šablony
	 * @param data			Vstupní data jako String
	 * @return				Vrací cestu k vygenerovanému PDF nebo null v případě chyby
	 */
	public String createPdfFromJsonByKey(String key, String data) {
		return createPdfFromJsonByKey(key, data, null);		
	}
	
	/**
	 * Vytvoří PDF dokument na základě šablony dané klíčem a vstupního datového streamu
	 * 
	 * @param key			klíč šablony
	 * @param data			Vstupní data jako String
	 * @param pdfFileName	Úplná cesta pro umístění PDF
	 * @return				Vrací cestu k vygenerovanému PDF nebo null v případě chyby
	 */
	public String createPdfFromJsonByKey(String key, String data, String pdfFileName) {
		return createPdfFromJsonByKey(key, stringToByteArrayInputStream(data), pdfFileName);		
	}
	
	/**
	 * Vytvoří PDF dokument implicitního jména na základě šablony dané klíčem a vstupního datového streamu
	 * 
	 * @param key			klíč šablony
	 * @param jsonData		Soubor se vstupními daty
	 * @return				Vrací cestu k vygenerovanému PDF nebo null v případě chyby
	 */
	public String createPdfFromJsonByKey(String key, File jsonData) {
		return createPdfFromJsonByKey(key, jsonData, null);		
	}
	
	
	/**
	 * Vytvoří PDF dokument na základě šablony dané klíčem a vstupního datového streamu
	 * 
	 * @param key			klíč šablony
	 * @param jsonData		Soubor se vstupními daty
	 * @param pdfFileName	Úplná cesta pro umístění PDF
	 * @return				Vrací cestu k vygenerovanému PDF nebo null v případě chyby
	 */
	public String createPdfFromJsonByKey(String key, File jsonData, String pdfFileName) {
		return createPdfFromJsonByKey(key, jsonFileToByteArrayInputStream(jsonData), pdfFileName);		
	}
	
	/**
	 * Vytvoří PDF dokument implicitního jména na základě šablony dané klíčem a vstupního datového streamu
	 * 
	 * @param key			klíč šablony
	 * @param dataStream	Stream vstupních dat
	 * @return				Vrací cestu k vygenerovanému PDF nebo null v případě chyby
	 */
	public String createPdfFromJsonByKey(String key, ByteArrayInputStream dataStream) {
		return createPdfFromJsonByKey(key, dataStream, null);
	}
	
	/**
	 * Vytvoří PDF dokument na základě šablony dané klíčem a vstupního datového streamu
	 * 
	 * @param key			klíč šablony
	 * @param dataStream	Stream vstupních dat
	 * @param pdfFileName	Úplná cesta pro umístění PDF
	 * @return				Vrací cestu k vygenerovanému PDF nebo null v případě chyby
	 */
	public String createPdfFromJsonByKey(String key, ByteArrayInputStream dataStream, String pdfFileName) {
		try {
			if (this.templateMap == null) throw new PdfGeneratorException("Template map does not esist");
			if (this.templateMap.isEmpty()) throw new PdfGeneratorException("Template map is empty");
			if (!this.templateMap.containsKey(key)) throw new PdfGeneratorException("Template map does not contain template '"+key+"'");
			String templateFilePath = this.templateMap.get(key);
			if (pdfFileName == null) pdfFileName = generatePdfFileName(key.toLowerCase());
			if (pdfFileName.isEmpty()) pdfFileName = generatePdfFileName(key.toLowerCase());
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
	
	/**
	 * Vytvoří PDF dokument na základě šablony dané streamem, vstupního datového streamu, mapy parametrů a názvu výstupního souboru
	 * 
	 * @param templateStream	Vstupní stream šablony Jasper Reports (jrxml) 
	 * @param dataStream		ByteArrayInputStream vstuní data
	 * @param parameters		parametry nastavení šablony
	 * @param pdfFileName		Název výstupního souboru PDF (jen název, ne cesta) 
	 * @return					Vrací cestu k uloženému výstupnímu souboru nebo null při chybě. 
	 */
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
		    if (pdfFileName == null) pdfFileName = generatePdfFileName();
		    if (pdfFileName.isEmpty()) pdfFileName = generatePdfFileName();
		    String pdfPath = PATH_PDF + pdfFileName;
			Path targetFilePath = Paths.get(PATH_PDF);
			if (!Files.exists(targetFilePath)) {
				Path td = Files.createDirectories(targetFilePath);
				LOG.info("Directory created: " + td.toString());
			}
			targetFilePath = Paths.get(pdfPath);
			Files.deleteIfExists(targetFilePath);
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
	
	public boolean storeTemplateMap() {
		try {
			if(this.templateMap == null) this.templateMap = new HashMap<String, String>(); 
			
			File file = new File(FILE_CONFIG_TEMPLATES);
			if(file.exists()) {
				LOG.debug("File exists: " + file.getAbsolutePath());
			} else {
				LOG.debug("File does not exist: " + file.getAbsolutePath());
				file.getParentFile().mkdirs();
				boolean n = file.createNewFile();
				n = file.createNewFile();
				if (n) LOG.debug("File created: " + file.getAbsolutePath());
			}
			FileWriter fw = new FileWriter(file);
			gson.toJson(this.templateMap, fw);
			fw.flush();
			fw.close();
			LOG.info("Template Map stored");
			return true;
			
		} catch (Exception e) {
			LOG.error("PdfFactory.storeTemplateMap: " + e.getMessage());
			e.printStackTrace();
			return false;
		} 
	}
	
	public boolean removeTemplateMap() {
		try {
			File file = new File(FILE_CONFIG_TEMPLATES);
			if(file.exists()) {
				FileWriter fstream = new FileWriter(file,false);
	            BufferedWriter out = new BufferedWriter(fstream);
	            out.write("");
	            out.close();
	            fstream.close();
				LOG.info("TemplateMap file was cleared");
				return true;
			}
			return false;
			
		} catch (Exception e) {
			LOG.error("PdfFactory.removeTemplateMap: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	public void cleanTemplateMap() {
		this.templateMap = new HashMap<String, String>();
		LOG.info("TemplateMap cleared");
	}
	
	@SuppressWarnings("unchecked")
	public boolean loadTemplateMap() {
		try {
			if (this.templateMap == null) this.templateMap = new HashMap<String, String>();
			Path path = Paths.get(FILE_CONFIG_TEMPLATES);
			if (!Files.exists(path)) throw new PdfGeneratorException("Konfigurace šablon neexistuje: " + path.toString());
			BufferedReader br = Files.newBufferedReader(path);
			if (br == null) throw new PdfGeneratorException("BufferedReader == null");
			Map<String, String> out = null;
	        out = gson.fromJson(br, this.templateMap.getClass());
	        this.templateMap = out;
			LOG.info("Template map loaded");
			return true;
			
		} catch (Exception e) {
			LOG.error("PdfFactory.loadTemplateMap: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	public static String generatePdfFileName() {
		return generatePdfFileName(null);		
	}
	
	public static String generatePdfFileName(String key) {
		try {
			if (key == null) key = "document";
			if (key.isEmpty()) key = "document";
			
			DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			//DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			Date today = Calendar.getInstance().getTime();    
			String datePart = df.format(today);
			
			String out = key + "_" + datePart + ".pdf";
			return out;
			
		} catch (Exception e) {
			LOG.error("PdfFactory.generatePdfFileName: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
//	private static boolean isFileLocked(String filePath) {
//		File file = new File(filePath);
//		return isFileLocked(file);		
//	}
//	
//	private static boolean isFileLocked(File file) {
//		try {
//			if (file.exists()) FileUtils.touch(file);
//			FileUtils.deleteQuietly(file);
//			
//			return false;
//			
//		} catch (Exception e) {
//			LOG.error("File is locked: " + file.getAbsolutePath());
//			return true;
//		}
//	}
	
}
