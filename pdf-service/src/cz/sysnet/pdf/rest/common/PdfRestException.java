package cz.sysnet.pdf.rest.common;

public class PdfRestException extends Exception {
	private static final long serialVersionUID = 1L;

	public PdfRestException() {
		super();
	}

	public PdfRestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PdfRestException(String message, Throwable cause) {
		super(message, cause);
	}

	public PdfRestException(String message) {
		super(message);
	}

	public PdfRestException(Throwable cause) {
		super(cause);
	}
	
}
