package cz.sysnet.pdf.rest.model;
 
import java.util.List;
 
import javax.ws.rs.core.Link;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
 
@XmlRootElement(name = "templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class Templates
{
    @XmlAttribute
    private Integer size;
     
    @XmlJavaTypeAdapter(Link.JaxbAdapter.class)
    @XmlElement
    private Link link;
     
    @XmlElement
    private List<Template> templates;
 
    public Integer getSize() {
        return size;
    }
 
    public void setSize(Integer size) {
        this.size = size;
    }
 
    public Link getLink() {
        return link;
    }
 
    public void setLink(Link link) {
        this.link = link;
    }
 
    public List<Template> getTemplates() {
        return templates;
    }
 
    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }
}