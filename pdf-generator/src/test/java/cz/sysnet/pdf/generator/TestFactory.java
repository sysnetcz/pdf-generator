package cz.sysnet.pdf.generator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestFactory {
	PdfFactory factory = null;

	@Test
	void testCreate() {
		factory = PdfFactory.getInstance();
		assertTrue(factory != null);
		
	}

}
