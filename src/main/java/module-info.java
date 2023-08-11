module com.yossy.yossyeq {
    requires javafx.controls;
    requires javafx.fxml;
    requires json.path;
    requires com.fasterxml.jackson.databind;
    requires org.json;
    requires json.smart;
    requires org.slf4j;
    requires org.geotools.main;
    requires org.geotools.opengis;
    requires java.sql;
    requires org.locationtech.jts;
    requires java.desktop;
    requires core;
    requires java.net.http;
    requires org.osgi.service.upnp;
    requires org.apache.commons.net;


    opens com.yossy4411.yossyeq to javafx.fxml;
    exports com.yossy4411.yossyeq;
    opens com.yossy4411.yossyeq.test to javafx.fxml;
    exports com.yossy4411.yossyeq.test;
}