module com.yossy4411.yossyeq {
    requires javafx.controls;
    requires javafx.fxml;
            
                            
    opens com.yossy4411.yossyeq to javafx.fxml;
    exports com.yossy4411.yossyeq;
}