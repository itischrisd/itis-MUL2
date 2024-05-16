import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private static final String defaultText = "Pociąg ze stacji Warszawa Wschodnia do stacji Poznań Główny przez stacje Kutno, Konin, odjedzie z toru drugiego przy peronie trzecim.";
    private static final Map<String, String> POLISH_TO_ENGLISH = Map.of("ą", "a", "ć", "c", "ę", "e", "ł", "l", "ń", "n", "ó", "o", "ś", "s", "ż", "z", "ź", "z");

    public static void main(String[] args) throws Exception {
        String textToSay = normalizeText(args.length == 0 ? defaultText : String.join(" ", args));
        Map<String, File> soundFiles = mapSoundFiles(Paths.get("nagrania"));
        List<File> soundsToPlay = mapWordsToSounds(textToSay.split(" "), soundFiles);
        playSounds(soundsToPlay);
    }

    private static String normalizeText(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD).toLowerCase();
        StringBuilder stringBuilder = new StringBuilder();
        normalized.chars().mapToObj(c -> String.valueOf((char) c))
                .forEach(c -> stringBuilder.append(POLISH_TO_ENGLISH.getOrDefault(c, c)));
        return stringBuilder.toString().replaceAll("[^a-z ]", "");
    }

    private static Map<String, File> mapSoundFiles(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".wav"))
                    .collect(Collectors.toMap(path -> path.getFileName().toString().replace('_', ' ')
                            .replace(".wav", ""), Path::toFile));
        }
    }

    private static List<File> mapWordsToSounds(String[] words, Map<String, File> fileMap) throws Exception {
        List<File> fileList = new ArrayList<>();
        for (int i = 0; i < words.length; ) {
            boolean matchFound = false;
            for (int j = words.length; j > i; j--) {
                String potentialKey = String.join(" ", Arrays.copyOfRange(words, i, j));
                if (fileMap.containsKey(potentialKey)) {
                    fileList.add(fileMap.get(potentialKey));
                    i = j;
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                throw new Exception("No matching file found for the sequence starting at index " + i);
            }
        }
        return fileList;
    }

    private static void playSounds(List<File> soundsToPlay) {
        for (File soundFile : soundsToPlay) {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
                Thread.sleep(500 + (clip.getMicrosecondLength() / 1000));
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
