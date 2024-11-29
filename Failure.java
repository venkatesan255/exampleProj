import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Data;

/**
 * Represents a failure with type, message, and content.
 */
@Data
public class Failure {

    @JacksonXmlText // Maps the content as the text value of the XML element
    private String content;

    @JacksonXmlProperty(isAttribute = true) // Marks 'type' as an XML attribute
    private String type;

    @JacksonXmlProperty(isAttribute = true) // Marks 'message' as an XML attribute
    private String message;
}
