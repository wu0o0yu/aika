package network.aika.meta;

import network.aika.Model;
import network.aika.debugger.AIKADebugger;
import network.aika.elements.activations.Activation;
import network.aika.parser.Context;
import network.aika.parser.ParserPhase;
import network.aika.parser.TrainingParser;
import network.aika.text.Document;
import network.aika.tokenizer.SimpleWordTokenizer;
import network.aika.tokenizer.Tokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static network.aika.parser.ParserPhase.COUNTING;
import static network.aika.parser.ParserPhase.TRAINING;

public class TextSectionTest extends TrainingParser {

    private PhraseTemplateModel templateModel;
    private Tokenizer tokenizer;

    private String exampleTxt = "(Senior) Java Softwareentwickler (m/w/d) Schulverwaltung\n" +
            "ort-DBMünchen\n" +
            "\n" +
            "\n" +
            " \n" +
            "Das IT-Dienstleistungszentrum (IT-DLZ) des Freistaats Bayern stellt leistungsfähige und zukunftsorientierte e-Government-Anwendungen sowie zentrale Infrastrukturen für den Betrieb von IT-Systemen für die Bayerische Staatsverwaltung und für die Fachgerichte zur Verfügung. Wir bieten mit über 550 Mitarbeiterinnen und Mitarbeitern und dem Einsatz modernster Technologien ein breites Spektrum an IT-Dienstleistungen.  \n" +
            "\n" +
            "Zur Verstärkung unseres Teams für die Konzeption und Weiterentwicklung von Anwendungen der Schulverwaltung suchen wir zum nächstmöglichen Zeitpunkt einen/e\n" +
            "\n" +
            "(SENIOR) JAVA SOFTWAREENTWICKLER (w/m/d) Schulverwaltung\n" +
            "\n" +
            "Vollzeit/Teilzeit | Unbefristet | Entgeltgruppe bis E11 | Besoldungsgruppe bis A11\n" +
            "\n" +
            "Arbeitsort: St.-Martin-Str. 47, 81541 München (bis zu 80 % Homeoffice) \n" +
            "\n" +
            "Der Bewerbungsschluss ist der 17.07.2023\n" +
            "\n" +
            " \n" +
            "IHRE AUFGABEN\n" +
            "Weiterentwicklung von verteilten Anwendungen für den Schulbereich in Zusammenarbeit mit dem Bayerischen Staatsministerium für Unterricht und Kultus\n" +
            "Prozessanalyse und konzeptionelle Tätigkeiten im Rahmen der Entwicklungstätigkeit\n" +
            "Anforderungsanalyse und -dokumentation von Kundenanforderungen\n" +
            "Betreuung und Beratung von Fachanwendern und Fachanwendungs-Administratoren\n" +
            "Abstimmung mit anderen Teams im Referat\n" +
            " \n" +
            "IHR PROFIL\n" +
            "Erfolgreich abgeschlossenes Studium (Dipl.-FH oder Bachelor) im Studienfach Informatik, oder eines vergleichbaren Studiengangs (z. B. Wirtschaftsinformatik oder Verwaltungsinformatik) oder\n" +
            "eine erfolgreich abgeschlossene IT-Berufsausbildung z. B. als Fachinformatiker (w/m/d) in der Fachrichtung Anwendungsentwicklung mit mehrjähriger Berufserfahrung im genannten Aufgabenbereich und in Kombination mit umfangreichen Fortbildungen in mindestens zwei weiteren IT-Fachgebieten\n" +
            "Bachelor-Absolventen im Bereich Informatik sind bei uns sehr willkommen und starten mit Entgeltgruppe 10\n" +
            "Erfahrung in der Konzeption und Entwicklung von Software\n" +
            "Vertiefte Kenntnisse und Erfahrungen in der Softwareentwicklung mit Java\n" +
            "Technische Fachkenntnisse bzw. Berufserfahrung beim Einsatz von JPA/Hibernate und relationalen Datenbanken (SQL)\n" +
            "Fähigkeit, sich in bestehende Softwarelandschaften einzuarbeiten und diese weiterzuentwickeln\n" +
            "Engagement und Eigeninitiative sowie Organisations-, Kommunikations- und Teamfähigkeit\n" +
            "Strukturierter, pragmatischer und lösungsorientierter Arbeitsstil\n" +
            "Sehr gute Deutschkenntnisse in Wort und Schrift (mindestens Stufe C2 des GER)\n" +
            "WÜNSCHENSWERT\n" +
            "\n" +
            "Erfahrung in der agilen Softwareentwicklung mit Scrum\n" +
            "Technische Fachkenntnisse bzw. Berufserfahrung in der Entwicklung von Swing und JavaFX-basierten Benutzeroberflächen sowie Erfahrung mit OSGi\n" +
            "Kenntnisse und Erfahrungen mit Microservice-Architekturen (REST) und SOA\n" +
            "Erfahrung und sicherer Umgang mit den im IT-DLZ eingesetzten Entwicklungswerkzeugen (Eclipse, JIRA, GIT, Bitbucket, Bamboo, Nexus, Confluence)\n" +
            "Mehrjährige Praxis im Umgang mit Kunden und externen Dienstleistern\n" +
            " \n" +
            "WIR BIETEN\n" +
            "Einen unbefristeten Arbeitsvertrag und krisensicheren Arbeitsplatz\n" +
            "Eine gute Work-Life-Balance durch flexible Arbeitszeiten, Familientage sowie ein Gleitzeitkonto und bis zu 80 % Homeoffice\n" +
            "Für Personen mit Bachelor-Abschluss besteht, je nach persönlicher Eignung, Leistung und Befähigung, die Möglichkeit der Übernahme in ein Beamtenverhältnis der 3. Qualifikationsebene\n" +
            "Gewährung eines IT-Fachkräftegewinnungszuschlages für Beamte/innen in Höhe von 400 € brutto monatlich, bei Vorliegen der persönlichen Voraussetzungen (Art. 60a BayBesG)\n" +
            "30 Tage Urlaub pro vollem Kalenderjahr, zusätzlich sind der 24.12. und der 31.12. frei\n" +
            "Alle Sozialleistungen des öffentlichen Dienstes in Bayern und eine zusätzliche Betriebsrente (VBL) sowie Jahressonderzahlung\n" +
            "Anspruchsvolle und zukunftsorientierte Aufgabengebiete\n" +
            "Umfangreiche Fortbildungsmöglichkeiten und ein aktives Gesundheitsmanagement\n" +
            "Vergünstigtes Job-Ticket für die Deutsche Bahn und den Münchner Verkehrs- und Tarifverbund\n" +
            "Unterstützung bei der Suche nach einer Staatsbedienstetenwohnung\n";

    @BeforeEach
    public void init() {
        Model model = new Model();

        templateModel = new PhraseTemplateModel(model);
        templateModel.initStaticNeurons();

        model.setN(0);

        tokenizer = new SimpleWordTokenizer(templateModel);
    }

    @Override
    protected Document initDocument(String txt, Context context, ParserPhase phase) {
        Document doc = super.initDocument(txt, context, phase);
        if(phase == TRAINING) {
            AIKADebugger.createAndShowGUI(doc, 100, 150);
        }

        return doc;
    }

    @Override
    public boolean check(Activation iAct) {
        return true;
    }

    @Test
    public void testTextSections() {
        log.info("Start");
        process(exampleTxt, null, COUNTING);
        templateModel.initTemplates();
        process(exampleTxt, null, TRAINING);
    }

    @Override
    protected AbstractTemplateModel getTemplateModel() {
        return templateModel;
    }

    @Override
    public Tokenizer getTokenizer() {
        return tokenizer;
    }

}
