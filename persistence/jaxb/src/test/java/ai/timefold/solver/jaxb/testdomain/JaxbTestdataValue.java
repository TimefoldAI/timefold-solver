package ai.timefold.solver.jaxb.testdomain;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JaxbTestdataValue extends JaxbTestdataObject {

    public JaxbTestdataValue() {
    }

    public JaxbTestdataValue(String code) {
        super(code);
    }

}
