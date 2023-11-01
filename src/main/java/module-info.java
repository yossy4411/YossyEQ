module com.yossy.yossyeq {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires org.geotools.main;
    requires org.geotools.opengis;
    requires java.sql;
    requires org.locationtech.jts;
    requires java.desktop;
    requires java.net.http;
    requires org.apache.commons.net;
    requires annotations;


    opens com.yossy4411.yossyeq to javafx.fxml;
    exports com.yossy4411.yossyeq;
    opens com.yossy4411.yossyeq.test to javafx.fxml;
    exports com.yossy4411.yossyeq.test;
}