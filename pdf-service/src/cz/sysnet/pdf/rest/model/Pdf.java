package cz.sysnet.pdf.rest.model;
 
import javax.ws.rs.core.Link;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
 
@XmlRootElement(name="pdf")
@XmlAccessorType(XmlAccessType.FIELD)
public class Pdf
{
    @XmlAttribute
    private String id;
    @XmlJavaTypeAdapter(Link.JaxbAdapter.class)
    private Link link;
    @XmlElement
    private String key;
    @XmlElement
    private String value;
    
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public Link getLink() {
		return link;
	}
	public void setLink(Link link) {
		this.link = link;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}   
}