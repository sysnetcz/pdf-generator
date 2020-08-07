package cz.sysnet.pdf.generator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TestFactory {
	public static final String FS = PdfFactory.FILE_SEPARATOR;
	public static final String BASE_PATH = (System.getProperty("java.io.tmpdir") + FS + "pdf-factory-base" + FS).replace(FS + FS, FS);
	PdfFactory factory = null;
	
	@Test
	void testProjectVersion() {		
		String out = PdfFactory.getProjectVersion();
		System.out.println(out);
		assertTrue(out != null);
	}
	

	@Test
	void testCreate() {
		if (factory == null) factory = PdfFactory.getInstance();
		assertTrue(factory != null);
		
		if (!factory.getTemplateMap().isEmpty()) {
			assertTrue(factory.removeTemplateMap());
			factory.cleanTemplateMap();
			assertTrue(factory.getTemplateMap().isEmpty());
		}
	}
	
	@Test
	void testCreateInBasePath() {
		factory = null;
		factory = PdfFactory.getInstance();
		assertTrue(factory != null);
		
		if (!factory.getTemplateMap().isEmpty()) {
			assertTrue(factory.removeTemplateMap());
			factory.cleanTemplateMap();
			assertTrue(factory.getTemplateMap().isEmpty());
		}
	}
	
	@Test
	void testTemplateMap() {
		if (factory == null) this.testCreate();
		assertTrue(factory.getTemplateMap() != null);
		
		String o = factory.addTemplate("PR", "src/test/resources/templates/picture_rl.jrxml", false);
		assertTrue(o != null);
		
		List<String> sf = new ArrayList<String>();
		sf.add("src/test/resources/templates/picture_rl_data-adapter.xml");
		sf.add("src/test/resources/resources/Foto3.JPG");
		sf.add("src/test/resources/resources/picture_rl_data.json");
		
		o = factory.addTemplateResource("PR", sf, false);
		assertTrue(o != null);
		
		assertTrue(factory.storeTemplateMap());
		
		factory.cleanTemplateMap();
		assertTrue(factory.getTemplateMap().isEmpty());
		assertTrue(factory.loadTemplateMap());
		assertTrue(!factory.getTemplateMap().isEmpty());
	}
	
	@Test
	void testCreatePdf() {
		if (factory == null) this.testTemplateMap();
		
		File jsonFile = new File("src/test/resources/resources/picture_rl_data.json");
		assertTrue(jsonFile.exists());
		
		String o = factory.createPdfFromJsonByKey("PR", jsonFile); 
		assertTrue(o != null);
		//System.out.println(o);
	}
	
	
	@Test
	void testUtils() {
		String o = PdfFactory.generatePdfFileName();
		assertTrue(o != null);
		//System.out.println(o);
	}

	
	

}
