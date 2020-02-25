

import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.StyleClassedTextArea;

import javafx.scene.control.Control;

public class DataUtils {
	public final static int MAX_WIDTH = 170;

	public static void initNode(Control control) {
		control.setMinWidth(MAX_WIDTH);
		control.setMaxWidth(MAX_WIDTH);
		control.setStyle("-fx-alignment: CENTER;");
	}
	
	public static void initNodeWithWidth(Control control, Integer width) {
		control.setMinWidth(width);
		control.setMaxWidth(width);
		control.setStyle("-fx-alignment: CENTER;");
	}


	public static void initNodeWithWidth(InlineCssTextArea textArea, int width) {
		textArea.setMinWidth(width);
		textArea.setMaxWidth(width);
		textArea.setStyle("-fx-alignment: CENTER;");
	}
}
