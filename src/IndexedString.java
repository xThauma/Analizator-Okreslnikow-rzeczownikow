
public class IndexedString {
	String text;
	String beforeChangeText;
	Integer fromIndex;
	Integer toIndex;

	public IndexedString(String text, String beforeChangeText, Integer fromIndex, Integer toIndex) {
		super();
		this.text = text;
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
		this.beforeChangeText = beforeChangeText;
	}

	public String getBeforeChangeText() {
		return beforeChangeText;
	}

	public void setBeforeChangeText(String beforeChangeText) {
		this.beforeChangeText = beforeChangeText;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Integer getFromIndex() {
		return fromIndex;
	}

	public void setFromIndex(Integer fromIndex) {
		this.fromIndex = fromIndex;
	}

	public Integer getToIndex() {
		return toIndex;
	}

	public void setToIndex(Integer toIndex) {
		this.toIndex = toIndex;
	}



	@Override
	public String toString() {
		return "IndexedString [text=" + text + ", beforeChangeText=" + beforeChangeText + ", fromIndex=" + fromIndex + ", toIndex=" + toIndex + "]";
	}

	public IndexedString(String text, Integer index) {
		super();
		this.text = text;
		this.fromIndex = index;
	}

}
