import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageTestGUI {
    @FXML
    ImageView imageView;
    @FXML
    Label labelID;
    Image noImageForProduct;

    @FXML
    void initialize(){
        Platform.runLater(() -> {
        noImageForProduct = new Image("/product_images/NO_IMAGE.png");
        });
    }

    void setImage(int id, Image image){
        Platform.runLater(() -> {
            labelID.setText(String.valueOf(id));
            imageView.setImage(image);
        });
    }

    void noImageForProduct(String productID) {
        imageView.setImage(noImageForProduct);
    }
}
