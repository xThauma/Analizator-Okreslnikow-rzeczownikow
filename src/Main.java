import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.fxmisc.richtext.InlineCssTextArea;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class Main extends Application {
	private StanfordCoreNLP pipeline;
	private Set<String> nounPhrases = new HashSet<>();
	private String text = "";
	private String fileName = "text.txt";
	private List<String> list = new ArrayList<>();
	private Properties props;
	private CoreDocument document;
	private InlineCssTextArea textArea;
	private String originalText = "";

	private List<IndexedString> indexedList = new ArrayList<>();

	private TableView<String> leftTable = new TableView<String>();
	private TableView<String> rightTable = new TableView<String>();

	private Map<String, List<String>> mapWithDeters = new HashMap<>();

	private String[] determiners = { "liczba", "przymiotnik" };
	private String[] words = { "Politics", "Camera", "Hotel", "Room" };

	TableColumn<String, String> wordColumn;
	TableColumn<String, String> deterColumn;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		BorderPane bp = new BorderPane();
		HBox hboxLeft = new HBox();
		hboxLeft.getChildren().add(leftTable);
		HBox hboxTop = new HBox();
		HBox hboxCenter = new HBox();
		HBox hboxRight = new HBox();
		hboxRight.getChildren().add(rightTable);
		bp.setLeft(hboxLeft);
		bp.setCenter(hboxRight);
		Button computeBtn = new Button("Wykonaj");
		computeBtn.setOnAction(e -> {
			highlightAreaMultipleAdjectives();
		});
		hboxCenter.getChildren().add(computeBtn);
		bp.setRight(hboxCenter);
		bp.setTop(hboxTop);
		Button loadFileBtn = new Button("Wczytaj plik");
		loadFileBtn.setOnAction(event -> {
			loadFile(primaryStage);
			initTables();
		});
		hboxTop.getChildren().add(loadFileBtn);

		textArea = new InlineCssTextArea();
		textArea.setWrapText(true);
		HBox hboxBottom = new HBox();
		hboxBottom.getChildren().add(textArea);
		bp.setBottom(hboxBottom);

		DataUtils.initNode(computeBtn);
		DataUtils.initNode(leftTable);
		DataUtils.initNode(rightTable);
		DataUtils.initNodeWithWidth(loadFileBtn, 510);
		DataUtils.initNodeWithWidth(textArea, 510);
		textArea.setMinHeight(300);

		wordColumn = new TableColumn<String, String>("S³owo");
		wordColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
		wordColumn.setPrefWidth(200);
		deterColumn = new TableColumn<String, String>("okreœlnik");
		deterColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
		deterColumn.setPrefWidth(200);

		leftTable.getColumns().add(wordColumn);
		leftTable.setOnMouseClicked(event -> {
			mapWithDeters.forEach((e, v) -> {
				if (leftTable.getSelectionModel().getSelectedItem().equals(e)) {
					if (checkIfAdjective(v.get(0))) {
						initRight(v);
						System.out.print("Weszlo: ");
						for (int i = 0; i < v.size(); i++) {
							System.out.print(v.get(i) + " ");
						}
					} else {
						initRight(new ArrayList<>());
						System.out.print("Nieweszlo: ");
						for (int i = 0; i < v.size(); i++) {
							System.out.print(v.get(i) + " ");
						}
					}
					System.out.println();
				}

			});
		});

		rightTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		rightTable.getColumns().add(deterColumn);

		Scene scene = new Scene(bp, 510, 710);
		primaryStage.setScene(scene);
		primaryStage.show();

	}

	private void highlightArea() {
		textArea.deselect();

		textArea.setStyle(0, indexedList.get(indexedList.size() - 1).getToIndex() - 1, "-fx-font-weight: normal;-fx-fill: black;");

		String selectedFromLeft = leftTable.getSelectionModel().getSelectedItem();
		String selectedFromRight = rightTable.getSelectionModel().getSelectedItem();
		for (int i = 0; i < indexedList.size(); i++) {
			if (i > 0) {
				String deter = indexedList.get(i - 1).getText().toString().toLowerCase();
				String word = indexedList.get(i).getText().toString().toLowerCase();
				;
				if (word.contains(selectedFromLeft.toLowerCase()) && deter.contains(selectedFromRight.toLowerCase())) {
					Integer fromIndex = indexedList.get(i - 1).getFromIndex();
					Integer from2ndWordIndex = indexedList.get(i - 1).getToIndex();
					Integer toIndex = from2ndWordIndex + indexedList.get(i).getBeforeChangeText().length();
					textArea.setStyle(fromIndex, toIndex, "-fx-font-weight: bold;-fx-fill: red;");
					System.out.println("[" + fromIndex + "]+" + deter + "-" + "[" + toIndex + "]+" + indexedList.get(i).getBeforeChangeText());
				}
			}
		}

	}

	private void highlightAreaMultipleAdjectives() {
		textArea.deselect();

		textArea.setStyle(0, indexedList.get(indexedList.size() - 1).getToIndex() - 1, "-fx-font-weight: normal;-fx-fill: black;");

		int rightTableSize = rightTable.getSelectionModel().getSelectedItems().size();
		String selectedFromLeft = leftTable.getSelectionModel().getSelectedItem();
		List<String> selectedWordsFromRight = new ArrayList<>();
		for (int i = 0; i < rightTableSize; i++) {
			selectedWordsFromRight.add(rightTable.getSelectionModel().getSelectedItems().get(i));
		}

		System.out.println("Items============================");
		selectedWordsFromRight.forEach(System.out::println);

		// String selectedFromRight = rightTable.getSelectionModel().getSelectedItem();

		for (int i = 0; i < indexedList.size(); i++) {
			if (i > 2) {
				List<String> deterList = new ArrayList<>();
				for (int j = 1; j <= rightTableSize; j++) {
					deterList.add(indexedList.get(i - j).getText().toString().toLowerCase());
				}
				// String deter = indexedList.get(i - 1).getText().toString().toLowerCase();
				String word = indexedList.get(i).getText().toString().toLowerCase();
				// && deter.contains(selectedFromRight.toLowerCase())
				if (word.contains(selectedFromLeft.toLowerCase())) {
					int counter = 0;
					for (int k = 0; k < rightTableSize; k++) {
						counter += selectedWordsFromRight.contains(deterList.get(k)) ? 1 : 0;
					}
					System.out.println("Counter=" + counter);
					if (counter == rightTableSize) {
						Integer fromIndex = indexedList.get(i - rightTableSize).getFromIndex();
						Integer from2ndWordIndex = indexedList.get(i - 1).getToIndex();
						Integer toIndex = from2ndWordIndex + indexedList.get(i).getBeforeChangeText().length();
						textArea.setStyle(fromIndex, toIndex, "-fx-font-weight: bold;-fx-fill: red;");
					}

//					System.out.println("[" + fromIndex + "]+" + deter + "-" + "[" + toIndex + "]+" + indexedList.get(i).getBeforeChangeText());
				}
			}
		}

	}

	public void initTables() {
		List<CoreLabel> listWithTokens = document.tokens();
		List<CoreLabel> tokens2 = new ArrayList<>();
		List<CoreLabel> listWithCorrectTokens = new ArrayList<>();
		int dots = 0;
		for (int i = 0; i < listWithTokens.size(); i++) {
			if (listWithTokens.get(i).toString().substring(0, 1).equals(".") || listWithTokens.get(i).toString().contains(",") || listWithTokens.get(i).toString().contains("``")
					|| listWithTokens.get(i).toString().contains("''") || listWithTokens.get(i).toString().contains("/") || listWithTokens.get(i).toString().contains("\\")
					|| listWithTokens.get(i).toString().contains("'")) {
				dots++;
				tokens2.add(listWithTokens.get(i));
			} else {
				listWithCorrectTokens.add(listWithTokens.get(i));
//				System.out.println(i + ": " + listWithTokens.get(i));
			}
		}
		List<String> nounPhrases = new ArrayList<>();
		List<String> adiectivePhrases = new ArrayList<>();

		for (CoreLabel label : listWithCorrectTokens) {
			if (label.tag().contains("NN")) {
				nounPhrases.add(label.toString().split("-[0-9]+")[0]);
			} else if (label.tag().contains("JJ") || label.tag().contains("CD")) {
				adiectivePhrases.add(label.originalText());
			} else {
				// System.out.println(label.toString() + " " + label.tag());
			}
		}

		System.out.println("Rzeczowniki: " + nounPhrases.size());
		// nounPhrases.forEach(System.out::println);
		System.out.println("Przymiotniki: " + adiectivePhrases.size());
		// adiectivePhrases.forEach(System.out::println);
		String s = nounPhrases.toString().toLowerCase().replaceAll(",|\\[|\\]", " ");
		String s2 = adiectivePhrases.toString().toLowerCase().replaceAll(",|\\[|\\]", " ");
		// System.out.println(s);
		List<String> lemmed = lemmatize(s);
		List<String> lemmedAdiectives = lemmatize(s2);

		LinkedHashSet<String> hashSet = new LinkedHashSet<>(lemmed);
		LinkedHashSet<String> hashSet2 = new LinkedHashSet<>(lemmedAdiectives);

		List<String> normalList = new ArrayList<>(hashSet);
		List<String> normalList2 = new ArrayList<>(hashSet2);

		final ObservableList<String> wordsList = FXCollections.observableArrayList(normalList);
		final ObservableList<String> detersList = FXCollections.observableArrayList(normalList2);

		SortedList<String> sortedData = new SortedList<>(wordsList);
		SortedList<String> sortedData2 = new SortedList<>(detersList);

		sortedData.comparatorProperty().bind(wordColumn.comparatorProperty());
		// sortedData2.comparatorProperty().bind(deterColumn.comparatorProperty());

		leftTable.setItems(sortedData);
		// rightTable.setItems(sortedData2);

	}

	public boolean checkIfAdjective(String word) {
		StanfordCoreNLP pipeline2 = new StanfordCoreNLP(props);
		CoreDocument document2 = new CoreDocument(word);
		pipeline2.annotate(document2);
		CoreLabel correctToken = document2.tokens().get(0);
		if (correctToken.tag().contains("JJ") || correctToken.tag().contains("CD")) {
			return true;
		}
		return false;
	}

	public void initRight(List<String> list) {
		String s = list.toString().toLowerCase().replaceAll(",|\\[|\\]", " ");
		document = new CoreDocument(s);
		pipeline.annotate(document);

		List<CoreLabel> listWithTokens = document.tokens();
		List<CoreLabel> tokens2 = new ArrayList<>();
		List<CoreLabel> listWithCorrectTokens = new ArrayList<>();
		int dots = 0;
		for (int i = 0; i < listWithTokens.size(); i++) {
			if (listWithTokens.get(i).toString().substring(0, 1).equals(".") || listWithTokens.get(i).toString().contains(",") || listWithTokens.get(i).toString().contains("``")
					|| listWithTokens.get(i).toString().contains("''") || listWithTokens.get(i).toString().contains("/") || listWithTokens.get(i).toString().contains("\\")
					|| listWithTokens.get(i).toString().contains("'")) {
				dots++;
				tokens2.add(listWithTokens.get(i));
			} else {
				listWithCorrectTokens.add(listWithTokens.get(i));
//				System.out.println(i + ": " + listWithTokens.get(i));
			}
		}

		List<String> adiectivePhrases = new ArrayList<>();

		for (CoreLabel label : listWithCorrectTokens) {
			if (label.tag().contains("JJ") || label.tag().contains("CD"))
				adiectivePhrases.add(label.originalText());
		}
		System.out.println("Przymiotniki: " + adiectivePhrases.size());
		String s2 = adiectivePhrases.toString().toLowerCase().replaceAll(",|\\[|\\]", " ");
		List<String> lemmedAdiectives = lemmatize(s2);

		LinkedHashSet<String> hashSet2 = new LinkedHashSet<>(lemmedAdiectives);

		List<String> normalList2 = new ArrayList<>(hashSet2);

		final ObservableList<String> detersList = FXCollections.observableArrayList(normalList2);

		SortedList<String> sortedData2 = new SortedList<>(detersList);
		sortedData2.comparatorProperty().bind(deterColumn.comparatorProperty());
		rightTable.setItems(sortedData2);

	}

	public void loadFile(Stage stage) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Text Files", "*.txt"));
		// Set to user directory or go to default if cannot access
		String userDirectoryString = System.getProperty("user.dir");
		File userDirectory = new File(userDirectoryString);
		fileChooser.setInitialDirectory(userDirectory);
		File selectedFile = fileChooser.showOpenDialog(stage);
		text = "";
		if (selectedFile != null) {
			indexedList = new ArrayList<>();
			textArea.deleteText(0, originalText.length());
			list = new ArrayList<>();
			try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
				String line;
				while ((line = reader.readLine()) != null) {
					list.add(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			for (String s : list) {
				text += s;
			}
			originalText = text;
			textArea.insertText(0, originalText);
			initJars();

			mapWithDeters = new HashMap<>();

			int currIndex = 0;
			String words[] = originalText.split(" ");
			for (int i = 0; i < words.length; i++) {
				int toIndex = currIndex + words[i].length() + 1;
				String word = words[i].toLowerCase();
				String lemmatizedWord = lemmatizeWord(word);
				indexedList.add(new IndexedString(lemmatizedWord, word, currIndex, toIndex));
				if (mapWithDeters.containsKey(lemmatizedWord)) {
					if (i > 2) {
						List<String> oldList = mapWithDeters.get(lemmatizedWord);
						oldList.add(words[i - 1]);
						oldList.add(words[i - 2]);
						oldList.add(words[i - 3]);
						mapWithDeters.put(lemmatizedWord, oldList);
					}
				} else {
					if (i > 2) {
						List<String> lastWords = new ArrayList<>();
						for (int j = 1; j < 4; j++) {
							lastWords.add(words[i - j].toLowerCase());
						}
						// String lastWord = words[i - 1].toLowerCase();
						if (!lemmatizedWord.equals(lastWords.get(0)) && !lemmatizedWord.equals(lastWords.get(1)) && !lemmatizedWord.equals(lastWords.get(2))) {
							List<String> newList = new ArrayList<>();
							newList.add(lastWords.get(0));
							newList.add(lastWords.get(1));
							newList.add(lastWords.get(2));
							mapWithDeters.put(lemmatizedWord, newList);
						}
					}
				}
				currIndex = toIndex;
			}

			mapWithDeters.forEach((e, v) -> {
				System.out.println(e + "-" + v);
			});

			// indexedList.forEach(System.out::println);
			text = text.replaceAll("(?<=[.!/\\?])(?!$)", " ");
		}

	}

	private List<String> lemmatize(String documentText) {
		List<String> lemmas = new LinkedList<String>();
		Annotation document = new Annotation(documentText);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				lemmas.add(token.get(LemmaAnnotation.class));
			}
		}

		return lemmas;
	}

	private String lemmatizeWord(String txt) {
		Annotation document = new Annotation(txt);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				return token.get(LemmaAnnotation.class);
			}
		}

		return "1";
	}

	private void initJars() {
		props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit, pos, lemma,parse");
		props.setProperty("coref.algorithm", "neural");
		pipeline = new StanfordCoreNLP(props);
		document = new CoreDocument(text);
		pipeline.annotate(document);
	}

}
