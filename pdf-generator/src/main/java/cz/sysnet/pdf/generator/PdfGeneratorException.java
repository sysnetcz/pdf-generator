package cz.sysnet.pdf.generator;

public class PdfGeneratorException extends Exception {
	private static final long serialVersionUID = 1L;

	public PdfGeneratorException() {
		super();
	}

	public PdfGeneratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PdfGeneratorException(String message, Throwable cause) {
		super(message, cause);
	}

	public PdfGeneratorException(String message) {
		super(message);
	}

	public PdfGeneratorException(Throwable cause) {
		super(cause);
	}
}
