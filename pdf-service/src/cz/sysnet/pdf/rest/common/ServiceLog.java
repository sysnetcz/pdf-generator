package cz.sysnet.pdf.rest.common;


import java.io.Serializable;

public class ServiceLog implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Long runs;
	private Long warnings;
	private Long errors;
	private Long runtime;
	
	
	public Long getRuns() {
		return runs;
	}
	public void setRuns(Long runs) {
		this.runs = runs;
	}
	public Long getWarnings() {
		return warnings;
	}
	public void setWarnings(Long warnings) {
		this.warnings = warnings;
	}
	public Long getErrors() {
		return errors;
	}
	public void setErrors(Long errors) {
		this.errors = errors;
	}
	public Long getRuntime() {
		return runtime;
	}
	public void setRuntime(Long runtime) {
		this.runtime = runtime;
	}	
}
