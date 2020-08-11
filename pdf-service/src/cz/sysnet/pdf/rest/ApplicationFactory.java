package cz.sysnet.pdf.rest;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
//import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

import cz.sysnet.pdf.generator.PdfFactory;
import cz.sysnet.pdf.generator.PdfFactory.PathKey;
import cz.sysnet.pdf.rest.common.LogType;
import cz.sysnet.pdf.rest.common.PdfRestException;
import cz.sysnet.pdf.rest.common.ServiceLog;
import cz.sysnet.pdf.rest.model.Template;

public final class ApplicationFactory {
	// slouzi k provadeni vsech produkcnich akci v aplikaci
	
	public static String VERSION = "1.1.4";
	private static volatile ApplicationFactory instance = null;
	//public static final Logger LOG = Logger.getLogger(ApplicationFactory.class.getName());
	public static final Logger LOG = LogManager.getLogger(ApplicationFactory.class);
	public static Long LONG_ZERO = Long.valueOf(0);
	public static final String TEMPLATE_EXT = ".jrxml";
	
	private String workDirectory = null;
	private String configDirectory = null;
	private String configPath = null;
	private String dataDirectory = null;
	private String templateDirectory = null;
	private String pdfDirectory = null;
	private String uploadDirectory = null;
	private String temporaryDirectory = null;
	private Map<String, String> dirMap = null;	
	private Map<String, String> templateMap = null;
	
	private Map<PathKey, String> pathMap = null;
	
	private String version = "";
	private String title = "";
	
	private int memoryTreshold	= 1024 * 1024 * 3;  // 3MB
	private int maxFileSize		= 1024 * 1024 * 40; // 40MB
	private int maxRequestSize	= 1024 * 1024 * 50; // 50MB 
	
	private Configurations configs = null;
	private FileBasedConfigurationBuilder<XMLConfiguration> configBuilder = null;
	private XMLConfiguration config = null;
	
	private PdfFactory pdfFactory = null;
	
	private DateTime startTime;
	private TreeMap<String, ServiceLog> counter;
	
	private ApplicationFactory() {
		super();
		this.init();
	}

	private void init() {
		try {
			this.version = this.getClass().getPackage().getImplementationVersion();
			this.title = this.getClass().getPackage().getImplementationTitle();
			
			this.dirMap = new HashMap<String, String>();
			
			this.startTime = new DateTime();
			this.counter = new TreeMap<String, ServiceLog>();
			
			this.pdfFactory = PdfFactory.getInstance();
			this.pathMap = this.pdfFactory.getPathMap();
			
			// init configuration			
			this.configDirectory = pathMap.get(PathKey.CONFIG);
			this.dirMap.put("CONFIG", this.configDirectory);
			this.configPath = this.configDirectory + File.separatorChar + "service.xml";
			this.initConfigFile();
			this.configs = new Configurations();
			this.configBuilder = configs.xmlBuilder(this.configPath);
			this.config = this.configBuilder.getConfiguration();
			
			// init directories
			this.dataDirectory = this.initConfigString("service.dataDirectory", this.pathMap.get(PathKey.DATA));
			this.dirMap.put("DATA", this.dataDirectory);
			
			this.temporaryDirectory = this.initConfigString("service.temporaryDirectory", this.pathMap.get(PathKey.TEMP));
			this.dirMap.put("TEMP", this.temporaryDirectory);

			this.templateDirectory = this.initConfigString("service.templateDirectory", this.pathMap.get(PathKey.TEMPLATE));
			this.dirMap.put("TEMPLATE", this.templateDirectory);
			
			this.pdfDirectory = this.initConfigString("service.pdfDirectory", this.pathMap.get(PathKey.PDF));
			this.dirMap.put("PDF", this.pdfDirectory);
			
			this.uploadDirectory = this.initConfigString("service.uploadDirectory", this.pathMap.get(PathKey.UPLOAD));
			this.dirMap.put("UPLOAD", this.uploadDirectory);
			
			this.workDirectory = this.initConfigString("service.workDirectory", this.pathMap.get(PathKey.WORK));
			this.dirMap.put("WORK", this.workDirectory);
			
			// finish configuration
			this.maxFileSize = this.initConfigInteger("upload.maxFileSize", 1024 * 1024 * 40);
			this.maxRequestSize = this.initConfigInteger("upload.maxRequestSize", 1024 * 1024 * 50);
			this.memoryTreshold = this.initConfigInteger("upload.memoryTreshold", 1024 * 1024 * 3);
			
			this.configBuilder.save();
			this.configBuilder.setAutoSave(true);
			
			this.templateMap = this.initTemplateMap();
			
			LOG.info("ApplicationFactory initialized: " + this.title + ", " + this.version);
			
		} catch (Exception e) {
			LOG.error("cz.sysnet.pdf.rest.ApplicationFactory.init FAILED: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Projde adresar se sablonami a sestavi mapu sablon. Vybira pouze soubory *.jrxml. 
	 * Pokud je jich vice, pak pouze ten nejnovejsi.
	 *  
	 * @return	vraci mapu sablon pouzitelnou pro PDFFactory 
	 */
	public Map<String, String> initTemplateMap() {
		Map<String, String> out = new HashMap<String, String>();
		String templateDir  = this.templateDirectory;
		try {
			File file = new File(templateDir);
			String[] directories = file.list(new FilenameFilter() {
			  @Override
			  public boolean accept(File current, String name) {
			    return new File(current, name).isDirectory();
			  }
			});
			if (directories != null) {
				LOG.info("TEMPLATES: " + Arrays.toString(directories));
				if (directories.length > 0) {
					List<String> dirList = Arrays.asList(directories);
					for (String item:dirList) {
						File dir = new File(templateDir + File.separator + item);
						File[] files = dir.listFiles(new FilenameFilter() {
						    public boolean accept(File dir, String name) {
						        return name.toLowerCase().endsWith(TEMPLATE_EXT);
						    }
						});
						if (files != null) {
							if (files.length > 0) {
								if (files.length > 1) {
									 long lastMod = Long.MIN_VALUE;
									 File choice = null;
									 for (File f : files) {
										 if (f.lastModified() > lastMod) {
											 choice = f;
											 lastMod = f.lastModified();
										 }
									 }
									 out.put(item, choice.getPath());
									 LOG.info(item + ": " + choice.getPath());
									 
								} else {
									out.put(item, files[0].getPath());		
									LOG.info(item + ": " + files[0].getPath());
								}
							} else {
								LOG.warn("NO TEMPLATE FILES");
							}
						} else {
							LOG.warn("NO TEMPLATE FILES (null)");
						}
					}
				} else {
					LOG.warn("NO TEMPLATES");
				}
			} else {
				LOG.warn("NO TEMPLATES (null)");
			}
		} catch (Exception e) {
			LOG.error(e.getMessage());
			e.printStackTrace();
			out = new HashMap<String, String>();
		}
		this.templateMap = out;
		if (this.pdfFactory != null) this.pdfFactory.setTemplateMap(out);
		return out;
	}
	
	
	private String initConfigString(String parameter, String value) {
		if (this.config != null) {
			if (this.config.containsKey(parameter)) {
				String v = this.config.getString(parameter);
				if (v != null) if (!v.isEmpty()) return v;
			}
			this.config.addProperty(parameter, value);
			return value;
		}
		return "";
	}
	
	private int initConfigInteger(String parameter, Integer value) {
		if (this.config != null) {
			if (this.config.containsKey(parameter)) { 
				int v = this.config.getInt(parameter);
				return v;
			}
			this.config.addProperty(parameter, value.intValue());
			return value.intValue();
		}
		return -1;
	}
	
	private void initConfigFile() {
		Writer out = null;
		try {
			File configFile = new File(this.configPath);
			if (configFile.exists()) if (configFile.length() > 0) return;
			configFile.createNewFile(); // if file already exists will do nothing 
			String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + 
					"<configuration>\n" + 
					"  <version>"+VERSION+"</version>\n" + 
					"</configuration>";
			
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8"));
		    out.write(xml);
		} catch (Exception e) {
			LOG.error("cz.sysnet.pdf.rest.ApplicationFactory.initConfigFile FAILED: " + e.getMessage());
			e.printStackTrace();
			
		} finally {
			try {
				if (out != null) out.close();
			} catch (IOException e) {
				LOG.error("cz.sysnet.pdf.rest.ApplicationFactory.initConfigFile finally FAILED: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	

	public static ApplicationFactory getInstance() {
		if (instance == null) {
			synchronized (ApplicationFactory.class) {
				if (instance == null) {
					instance = new ApplicationFactory();
				}
			}
		}
		return instance;
	}
	
	// produkcni metody
	private Duration getDuration() {
		DateTime now = new DateTime();
		Duration dur = new Duration(this.startTime, now);
		return dur;
	}
	
	public String getDurationString() {
		Duration dur = this.getDuration();
		Period per = dur.toPeriod();
		
		int hours = per.getHours();
		int days = hours / 24;
		hours = hours % 24;
		
		String out = "Doba běhu: ";
		out += Integer.toString(days) + "d, ";
		out += Integer.toString(hours) + "h, ";
		out += per.getMinutes() + "m, ";
		out += per.getSeconds() + "s";
		return out;		
	}
	
	public void counterIncrement(String key, LogType type, int increment) {
		if (key == null) return;
		if (key.isEmpty()) return;
		ServiceLog value = this.getCounterValue(key);
		Long v = Long.valueOf(0);
		switch (type) {
		case ERROR:
			v = value.getErrors() + increment;
			value.setErrors(v);
			break;
		case RUN:
			v = value.getRuns() + increment;
			value.setRuns(v);
			break;
		case RUNTIME:
			v = value.getRuntime() + increment;
			value.setRuntime(v);
			break;
		case WARNING:
			v = value.getWarnings() + increment;
			value.setWarnings(v);
			break;

		default:
			v = null;
			break;
		}
		if (v != null) this.counter.put(key, value);
	}
	
	public void counterIncrement(String key, LogType type) {
		this.counterIncrement(key, type, 1);
	}
	
	public void counterErrorIncrement(String key) {
		this.counterIncrement(key, LogType.ERROR, 1);
	}
	
	public void counterRunIncrement(String key) {
		this.counterIncrement(key, LogType.RUN, 1);
	}
	
	public void counterWarningIncrement(String key) {
		this.counterIncrement(key, LogType.WARNING, 1);
	}
	
	public void counterRuntimeIncrement(String key, int increment) {
		this.counterIncrement(key, LogType.RUN, increment);
	}
	
	public ServiceLog getCounterValue(String key) {
		ServiceLog out = new ServiceLog();
		out.setErrors(LONG_ZERO);
		out.setRuns(LONG_ZERO);
		out.setRuntime(LONG_ZERO);
		out.setWarnings(LONG_ZERO);
		if (this.counter.containsKey(key)) out = this.counter.get(key);
		return out;
	}
	
	public String getHtmlUploadedTable() {
		try {
			String out = "";
			out += "<div class=\"table-responsive\">\n";
			out += "<table class=\"table table-striped\">\n";
			out += "<thead>\n";
			out += "<th>Uploadované soubory</th>\n";
			out += "</thead>\n";
			out += "<tbody>\n";
			
			File f = new File(this.uploadDirectory);
			
			ArrayList<String> names = new ArrayList<String>(Arrays.asList(f.list()));
			
			if (names.isEmpty())  {
				out += "<tr>";
				out += "<td>-adresář je prázdný-</td>";
				out += "</tr>\n";
			} else {
				for (String item:names) {
					out += "<tr>";
					String fileParam = "/upload/" + item;
					fileParam = URLEncoder.encode(fileParam, "UTF-8");
					out += "<td><a href=\"download?file="+fileParam+"\">"+item+"</a></td>";
					out += "</tr>\n";
				}			
			}
			out += "</tbody>\n";
			out += "</table>\n";
			out += "</div>\n";
			return out;
			
		} catch (Exception e) {
			LOG.error("cz.sysnet.pdf.rest.ApplicationFactory.getHtmlUploadedTable: " + e.getMessage());
			e.printStackTrace();
			return e.getMessage();
		}
	}
	
	public List<File> getPdfFileList() {
		return this.getPdfFileList(null);		
	}
	
	public List<File> getPdfFileList(String key) {
		List<File> out = new ArrayList<File>();
		try {
			File dir = new File(this.pdfDirectory);
			//LOG.info("DIR: " + dir.getPath());
			File[] files = dir.listFiles(new FilenameFilter() {
			    public boolean accept(File dir, String name) {
			    	//LOG.info("NAME: " + name);
			    	if ((key == null) || (key.isEmpty())) return true;
			    	//LOG.info("KEY: " + key);
			    	if (name.toLowerCase().startsWith(key.toLowerCase())) return true;
			    	//LOG.info("FILTER: false");
			    	return false;
			    }
			});
			out = Arrays.asList(files);

		} catch (Exception e) {
			LOG.error("cz.sysnet.pdf.rest.ApplicationFactory.getPdfFileList: " + e.getMessage());
			e.printStackTrace();
			out = new ArrayList<File>();
		}
		return out;		
	}
	
	public File getPdfFile(String fileName) {
		File out = null;
		try {
			LOG.info("PDF: " + fileName);
			File dir = new File(this.pdfDirectory);
			File[] files = dir.listFiles(new FilenameFilter() {
			    public boolean accept(File dir, String name) {
			    	return name.equalsIgnoreCase(fileName);
			    }
			});
			if (files != null) if (files.length > 0) out = files[0]; 

		} catch (Exception e) {
			LOG.error("cz.sysnet.pdf.rest.ApplicationFactory.getPdfFile: " + e.getMessage());
			e.printStackTrace();
			out = null;
		}
		return out;		
	}

	
	public String getHtmlDirTable() {
		String out = "";
		out += "<div class=\"table-responsive\">\n";
		out += "<table class=\"table table-striped\">\n";
		out += "<thead>\n";
		out += "<th>Adresář</th>\n";
		out += "<th>Cesta</th>\n";
		out += "</thead>\n";
		out += "<tbody>\n";
		for (Entry<String, String> entry:this.dirMap.entrySet()) {
			out += "<tr>";
			out += "<td>" + entry.getKey() + "</td>";
			out += "<td>" + entry.getValue() + "</td>";
			out += "</tr>\n";
		}
		out += "</tbody>\n";
		out += "</table>\n";
		out += "</div>\n";
		return out;
	}
	
	public String getHtmlTemplateTable() {
		String out = "";
		out += "<div class=\"table-responsive\">\n";
		out += "<table class=\"table table-striped\">\n";
		out += "<thead>\n";
		out += "<th>Klíč</th>\n";
		out += "<th>Cesta</th>\n";
		out += "</thead>\n";
		out += "<tbody>\n";
		for (Entry<String, String> entry:this.templateMap.entrySet()) {
			out += "<tr>";
			out += "<td>" + entry.getKey() + "</td>";
			out += "<td>" + entry.getValue() + "</td>";
			out += "</tr>\n";
		}
		out += "</tbody>\n";
		out += "</table>\n";
		out += "</div>\n";
		return out;
	}
	
	public String getHtmlCounterTable() {
		String out = "";
		if (!this.counter.isEmpty()) {
			out += "<div class=\"table-responsive\">\n";
			out += "<table class=\"table table-striped\">\n";
			out += "<thead>\n";
			out += "<th>Služba</th>\n";
			out += "<th class=\"text-center\">Požadavky</th>\n";
			out += "<th class=\"text-center\">Varování</th>\n";
			out += "<th class=\"text-center\">Chyby</th>\n";
			out += "<th class=\"text-center\">Čas běhu</th>\n";
			out += "</thead>\n";
			out += "<tbody>\n";
			Entry<String, ServiceLog> entry = this.counter.firstEntry();
			while (entry != null) {
				String key = entry.getKey();
				ServiceLog value = entry.getValue();
				out += "<tr>";
				out += "<td>" + key + "</td>";
				out += "<td class=\"text-center\">" + value.getRuns().toString() + "</td>";
				out += "<td class=\"text-center\">" + value.getWarnings().toString() + "</td>";
				out += "<td class=\"text-center\">" + value.getErrors().toString() + "</td>";
				out += "<td class=\"text-center\">" + value.getRuntime().toString() + "</td>";
				out += "</tr>\n";
				entry = this.counter.higherEntry(key);
			}
			out += "</tbody>\n";
			out += "</table>\n";
			out += "</div>\n";
		}
		return out;
	}
	
	// getters, setters
	public DateTime getStartTime() {
		return startTime;
	}

	public TreeMap<String, ServiceLog> getCounter() {
		return counter;
	}
	
	public String getPath() {
		return this.getPath(null);
	}
	
	public String getPath(String fileName) {
		try {
			String path = this.getClass().getClassLoader().getResource("").getPath();
			String fullPath = URLDecoder.decode(path, "UTF-8");
			String pathArr[] = fullPath.split("/WEB-INF/classes/");
			//System.out.println(fullPath);
			//System.out.println(pathArr[0]);
			fullPath = pathArr[0];
			String responsePath = "";
			// to read a file from webcontent
			if (fileName != null) 
				responsePath = new File(fullPath).getPath() + File.separatorChar + fileName;
			else
				responsePath = fullPath;
			
			return responsePath;
			
		} catch (Exception e) {
			LOG.error("cz.sysnet.pdf.rest.ApplicationFactory.getPath: " + e.getMessage());
			e.printStackTrace();
			return "";
		}
	}
	
	public String getConfigDirectory() {
		return configDirectory;
	}
	public String getDataDirectory() {
		return dataDirectory;
	}
	public String getPdfDirectory() {
		return pdfDirectory;
	}
	public String getTemplateDirectory() {
		return templateDirectory;
	}
	public String getTemporaryDirectory() {
		return temporaryDirectory;
	}
	public String getUploadDirectory() {
		return uploadDirectory;
	}
	public String getWorkDirectory() {
		return workDirectory;
	}
	
	public String getConfigPath() {
		return configPath;
	}

	public PdfFactory getPdfFactory() {
		return pdfFactory;
	}

	public int getMemoryTreshold() {
		return memoryTreshold;
	}

	public int getMaxFileSize() {
		return maxFileSize;
	}

	public int getMaxRequestSize() {
		return maxRequestSize;
	}

	public String getVersion() {
		return VERSION;
	}	
	public String getTitle() {
		return title;
	}
	public Map<String, String> getTemplateMap() {
		return templateMap;
	}
	public String storeTemplate(Template template) {
		String out = null;
		try {
			if (template == null) throw new PdfRestException("No template (null)");
			String key = template.getKey();
			if (key == null) throw new PdfRestException("Template does not contain key (null)");
			if (key.isEmpty()) throw new PdfRestException("Template does not contain key (empty)");
			String fileName = template.getFile();
			if (fileName == null) throw new PdfRestException("Template does not contain fileName (null)");
			if (fileName.isEmpty()) throw new PdfRestException("Template does not contain fileName (empty)");
			String file = template.getFile();
			if (file == null) throw new PdfRestException("Template does not contain file (null)");
			if (file.isEmpty()) throw new PdfRestException("Template does not contain file (empty)");
			String fileExt = FilenameUtils.getExtension(fileName);
			
			String uploadPath = this.uploadDirectory;
	        String fe = "." + fileExt.toLowerCase();
	        String filePath = uploadPath + File.separator + fileName;
			
			byte[] bytes = Base64.decodeBase64(file);
			File storeFile = new File(filePath);
			FileOutputStream fop = new FileOutputStream(storeFile);
			fop.write(bytes);
		    fop.flush();
		    fop.close();
        	if (TEMPLATE_EXT.equalsIgnoreCase(fe)) {
        		// sablona - pridat mezi sablony a do mapy sablon
        		out = pdfFactory.addTemplate(key, filePath, true);
        		LOG.info("TEMPLATE STORED: " + key + ", " + out);
        		
        	} else {
        		// resource - pridat k sablone 
        		out = pdfFactory.addTemplateResource(key, filePath, true);
        		LOG.info("TEMPLATE RESOURCE STORED: " + key + ", " + out);
        	}
		} catch (Exception e) {
			LOG.error("cz.sysnet.pdf.rest.ApplicationFactory.extractFile FAILED: " + e.getMessage());
			e.printStackTrace();
			out = null;
		}
		return out;
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
			LOG.error("cz.sysnet.pdf.rest.ApplicationFactory.getProjectModel FAILED: " + e.getMessage());
			e.printStackTrace();
			out = null;
		} 
		return out;
	}
}
