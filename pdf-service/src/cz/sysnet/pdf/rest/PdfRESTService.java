package cz.sysnet.pdf.rest;
 
/**
 * @author sysnet.cz
 * 
 */
 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;

import cz.sysnet.pdf.generator.PdfFactory;
import cz.sysnet.pdf.rest.common.Message;
import cz.sysnet.pdf.rest.dao.TemplateDB;
import cz.sysnet.pdf.rest.model.Pdf;
import cz.sysnet.pdf.rest.model.Pdfs;
import cz.sysnet.pdf.rest.model.Template;
import cz.sysnet.pdf.rest.model.Templates;
 
@Path("")
public class PdfRESTService {
	
	private static final Logger LOG = LogManager.getLogger(PdfRESTService.class);

    
    /**
     * Initialize the application with default configurations
     * */
    static {
    	//TemplateDB.fillMockData();
    	TemplateDB.loadTemplateMap(ApplicationFactory.getInstance().getTemplateMap());
    }

    /**
     * Get template  collection resource mapped at path "HTTP GET /template"
     */  
    @GET
	@Path("template")
	@Produces(MediaType.APPLICATION_JSON+";charset=utf-8")
    public Templates getTemplate(@Context UriInfo uriInfo) {
        ApplicationFactory.getInstance().counterRunIncrement("template GET");
        
        reloadTemplateDb();
        List<Template> list = TemplateDB.getAllTemplates(); 
        
        Templates templates = new Templates();
        templates.setTemplates(list);
        templates.setSize(list.size());
        
        //JerseyUriBuilder builder = (JerseyUriBuilder) uriInfo.getAbsolutePathBuilder();
    	
        //Set link for primary collection
        Link link = Link.fromUri(uriInfo.getPath()).rel("uri").build();
        templates.setLink(link);
          
        //Set links in template items
        for(Template t: list){
            Link lnk = Link.fromUri(uriInfo.getPath() + "/" + t.getKey()).rel("self").build();
            t.setLink(lnk);
        }
        LOG.info("template GET");
        return templates;
    }
    
    
    /**
     * Get individual template resource mapped at path "HTTP GET /template/{key}"
     */
    @GET
    @Path("template/{key}")
	@Produces(MediaType.APPLICATION_JSON+";charset=utf-8")
    public Response getTemplateByKey(@PathParam("key") String key, @Context UriInfo uriInfo){
    	ApplicationFactory.getInstance().counterRunIncrement("template GET");
    	
    	reloadTemplateDb();
        Template item = TemplateDB.getTemplate(key);
        
        if(item == null) {
        	ApplicationFactory.getInstance().counterRunIncrement("template GET");
        	LOG.error("template GET {"+key+"} NOT FOUND" );
            return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
        }
          
        if(item != null){
        	JerseyUriBuilder builder = (JerseyUriBuilder) uriInfo.getAbsolutePathBuilder();
        	//UriBuilder builder = UriBuilder.fromResource(PdfRESTService.class).path(PdfRESTService.class, "getTemplateByKey");
            
            Link link = Link.fromUri(builder.build(key)).rel("self").build();
            item.setLink(link);
        }
        LOG.info("template GET {"+key+"}");       
        return Response.status(javax.ws.rs.core.Response.Status.OK).entity(item).build();
    }
    
    /**
     * Create NEW template resource in template collection resource
     */
    @POST
	@Path("template")
    @Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON+";charset=utf-8")
    public Response createTemplate(Template item, @Context UriInfo uriInfo){
     	ApplicationFactory.getInstance().counterRunIncrement("template POST");
     	boolean hasValue = false;
     	boolean hasFile = false;
     	String value = null;
     	
     	if (item.getValue() != null) if (!item.getValue().isEmpty()) hasValue = true;
     	if (item.getFile() != null) if (!item.getFile().isEmpty()) hasFile = true;
     	if(!hasValue && !hasFile) {
       		ApplicationFactory.getInstance().counterErrorIncrement("template POST");
    		LOG.error("template POST: BAD REQUEST (no value or file)" );
    		return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST)
    				.entity(new Message("Template value or file not found"))
    				.build();
 		
     	} else if (hasFile) {
     		value = ApplicationFactory.getInstance().storeTemplate(item);
     	} else {
     		value = item.getValue();
     	}
        if (value == null) {
        	ApplicationFactory.getInstance().counterErrorIncrement("template POST");
        	LOG.error("template POST: BAD REQUEST CONTENT" );
            return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST)
                            .entity(new Message("Template cannot be stored"))
                            .build();
        }
        item.setValue(value); 
        String key = TemplateDB.createTemplate(item.getKey(), item.getValue());
        Link lnk = Link.fromUri(uriInfo.getPath() + "/" + key).rel("self").build();
        LOG.info("template POST");       
        return Response.status(javax.ws.rs.core.Response.Status.CREATED).location(lnk.getUri()).build();
    }
    
    @GET
    @Path("template/{key}/pdf/{id}")
    @Produces("application/pdf")
    public Response getPdf(@PathParam("key") String key, @PathParam("id") String id, @Context UriInfo uriInfo) {
    	ApplicationFactory.getInstance().counterRunIncrement("PDF GET");
    	try {
    	 	Template template = TemplateDB.getTemplate(key);
            if(template == null) {
            	ApplicationFactory.getInstance().counterErrorIncrement("PDF GET");
            	LOG.error("PDF GET: template NOT FOUND" );
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            LOG.info("PDF GET: template FOUND");
        	
            String fileName = key + "_" + id + ".pdf";
            File file = ApplicationFactory.getInstance().getPdfFile(fileName);
            if ((file == null) || !file.exists()) {
             	ApplicationFactory.getInstance().counterErrorIncrement("PDF GET");
            	LOG.error("PDF GET: pdf NOT FOUND" );
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            FileInputStream fileInputStream;
        	fileInputStream = new FileInputStream(file);
    		
            Response.ResponseBuilder responseBuilder = Response.ok((Object) fileInputStream);
            responseBuilder.type("application/pdf");
            responseBuilder.header("Content-Disposition", "filename=" + fileName);
            return responseBuilder.build();
        	
    	} catch (FileNotFoundException e) {
    		LOG.error("PDF GET: " + e.getMessage());
    		e.printStackTrace();
    		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    	}
    }   
    
    
    /**
     * Get pdf collection resource for specified template mapped at path "HTTP GET /template/{key}/pdf"
     */  
    @GET
    @Path("template/{key}/pdf")
    @Produces(MediaType.APPLICATION_JSON+";charset=utf-8")
    public Response getPdfCollection(@PathParam("key") String key, @Context UriInfo uriInfo){
    	ApplicationFactory.getInstance().counterRunIncrement("PDF GET");
      	Template template = TemplateDB.getTemplate(key);
        if(template == null) {
        	ApplicationFactory.getInstance().counterErrorIncrement("PDF GET");
        	LOG.error("PDF GET: template NOT FOUND" );
            return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
        }
        LOG.info("PDF GET: template FOUND");
        List<File> fileList = ApplicationFactory.getInstance().getPdfFileList(key);
        LOG.info("PDF GET: fileList" + Integer.toString(fileList.size()));
        
        List<Pdf> pdfList = new ArrayList<Pdf>();
        for (File item:fileList) {
        	Pdf pdf = new Pdf();
        	pdf.setKey(key);
        	Pattern pattern = Pattern.compile("_(.*?).pdf");
    		Matcher matcher = pattern.matcher(item.getName());
    		if (matcher.find()) pdf.setId(matcher.group(1));
        	pdf.setValue(item.getPath());
        	JerseyUriBuilder builder = (JerseyUriBuilder) uriInfo.getAbsolutePathBuilder();
            Link link = Link.fromUri(builder.build(key)).rel("self").build();
            pdf.setLink(link);
            pdfList.add(pdf);
        }
        Pdfs pdfs = new Pdfs();
        pdfs.setPdfs(pdfList);
        pdfs.setSize(pdfList.size());
   
        Link link = Link.fromUri(uriInfo.getPath()).rel("uri").build();
        pdfs.setLink(link);
    
        LOG.info("PDF GET");
        return Response.status(javax.ws.rs.core.Response.Status.OK).entity(pdfs).build();
    }
    
    /**
     * Create NEW PDF file using  template
     */
    @POST
	@Path("template/{key}/pdf")
    @Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON+";charset=utf-8")
    public Response createPdf(@PathParam("key") String key, String data, @Context UriInfo uriInfo){
     	ApplicationFactory.getInstance().counterRunIncrement("PDF POST");
        if(data == null)  {
        	ApplicationFactory.getInstance().counterErrorIncrement("PDF POST");
        	LOG.error("PDF POST: BAD REQUEST" );
            return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST)
                            .entity(new Message("Data not found"))
                            .build();
        }
      	Template template = TemplateDB.getTemplate(key);
        if(template == null) {
        	ApplicationFactory.getInstance().counterErrorIncrement("PDF POST");
        	LOG.error("PDF POST: template NOT FOUND" );
            return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
        }
        
        LOG.info("KEY: " + key + ", DATA: " + data);
        Pdf pdf = new Pdf();
        pdf.setKey(key);
        String pdfFileName = PdfFactory.generatePdfFileName(key);
    	Pattern pattern = Pattern.compile("_(.*?).pdf");
		Matcher matcher = pattern.matcher(pdfFileName);
		if (matcher.find()) pdf.setId(matcher.group(1));
		pdf.setValue(ApplicationFactory.getInstance().getPdfFactory().createPdfFromJsonByKey(key, data, pdfFileName));
    	
	    if(pdf != null){
	    	LOG.info("URI0: " + uriInfo.getAbsolutePath());
	    	LOG.info("URI1: " + uriInfo.getAbsolutePath()+"/"+pdf.getId());
	    	Link link = Link.fromPath(uriInfo.getAbsolutePath()+"/"+pdf.getId()).rel("self").build();
	    	//JerseyUriBuilder builder = (JerseyUriBuilder) uriInfo.getAbsolutePathBuilder();
	    	//Link link = Link.fromUri(builder.build(key)).rel("self").build();
	    	pdf.setLink(link);
	    	pdf.setValue(pdfFileName);
	    }
	    LOG.info("PDF POST {"+key+"}");       
        return Response.status(javax.ws.rs.core.Response.Status.OK).entity(pdf).build();
    }
    
    
    
    
    /**
     * Modify EXISTING template resource by it's "key" at path "/template/{key}"
     */
    @PUT
    @Path("template/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response updateConfiguration(@PathParam("key") String key, Template item, @Context UriInfo uriInfo){
    	ApplicationFactory.getInstance().counterRunIncrement("template PUT");
 
    	Template origItem = TemplateDB.getTemplate(key);
        if(origItem == null) {
        	ApplicationFactory.getInstance().counterErrorIncrement("template PUT");
        	LOG.error("template PUT: NOT FOUND" );
            return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
        }
         
        if(item.getValue() == null)  {
        	ApplicationFactory.getInstance().counterErrorIncrement("template PUT");  
        	LOG.error("template PUT: BAD REQUEST" );
            return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST)
                            .entity(new Message("Template value not found"))
                            .build();
        }
 
        TemplateDB.updateTemplate(key, item);
        LOG.info("template PUT {"+key+"}");       
        return Response.status(javax.ws.rs.core.Response.Status.OK).entity(new Message("Template Updated Successfully")).build();
    }
 
    /**
     * Delete template resource by it's "key" at path "/template/{key}"
     * */
    @DELETE
    @Path("template/{key}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response deleteConfiguration(@PathParam("key") String key, @Context UriInfo uriInfo) {
    	ApplicationFactory.getInstance().counterRunIncrement("template DELETE");  
         
        Template origTemplate = TemplateDB.getTemplate(key);
        if(origTemplate == null) {
        	ApplicationFactory.getInstance().counterErrorIncrement("template DELETE");
        	LOG.error("template DELETE: NOT FOUND" );
            return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
        }
         
        TemplateDB.removeTemplate(key);
        LOG.info("template DELETE {"+key+"}");       
        return Response.status(javax.ws.rs.core.Response.Status.OK).build();
    }
 	
	@POST
	@Path("templateTest")
	@Consumes(MediaType.APPLICATION_JSON)
	//@Produces(MediaType.APPLICATION_JSON+";charset=utf-8")
	@Produces("application/json;charset=utf-8")
	public Response templateREST(InputStream incomingData) {
		String inData = parseInputData(incomingData);
		
		// return HTTP response 200 in case of success
		LOG.info("templateTest POST");       
        return Response.status(200).entity(inData).build();
	}
 
	@GET
	@Path("verify")
	//@Produces(MediaType.TEXT_PLAIN+";charset=utf-8")
	@Produces(MediaType.APPLICATION_JSON+";charset=utf-8")
	public Response verifyRESTService(InputStream incomingData) {
		ApplicationFactory.getInstance().counterRunIncrement("verify GET");
        
		Map<String, String> result = new HashMap<String, String>();
		result.put("verify", "PdfRESTService Successfully started..");
		
		// return HTTP response 200 in case of success
		LOG.info("verify GET");       
        return Response.status(200).entity(result).build();
	}
	
	
	private static String parseInputData(InputStream incomingData) {
		StringBuilder templateBuilder = new StringBuilder();
		String out = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(incomingData, StandardCharsets.UTF_8));
			String line = null;
			while ((line = in.readLine()) != null) {
				templateBuilder.append(line);
			}
			out = templateBuilder.toString();
			System.out.println("Data Received: " + out);
			
		} catch (Exception e) {
			System.out.println("Error Parsing: - ");
			e.printStackTrace();
			out = "{\"error\":\"Parsing input data\"}";
		}
		return out;		
	}
	
	private static void reloadTemplateDb() {
		// regenerate template map form filesystem
        Map<String, String> templateMap = ApplicationFactory.getInstance().initTemplateMap();	
        TemplateDB.loadTemplateMap(templateMap);		
	}
}