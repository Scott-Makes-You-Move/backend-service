package nl.optifit.backendservice.utility;

import lombok.extern.slf4j.Slf4j;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.util.Span;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MaskingUtil {

    private static final Map<String, String> entityMap = new LinkedHashMap<>();

    private static final String PHONE_REGEX = "(\\+\\d{1,3}[-.\\s]?)?\\d{2,4}[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}";
    private static final String EMAIL_REGEX = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b";
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX, Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);

    private static final Map<String, List<String>> MEDICAL_TERMS = Map.of(
            "conditions", Arrays.asList("sciatica", "acl tear", "depression", "anxiety",
                    "arthritis", "sprain", "fracture"),
            "medications", Arrays.asList("ibuprofen", "paracetamol", "sertraline", "amoxicillin"),
            "professions", Arrays.asList("doctor", "physiotherapist", "nurse",
                    "therapist", "psychologist")
    );

    private MaskingUtil() {
    }

    public static String maskUserMessage(String userMessage) {
        long startMasking = System.nanoTime();
        entityMap.clear();
        List<Span> allSpans = new ArrayList<>();

        allSpans.addAll(detectStandardEntities(userMessage));
        allSpans.addAll(Arrays.asList(detectPatternEntities(userMessage, PHONE_PATTERN)));
        allSpans.addAll(Arrays.asList(detectPatternEntities(userMessage, EMAIL_PATTERN)));
        allSpans.addAll(detectMedicalTerms(userMessage));

        String maskedOutput = applyMasks(userMessage, allSpans);

        log.debug("Masking took {}ms", (System.nanoTime() - startMasking) / 1_000_000);

        return maskedOutput;
    }

    public static String unmaskAssistantMessage(String maskedAssistantMessage) {
        String result = maskedAssistantMessage;
        List<String> masks = new ArrayList<>(entityMap.keySet());
        Collections.sort(masks, Collections.reverseOrder());

        for (String mask : masks) {
            result = result.replace(mask, entityMap.get(mask));
        }
        return result;
    }

    private static List<Span> detectStandardEntities(String sentence) {
        List<Span> spans = new ArrayList<>();

        // English models
        spans.addAll(Arrays.asList(detectEntities(sentence, "/models/en-ner-person.bin")));
        spans.addAll(Arrays.asList(detectEntities(sentence, "/models/en-ner-location.bin")));
        spans.addAll(Arrays.asList(detectEntities(sentence, "/models/en-ner-date.bin")));
        spans.addAll(Arrays.asList(detectEntities(sentence, "/models/en-ner-time.bin")));
        spans.addAll(Arrays.asList(detectEntities(sentence, "/models/en-ner-organization.bin")));

        // Dutch models
        spans.addAll(Arrays.asList(detectEntities(sentence, "/models/nl-ner-person.bin")));
        spans.addAll(Arrays.asList(detectEntities(sentence, "/models/nl-ner-location.bin")));
        spans.addAll(Arrays.asList(detectEntities(sentence, "/models/nl-ner-misc.bin")));
        spans.addAll(Arrays.asList(detectEntities(sentence, "/models/nl-ner-organization.bin")));

        return spans;
    }

    private static List<Span> detectMedicalTerms(String sentence) {
        List<Span> spans = new ArrayList<>();
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        Span[] tokenSpans = tokenizer.tokenizePos(sentence);

        List<String> allTerms = new ArrayList<>();
        MEDICAL_TERMS.values().forEach(allTerms::addAll);
        String patternStr = "\\b(" + String.join("|", allTerms) + ")\\b";
        Pattern medicalPattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);

        Matcher matcher = medicalPattern.matcher(sentence);
        while (matcher.find()) {
            int matchStart = matcher.start();
            int matchEnd = matcher.end();

            int tokenStart = -1;
            int tokenEnd = -1;
            for (int i = 0; i < tokenSpans.length; i++) {
                Span tokenSpan = tokenSpans[i];
                if (tokenStart == -1 && tokenSpan.getStart() <= matchStart && tokenSpan.getEnd() > matchStart) {
                    tokenStart = i;
                }
                if (tokenSpan.getStart() < matchEnd && tokenSpan.getEnd() >= matchEnd) {
                    tokenEnd = i + 1;
                    break;
                }
            }

            if (tokenStart != -1 && tokenEnd != -1) {
                spans.add(new Span(tokenStart, tokenEnd));
            }
        }
        return spans;
    }

    private static String applyMasks(String sentence, List<Span> allSpans) {
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(sentence);
        Span[] tokenSpans = tokenizer.tokenizePos(sentence);
        int entityCount = 1;

        allSpans.sort(Comparator.comparingInt(Span::getStart));

        boolean[] maskedTokens = new boolean[tokens.length];
        String[] resultTokens = tokens.clone();

        for (Span span : allSpans) {
            if (isAlreadyMasked(span, maskedTokens)) continue;

            Span firstToken = tokenSpans[span.getStart()];
            Span lastToken = tokenSpans[span.getEnd() - 1];
            String original = sentence.substring(firstToken.getStart(), lastToken.getEnd());

            String mask = "[ENTITY_" + entityCount++ + "]";
            entityMap.put(mask, original);

            for (int i = span.getStart(); i < span.getEnd(); i++) {
                resultTokens[i] = i == span.getStart() ? mask : "";
                maskedTokens[i] = true;
            }
        }

        return rebuildSentence(resultTokens);
    }

    private static boolean isAlreadyMasked(Span span, boolean[] maskedTokens) {
        for (int i = span.getStart(); i < span.getEnd(); i++) {
            if (maskedTokens[i]) return true;
        }
        return false;
    }

    private static Span[] detectEntities(String sentence, String modelPath) {
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(sentence);

        try (InputStream is = MaskingUtil.class.getResourceAsStream(modelPath)) {
            if (is == null) {
                log.warn("Model not found: '{}'", modelPath);
                return new Span[0];
            }
            TokenNameFinderModel model = new TokenNameFinderModel(is);
            NameFinderME finder = new NameFinderME(model);
            return finder.find(tokens);
        } catch (IOException e) {
            log.error("Error while detecting entities", e);
            throw new RuntimeException(e);
        }
    }

    private static Span[] detectPatternEntities(String sentence, Pattern pattern) {
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        Span[] tokenSpans = tokenizer.tokenizePos(sentence);

        List<Span> resultSpans = new ArrayList<>();
        Matcher matcher = pattern.matcher(sentence);

        while (matcher.find()) {
            int matchStart = matcher.start();
            int matchEnd = matcher.end();

            int tokenStart = -1;
            int tokenEnd = -1;

            for (int i = 0; i < tokenSpans.length; i++) {
                Span tokenSpan = tokenSpans[i];
                if (tokenStart == -1 && tokenSpan.getStart() <= matchStart && tokenSpan.getEnd() > matchStart) {
                    tokenStart = i;
                }
                if (tokenSpan.getStart() < matchEnd && tokenSpan.getEnd() >= matchEnd) {
                    tokenEnd = i + 1;
                    break;
                }
            }

            if (tokenStart != -1 && tokenEnd != -1) {
                resultSpans.add(new Span(tokenStart, tokenEnd));
            }
        }

        return resultSpans.toArray(new Span[0]);
    }

    private static String rebuildSentence(String[] tokens) {
        StringBuilder sb = new StringBuilder();
        String prevToken = "";

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            if (!sb.isEmpty() && !prevToken.endsWith("(") && !token.matches("^[.,;:!?)]$")) {
                sb.append(" ");
            }

            sb.append(token);
            prevToken = token;
        }

        return sb.toString()
                .replaceAll("\\s+([.,;:!?])", "$1")
                .replaceAll("\\(\\s+", "(")
                .replaceAll("\\s+\\)", ")");
    }
}
