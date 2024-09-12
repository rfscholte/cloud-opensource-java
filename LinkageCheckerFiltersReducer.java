import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;


/**
 * This class expects an xml file with LinkageCheckerFilters as root element.
 * Next a comment containing the id of the maven module (or any other identifier)
 * Followed by an optional LinkageCheckerFilter for that module.
 *
 * Depending on the status of the baseline creation file, it might require manual adding of the root tag!!
 */
class LinkageCheckerFiltersReducer {
  public static void main(String[] args) {
    Path inputFile = Path.of(args[0]);

    if(!Files.exists(inputFile)) {
      System.out.println("Input file " +inputFile+ " not found");
      System.exit(1);
    }

    Path outputFile = Path.of(args[1]);

    XMLEvent event = null;
    try (InputStream is = Files.newInputStream(inputFile, StandardOpenOption.READ)) {
      // Create an XMLInputFactory
      XMLInputFactory factory = XMLInputFactory.newInstance();

      // Create an XMLEventReader
      XMLEventReader eventReader = factory.createXMLEventReader(is);

      String lastComment = "";

      // Variables to hold the current state
      Source source = null;
      Target target = null;
      String reason = null;

      Symbol symbol = null;

      String name = null;
      String className = null;

      Map<LinkageError, Collection<String>> linkageErrorMap = new HashMap<>();

      int linkageErrorCount = 0;
      // Iterate through the events
      while (eventReader.hasNext()) {
        event = eventReader.nextEvent();
        

        if (event.isStartElement()) {
          StartElement startElement = event.asStartElement();
          String elementName = startElement.getName().getLocalPart();

          switch (elementName) {
            case "Method":
            case "Field":
              className = startElement.getAttributeByName(new QName("className")).getValue();
            case "Class":
              name = startElement.getAttributeByName(new QName("name")).getValue();
              break;
            case "Reason":
              event = eventReader.nextEvent();
              if (event.isCharacters()) {
                reason = event.asCharacters().getData().trim();
              }
              break;
            case "LinkageCheckerFilters":
            case "LinkageCheckerFilter":
            case "LinkageError":
            case "Source":
            case "Target":
              break;
            default:
              throw new UnsupportedOperationException("Don't recognize " + elementName);
          }
        } else if (event.isEndElement()) {
          String elementName = event.asEndElement().getName().getLocalPart();
          switch (elementName) {
            case "Method":
              symbol = new Method(className, name);
              className = null;
              name = null;
              break;
            case "Field":
              symbol = new Field(className, name);
              className = null;
              name = null;
              break;
            case "Class":
              symbol = new Clazz(name);
              className = null;
              name = null;
              break;
            case "Source":
              source = new Source(symbol);
              symbol = null;
              className = null;
              break;
            case "Target":
              target = new Target(symbol);
              symbol = null;
              className = null;
              break;
            case "LinkageError":
              linkageErrorCount++;
              linkageErrorMap.computeIfAbsent(new LinkageError(source, target, reason), k -> new HashSet<>()).add(lastComment);
              source = null;
              target = null;
              reason = null;
          }
        } else if (event.getEventType() == XMLEvent.COMMENT) {
          Comment comment = (Comment) event;
          lastComment = comment.getText().trim();
        }
      }

      System.out.println("LinkageErrors: " + linkageErrorCount);
      System.out.println("Flattened LinkageErrors: " + linkageErrorMap.size());

      try (BufferedWriter os = Files.newBufferedWriter(outputFile, StandardOpenOption.CREATE)) {
        os.write("<LinkageCheckerFilter>\n");
        linkageErrorMap.forEach((k,v) -> {
          try {
            os.write("  <!--" + v + "-->\n" + k);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
        os.write("</LinkageCheckerFilter>");
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(event.getLocation().toString());
    }
  }

  record LinkageError(Source source, Target target, String reason) {

    LinkageError{
      Objects.requireNonNull(source);
      Objects.requireNonNull(target);
      Objects.requireNonNull(reason);
    }

    @Override
    public String toString() {
      return "  <LinkageError>\n" + source + target + "    <Reason>" + reason.replace("<", "&lt;").replace(">", "&gt;") + "</Reason>\n  </LinkageError>\n";
    }
  }

  record Source(Symbol symbol) {
    @Override
    public String toString() {
      return "    <Source>\n" + symbol + "\n" + "    </Source>\n";
    }
  }

  record Target(Symbol symbol) {
    @Override
    public String toString() {
      return "    <Target>\n" + symbol + "\n" + "    </Target>\n";
    }
  }

  interface Symbol {
  }

  record Clazz(String name) implements Symbol {
    Clazz {
      Objects.requireNonNull(name);
    }

    @Override
    public String toString() {
      return "      <Class name=\"" + name + "\"/>";
    }
  }

  record Field(String className, String name) implements Symbol {
    @Override
    public String toString() {
      return "      <Field className=\"" + className + "\" name=\"" + name + "\"/>";
    }
  }

  record Method(String className, String name) implements Symbol {
    @Override
    public String toString() {
      return "      <Method className=\"" + className + "\" name=\"" + name.replace("<", "&lt;").replace(">", "&gt;") + "\"/>";
    }
  }

}