package cz.sysnet.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.sysnet.pdf.rest.ApplicationFactory;

public class FileDownloadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public static final Logger LOG = Logger.getLogger(FileDownloadServlet.class.getName());
	
	//	http://localhost:8080/myapp/download?file=7
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String fileName = request.getParameter("file");
		LOG.info("File to downolad: " + fileName);
		fileName = URLDecoder.decode(fileName, "UTF-8");
		LOG.info("File decoded: " + fileName);
		
		
		// obtains ServletContext
		ServletContext context = getServletContext();
				
		String relativePath = context.getRealPath("");
		LOG.info("Relative path: " + relativePath);
		
		String workDir = ApplicationFactory.getInstance().getWorkDirectory();
		
		String filePath = workDir + File.separatorChar + fileName;
		
		File downloadFile = new File(filePath);
		FileInputStream inStream = new FileInputStream(downloadFile);
	
		// gets MIME type of the file
		String mimeType = context.getMimeType(filePath);
		if (mimeType == null) mimeType = "application/octet-stream";
		LOG.info("MIME type: " + mimeType);
		
		// modifies response
		response.setContentType(mimeType);
		response.setContentLength((int) downloadFile.length());
		
		// forces download
		String headerKey = "Content-Disposition";
		String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
		response.setHeader(headerKey, headerValue);
		
		// obtains response's output stream
		OutputStream outStream = response.getOutputStream();
		
		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		
		while ((bytesRead = inStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, bytesRead);
		}
		inStream.close();
		outStream.close();		
	}
}