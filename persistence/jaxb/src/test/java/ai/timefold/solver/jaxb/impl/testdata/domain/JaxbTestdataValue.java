package ai.timefold.solver.jaxb.impl.testdata.domain;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JaxbTestdataValue extends JaxbTestdataObject {

    public JaxbTestdataValue() {
    }

    public JaxbTestdataValue(String code) {
        super(code);
    }

}
