# pdf-generator

Knihovna pro generování PDF na základě šablon JasperReport

## Jak se to používá?
Knihova obsahuje jedinou třídu - singleton **PdfFactory**. Metody singletonu se volají formou **PdfFactory.getInstance.metoda...**. Singleton  umožňuje:

1. Spravovat šablony JasperSoft
2. Vytvářet dokumenty PDF pomocí šablon

## Správa šablon
Šablony lze vložit jako parametr metody **createPdfFromJson**  jako *InputStream* nebo cestu k souboru se šablonou, případe *File* šablony.  Tato vlastnost umožňuje používat libovolné šablony.
U metody **createPdfFromJsonByKey** se šablona uložená na serveru identifikuje pomocí klíče. tato vlastnost umožňuje spravovat seznam šablon

Při inicializaci se singletonu se hledá konfigurační soubor *templates.json* obsahující mapu šablon.

Pomocí metod **addTemplate**, **addTemplateResource**, **cleanTemplateMap**, **loadTemplateMap**, **removeTemplateMap** a **storeTemplateMap** se provádí správa šablon.

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

## Tvorba PDF

	@Test
	void testCreatePdf() {
		if (factory == null) this.testTemplateMap();
		
		File jsonFile = new File("src/test/resources/resources/picture_rl_data.json");
		assertTrue(jsonFile.exists());
		
		String o = factory.createPdfFromJsonByKey("PR", jsonFile); 
		assertTrue(o != null);
		//System.out.println(o);
	}
