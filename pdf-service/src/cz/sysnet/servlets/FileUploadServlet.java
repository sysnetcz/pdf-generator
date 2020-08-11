package cz.sysnet.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
//import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cz.sysnet.pdf.rest.ApplicationFactory;

 
/**
 * A Java servlet that handles file upload from client.
 *
 * @author www.codejava.net
 */
public class FileUploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    //public static final Logger LOG = Logger.getLogger(FileUploadServlet.class.getName());
    public static final Logger LOG = LogManager.getLogger(FileUploadServlet.class);
	 
    // location to store file uploaded
    //private static final String UPLOAD_DIRECTORY = "upload";
 
    // upload settings
    //private static final int MEMORY_THRESHOLD   = 1024 * 1024 * 3;  // 3MB
    //private static final int MAX_FILE_SIZE      = 1024 * 1024 * 40; // 40MB
    //private static final int MAX_REQUEST_SIZE   = 1024 * 1024 * 50; // 50MB
    
    private static final int MEMORY_THRESHOLD   = ApplicationFactory.getInstance().getMemoryTreshold();
    private static final int MAX_FILE_SIZE      = ApplicationFactory.getInstance().getMaxFileSize();
    private static final int MAX_REQUEST_SIZE   = ApplicationFactory.getInstance().getMaxRequestSize();
    
 
    /**
     * Upon receiving file upload submission, parses the request to read
     * upload data and saves the file on disk.
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // checks if the request actually contains upload file
        if (!ServletFileUpload.isMultipartContent(request)) {
            // if not, we stop here
            PrintWriter writer = response.getWriter();
            writer.println("Error: Form must has enctype=multipart/form-data.");
            writer.flush();
            LOG.error("Error: Form must has enctype=multipart/form-data.");
            return;
        }
 
        // configures upload settings
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // sets memory threshold - beyond which files are stored in disk
        factory.setSizeThreshold(MEMORY_THRESHOLD);
        // sets temporary location to store files
        factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
 
        ServletFileUpload upload = new ServletFileUpload(factory);
         
        // sets maximum size of upload file
        upload.setFileSizeMax(MAX_FILE_SIZE);
         
        // sets maximum size of request (include file + form data)
        upload.setSizeMax(MAX_REQUEST_SIZE);
 
        // constructs the directory path to store upload file
        // this path is relative to application's directory
        //String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
        String uploadPath = ApplicationFactory.getInstance().getUploadDirectory();
         
        // creates the directory if it does not exist
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) uploadDir.mkdir();
        
        String fileName = "";
        String filePath = "";
        String fileExt = "";
        String key = "-neuvedeno-"; 
        String description = "-neuvedeno-";
        String message = "";
        boolean uploaded = false;
        boolean hasKey = false;
        boolean success = false;
    	
        try {
            // parses the request's content to extract file data
            List<FileItem> formItems = upload.parseRequest(request);
 
            if (formItems != null && formItems.size() > 0) {
                // iterates over form's fields
                for (FileItem item : formItems) {
                    if (!item.isFormField()) {
                    	// processes only fields that are not form fields
                        fileName = new File(item.getName()).getName();
                        fileExt = FilenameUtils.getExtension(fileName);
                        filePath = uploadPath + File.separator + fileName;
                        File storeFile = new File(filePath);
 
                        // saves the file on disk
                        item.write(storeFile);
                        message = "Upload has been done successfully! <br />" + filePath;
                        LOG.info("Upload has been done successfully: " + filePath);
                        uploaded = true;
                        
                        
                        
                    } else {
                    	// processes only fields that are form fields
                    	String name = item.getFieldName();
                    	String value = item.getString();
                    	if (name.equalsIgnoreCase("key")) {
                    		if (value != null) if (!value.isEmpty()) {
                    			key = value;
                    			LOG.info("KEY: " + key);
                    			hasKey = true;
                    		}
                    	}
                    	else if (name.equalsIgnoreCase("description")) {
                       		if (value != null) if (!value.isEmpty()) {
                       			description = value;
                    			LOG.info("DESCRIPTION: " + description);
                    		}
                    	}
                    }
                }
            }
        } catch (Exception e) {
        	message = "There was an error: " + e.getMessage();
            LOG.error("There was an error: " + e.getMessage());
            e.printStackTrace();
        }
        if (uploaded && hasKey) {
        	// Ted je vsechno v poradku. Uploaded file se zpracuje
        	String fe = "." + fileExt.toLowerCase();
        	if (ApplicationFactory.TEMPLATE_EXT.equalsIgnoreCase(fe)) {
        		// sablona - pridat mezi sablony a do mapy sablon
        		String o = ApplicationFactory.getInstance().getPdfFactory().addTemplate(key, filePath, true);
        		LOG.info("TEMPLATE STORED: " + key + ", " + o);
        		message +=  "<br/><strong>TEMPLATE STORED: </strong>" + key +", " + o;
        		success = true;
        		
        	} else {
        		// resource - pridat k sablone 
        		String o = ApplicationFactory.getInstance().getPdfFactory().addTemplateResource(key, filePath, true);
        		LOG.info("TEMPLATE RESOURCE STORED: " + key + ", " + o);
        		message +=  "<br/><strong>TEMPLATE RESOURCE STORED: </strong>" + key +", " + o;
        		success = true;
        	}
        }
    	if (!success) {
         	message +=  "<br/><strong>NO TEMPLATE PROCESSED: </strong>>" + key +" (" + description + ")";
        	LOG.warn("NO TEMPLATE PROCESSED - " + "KEY: " + key + ", " + "FILE: " + fileName);
    	}
    	if(success) {
    		request.setAttribute("alert", "success");
    		request.setAttribute("alertText", "PODAŘILO SE!");
    	} else {
    		request.setAttribute("alert", "danger");
    		request.setAttribute("alertText", "OUHA!!!");
    	}
        request.setAttribute("message", message);
        
        // redirects client to message page
        getServletContext().getRequestDispatcher("/message.jsp").forward(request, response);
    }
}