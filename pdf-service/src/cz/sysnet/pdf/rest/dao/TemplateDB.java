package cz.sysnet.pdf.rest.dao;
 
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import cz.sysnet.pdf.generator.PdfFactory;
import cz.sysnet.pdf.rest.model.Template;
 
public class TemplateDB {
    private static Map<String, Template> templateDB = new ConcurrentHashMap<String, Template>();
     
    public static String createTemplate(String key, String value){
    	Template t = new Template();
    	t.setKey(key);
    	t.setValue(value);
        templateDB.put(t.getKey(), t);
        return t.getKey();
    }
     
    public static Template getTemplate(String key) {
        return templateDB.get(key);
    }
     
    public static List<Template> getAllTemplates() {
        return new ArrayList<Template>(templateDB.values());
    }
     
    public static Template removeTemplate(String key){
        return templateDB.remove(key);
    }
     
    public static Template updateTemplate(String key, Template t) {
        return templateDB.put(key, t);
    }
    
    public static String toJson() {
		return PdfFactory.gson.toJson(templateDB);
    }
    
    public static String toJson(String filePath) {
    	String out = null;
    	try(Writer writer = new OutputStreamWriter(new FileOutputStream(filePath) , "UTF-8")){
    		PdfFactory.gson.toJson(templateDB, writer);
    		out = toJson();
            
        } catch (Exception e) {
        	System.out.println(e.getMessage());
        	e.printStackTrace();
        	out = "{\"error\":\""+e.getMessage()+"\"}";       	
        }
    	return out;   	
    }
    
    @SuppressWarnings("unchecked")
	public static String fromJson(String filePath) {
    	String out = null;
    	try {
    		Reader reader = new InputStreamReader(TemplateDB.class.getResourceAsStream(filePath), "UTF-8");
    		Map<String, Template> tdb = PdfFactory.gson.fromJson(reader, templateDB.getClass());
    		templateDB = tdb;
    		out = toJson();
    		
    	} catch (Exception e) {
        	System.out.println(e.getMessage());
        	e.printStackTrace();
        	out = "{\"error\":\""+e.getMessage()+"\"}";       	
        }
    	return out;
    }
    
    @Deprecated
    public static void fillMockData() {
    	templateDB = new ConcurrentHashMap<String, Template>();
    	createTemplate("T1", "http://exapmle.com/template1");
    	createTemplate("T2", "http://exapmle.com/template2");
    	createTemplate("T3", "http://exapmle.com/template3");
    	createTemplate("T4", "http://exapmle.com/template4");
    }
    
    /**
     * Naplní databázi z mapy šablon (cz.sysnet.pdf.generator.PdfFactory)
     * 
     * @param templateMap	mapa šablon (cz.sysnet.pdf.generator.PdfFactory) 
     */
    public static void loadTemplateMap(Map<String, String> templateMap) {
    	templateDB = new ConcurrentHashMap<String, Template>();
    	if (templateMap != null) {
    		for (Entry<String, String> entry:templateMap.entrySet()) {
    			createTemplate(entry.getKey(), entry.getValue());
    		}
    	}
    }
    
    /**
     * Vytvoří mapu šablon použitelnou pro cz.sysnet.pdf.generator.PdfFactory
     * 
     * @return	mapa šablon pro cz.sysnet.pdf.generator.PdfFactory
     */
    public static Map<String, String> getTemplateMap() {
    	Map<String, String> out = new HashMap<String, String>();
    	if (templateDB != null) {
    		for (Entry<String, Template> entry:templateDB.entrySet()) {
    			out.put(entry.getKey(), entry.getValue().getValue());
    		}
    	}
    	return out;    	
    }
}