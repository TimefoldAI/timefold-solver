package ai.timefold.solver.xstream.impl.testdata.domain;

import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("xStreamTestdataValue")
public class XStreamTestdataValue extends TestdataObject {

    public XStreamTestdataValue() {
    }

    public XStreamTestdataValue(String code) {
        super(code);
    }

}
