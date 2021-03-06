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
import java.util.Map.Entry;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
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
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

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
	protected static Map<String, String> ENV = System.getenv(); 
	
	
	public static final String GSON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	
	public static Gson gson = new GsonBuilder()
			.setDateFormat(GSON_DATE_FORMAT)
			.setPrettyPrinting()
			.create();
	public static JsonParser jsonParser  = new JsonParser();
	
	private Map<String, String> templateMap;

	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	
	public static String BASE_DIR = getEnvString("PDF_DATA_DIR", System.getProperty("user.home"));
	
	public static String PATH_CONFIG = (BASE_DIR + FILE_SEPARATOR + "config" + FILE_SEPARATOR).replace(FILE_SEPARATOR+FILE_SEPARATOR, FILE_SEPARATOR);
	public static String FILE_CONFIG_TEMPLATES = PATH_CONFIG + "templates.json";
	public static String PATH_DATA = (BASE_DIR + FILE_SEPARATOR + "data" + FILE_SEPARATOR).replace(FILE_SEPARATOR+FILE_SEPARATOR, FILE_SEPARATOR);
	public static String PATH_PDF = PATH_DATA + "pdf"  + FILE_SEPARATOR;
	public static String PATH_TEMP = (BASE_DIR + FILE_SEPARATOR + "temp" + FILE_SEPARATOR).replace(FILE_SEPARATOR+FILE_SEPARATOR, FILE_SEPARATOR);
	public static String PATH_TEMPLATE = PATH_DATA + "templates"  + FILE_SEPARATOR;
	public static String PATH_UPLOAD = PATH_DATA + "uploads"  + FILE_SEPARATOR;
	public static String PATH_WORK = (BASE_DIR + FILE_SEPARATOR + "work" + FILE_SEPARATOR).replace(FILE_SEPARATOR+FILE_SEPARATOR, FILE_SEPARATOR);
	public static Map<PathKey, String> PATH_MAP = null;
	
	public static enum PathKey {
		CONFIG, DATA, PDF, TEMP, TEMPLATE, UPLOAD, WORK
	}
	
	private PdfFactory(Map<PathKey, String> pathMap) {
		super();
		init(pathMap);
		LOG.info("Factory created");
	}
		
	private void init(Map<PathKey, String> pathMap) {
		gson = new GsonBuilder()
				.setDateFormat(GSON_DATE_FORMAT)
				.setPrettyPrinting()
				.create();
		jsonParser = new JsonParser();
		
		this.initDirectories(pathMap);
		
		this.templateMap = new HashMap<String, String>();
		
		Path cp = Paths.get(FILE_CONFIG_TEMPLATES);
		if (Files.exists(cp)) this.loadTemplateMap();
		LOG.info("Init done");
	}
	
	private boolean initDirectories(Map<PathKey, String> pathMap) {
		try {
			if (pathMap == null) pathMap = new HashMap<PathKey, String>();
			if (!pathMap.containsKey(PathKey.CONFIG)) pathMap.put(PathKey.CONFIG, PATH_CONFIG);
			if (!pathMap.containsKey(PathKey.DATA)) pathMap.put(PathKey.DATA, PATH_DATA);
			if (!pathMap.containsKey(PathKey.PDF)) pathMap.put(PathKey.PDF, PATH_PDF);
			if (!pathMap.containsKey(PathKey.TEMP)) pathMap.put(PathKey.TEMP, PATH_TEMP);
			if (!pathMap.containsKey(PathKey.TEMPLATE)) pathMap.put(PathKey.TEMPLATE, PATH_TEMPLATE);			
			if (!pathMap.containsKey(PathKey.UPLOAD)) pathMap.put(PathKey.UPLOAD, PATH_UPLOAD);
			if (!pathMap.containsKey(PathKey.WORK)) pathMap.put(PathKey.WORK, PATH_WORK);			
			for (Entry<PathKey, String> entry:pathMap.entrySet()) {
				this.createDirectoryTree(entry.getValue());
			}
			PATH_MAP = pathMap;
			return true;
			
		} catch (Exception e) {
			LOG.error("PdfFactory.initDirectories: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
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
	
	public static Map<String, String> getEnv() {
		return ENV;
	}
	
	public static String getEnvString(String key, String defaultValue) {
		String out = defaultValue;
		if (ENV.containsKey(key)) out = ENV.get(key);
		LOG.debug("(getEnvString) " + key + ": " + out);
		return out;		
	}
	
	public static boolean getEnvBoolean(String key, boolean defaultValue) {
		boolean out = defaultValue;
		String w = getEnvString(key, Boolean.toString(out));
		out = Boolean.parseBoolean(w);
		return out;		
	}
	
	public static int getEnvInteger(String key, int defaultValue) {
		int out = defaultValue;
		String w = getEnvString(key, Integer.toString(out));
		out = Integer.parseInt(w);
		return out;	
	}
	
	public static String convertMapToString(Map<String, ?> map) {
	    return Joiner.on(",").withKeyValueSeparator("=").join(map);
	}
	
	public static Map<String, String> convertStringToMap(String stringToSplit) {
		try {
			return Splitter.on(",").withKeyValueSeparator("=").split(stringToSplit);
			
		} catch (Exception e) {
			return null;
		}		
	}
	
	public static Map<String, String> mergeMaps(Map<String, String> map1, Map<String, String> map2) {
		if ((map1 == null) && (map2 == null)) return null;
		Map<String, String> out = new HashMap<String, String>();
		if ((map1 == null) && (map2 != null)) {
			out.putAll(map2);
			return out;
		}
		if ((map1 != null) && (map2 == null)) { 
			out.putAll(map1);
			return out;
		}
		out.putAll(map1);
		out.putAll(map2);
		return out;		
	}
	
	public static PdfFactory getInstance() {
		return _getInstance("");	// vzdycky pouzijeme BASE_DIR
	}
	
	private static PdfFactory _getInstance(String basePath) {
		Map<PathKey, String> pathMap = new HashMap<PathKey, String>();
		if (basePath != null) {
			if (basePath.isEmpty()) basePath = BASE_DIR;
			basePath = (basePath + FILE_SEPARATOR + "pdf_factory" + FILE_SEPARATOR).replace(FILE_SEPARATOR+FILE_SEPARATOR, FILE_SEPARATOR);
			pathMap.put(PathKey.CONFIG, basePath + "conf" + FILE_SEPARATOR);
			pathMap.put(PathKey.DATA, basePath + "data" + FILE_SEPARATOR);
			pathMap.put(PathKey.PDF, basePath + "data"+ FILE_SEPARATOR + "pdf" + FILE_SEPARATOR);
			pathMap.put(PathKey.TEMP, basePath + "temp" + FILE_SEPARATOR);
			pathMap.put(PathKey.TEMPLATE, basePath + "data"+ FILE_SEPARATOR + "templates" + FILE_SEPARATOR);
			pathMap.put(PathKey.UPLOAD, basePath + "data"+ FILE_SEPARATOR + "uploads" + FILE_SEPARATOR);
			pathMap.put(PathKey.WORK, basePath + "work"+ FILE_SEPARATOR);			
		}
		return _getInstance(pathMap);
	}
	
	private static PdfFactory _getInstance(Map<PathKey, String> pathMap) {
		PdfFactory result = instance;
		if (result == null) {
			synchronized (mutex) {
				result = instance;
				if (result == null) instance = new PdfFactory(pathMap);
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
			String targetFile = PATH_MAP.get(PathKey.TEMPLATE) + key + FILE_SEPARATOR + file.getName();
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
			String targetFile = PATH_MAP.get(PathKey.TEMPLATE) + key + FILE_SEPARATOR + file.getName();
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
		    String pdfPath = PATH_MAP.get(PathKey.PDF) + pdfFileName;
			Path targetFilePath = Paths.get(PATH_MAP.get(PathKey.PDF));
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
	
	public Map<PathKey, String> getPathMap() {
		return PATH_MAP;
	}
	
	public static String getProjectVersion() {
		String out = "";
		Model model = getProjectModel();
		if (model != null) out = model.getVersion();
		return out;
	}
	
	public static Model getProjectModel() {
		Model out = null;
		try {
			MavenXpp3Reader reader = new MavenXpp3Reader();
	        out = reader.read(new FileReader("pom.xml"));
		
		} catch (Exception e) {
			LOG.error("getProjectModel - " + e.getMessage());
			e.printStackTrace();
			out = null;
		} 
		return out;
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
