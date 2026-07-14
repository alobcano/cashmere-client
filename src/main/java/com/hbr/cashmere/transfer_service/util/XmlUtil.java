package com.hbr.cashmere.transfer_service.util;

import com.hbr.cashmere.transfer_service.constants.XmlConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Slf4j
public class XmlUtil {

  private static final DocumentBuilderFactory FACTORY = DocumentBuilderFactory.newInstance();
  private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();
  private static final String APACHE_INDENT_AMOUNT_KEY =
      "{http://xml.apache.org/xslt}indent-amount";
  private static final String DEFAULT_INDENT_AMOUNT = "2";

  private XmlUtil() {}

  /**
   * Helper method to remove all nodes with a given tag name from a document.
   *
   * @param document The XML document
   * @param tagName The tag name to remove
   */
  private static void removeNodesByTagName(Document document, String tagName) {
    NodeList nodes = document.getElementsByTagName(tagName);
    while (nodes.getLength() > 0) {
      nodes.item(0).getParentNode().removeChild(nodes.item(0));
    }
  }

  /**
   * Helper method to convert custom HTML tags to standard div tags while preserving attributes.
   *
   * @param htmlDoc The Jsoup HTML document
   * @param tagName The custom tag name to convert (e.g., "article-sidebar")
   */
  private static void convertCustomTagsToDiv(org.jsoup.nodes.Document htmlDoc, String tagName) {
    Elements customElements = htmlDoc.select(tagName);
    for (Element element : customElements) {
      Element div = htmlDoc.createElement("div");
      // Copy all attributes from the custom element to the div
      div.attributes().addAll(element.attributes());
      // Move all children from the custom element to the div
      div.insertChildren(0, element.childNodes());
      // Replace the custom element with the div
      element.replaceWith(div);
    }
  }

  /**
   * Helper method to replace deprecated <strike> tags with standard <s> tags.
   *
   * @param htmlDoc The Jsoup HTML document
   */
  private static void replaceStrikeTags(org.jsoup.nodes.Document htmlDoc) {
    Elements strikeElements = htmlDoc.select("strike");
    for (Element strikeElement : strikeElements) {
      Element sTag = htmlDoc.createElement("s");
      // Copy all attributes from the strike element to the s tag
      sTag.attributes().addAll(strikeElement.attributes());
      // Move all children from the strike element to the s tag
      sTag.insertChildren(0, strikeElement.childNodes());
      // Replace the strike element with the s tag
      strikeElement.replaceWith(sTag);
    }
  }

  /**
   * Helper method to remove problematic URLs that contain patterns that can be misinterpreted.
   *
   * @param htmlDoc The Jsoup HTML document
   */
  private static void removeProblematicUrls(org.jsoup.nodes.Document htmlDoc) {
    Elements links = htmlDoc.select("a[href]");
    for (Element link : links) {
      String href = link.attr("href");
      // Remove Google cache URLs with colons (e.g., cache:KEY:URL) that can be misinterpreted as
      // IPv6
      if (href.contains("cache:") && href.split(":").length > 3) {
        link.remove();
      }
    }
  }

  /**
   * Decodes HTML entities (&lt;, &gt;) to their corresponding brackets in a string.
   *
   * @param input The string with HTML entities
   * @return The decoded string
   */
  public static String decodeHtmlEntities(String input) {
    if (input == null) return null;
    return input.replace("&lt;", "<").replace("&gt;", ">");
  }

  /**
   * Decodes HTML entities (&lt;, &gt;) in a byte array and returns the decoded byte array.
   *
   * @param inputBytes The byte array with HTML entities
   * @return The decoded byte array
   */
  public static byte[] decodeHtmlEntities(byte[] inputBytes) {
    if (inputBytes == null) return new byte[0];
    String input = new String(inputBytes);
    String decoded = decodeHtmlEntities(input);
    return decoded.getBytes();
  }

  /**
   * Extracts the <title> value from the XML byte array as a String.
   *
   * @param xmlBytes The XML content as a byte array
   * @return The title as a String, or null if not found
   */
  public static String extractTitle(byte[] xmlBytes) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes)) {
      DocumentBuilderFactory namespaceAwareFactory = DocumentBuilderFactory.newInstance();
      namespaceAwareFactory.setNamespaceAware(true);
      DocumentBuilder builder = namespaceAwareFactory.newDocumentBuilder();
      Document doc = builder.parse(bais);
      String title;
      doc.getDocumentElement().normalize();
      NodeList titleNodes = doc.getElementsByTagNameNS("*", XmlConstants.TITLE_TAG);
      if (titleNodes.getLength() > 0) {
        title = titleNodes.item(0).getTextContent().trim();
        return title;
      }
    } catch (Exception e) {
      log.error("Error extracting title from XML", e);
    }
    return "";
  }

  /**
   * Extracts the <author> values from the XML byte array as a String array.
   *
   * @param xmlBytes The XML content as a byte array
   * @return The authors as a String[], or empty array if not found
   */
  public static String[] extractAuthors(byte[] xmlBytes) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes)) {
      DocumentBuilderFactory namespaceAwareFactory = DocumentBuilderFactory.newInstance();
      namespaceAwareFactory.setNamespaceAware(true);
      DocumentBuilder builder = namespaceAwareFactory.newDocumentBuilder();
      Document doc = builder.parse(bais);
      doc.getDocumentElement().normalize();
      NodeList authorNodes = doc.getElementsByTagNameNS("*", XmlConstants.AUTHOR_TAG);
      Set<String> authorsSet = new LinkedHashSet<>();
      for (int i = 0; i < authorNodes.getLength(); i++) {
        Node authorNode = authorNodes.item(i);
        String name = null;
        NodeList children = authorNode.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
          Node child = children.item(j);
          if (child.getNodeType() == Node.ELEMENT_NODE
              && XmlConstants.NAME_TAG.equals(child.getNodeName())) {
            name = child.getTextContent().trim();
            break;
          }
        }
        if (name != null && !name.isEmpty() && !authorsSet.contains(name)) {
          authorsSet.add(name);
        }
      }
      return authorsSet.toArray(new String[0]);
    } catch (Exception e) {
      log.error("Error extracting authors from XML", e);
    }
    return new String[0];
  }

  /**
   * Extracts the <published> value from the XML byte array as a String.
   *
   * @param xmlBytes The XML content as a byte array
   * @return The published date as a LocalDateTime, or null if not found or parse error
   */
  public static String extractDate(byte[] xmlBytes, String tagName) {
    if (tagName == null || tagName.isBlank()) {
      return null;
    }

    try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes)) {
      DocumentBuilderFactory namespaceAwareFactory = DocumentBuilderFactory.newInstance();
      namespaceAwareFactory.setNamespaceAware(true);
      DocumentBuilder builder = namespaceAwareFactory.newDocumentBuilder();
      Document doc = builder.parse(bais);
      doc.getDocumentElement().normalize();

      String localTagName = tagName;
      if (localTagName.contains(":")) {
        localTagName = localTagName.substring(localTagName.indexOf(':') + 1);
      }

      NodeList dateNodes = doc.getElementsByTagNameNS("*", localTagName);
      if (dateNodes.getLength() > 0) {
        return dateNodes.item(0).getTextContent().trim();
      }

      // Fallback for XML that does not use namespaces.
      NodeList fallbackNodes = doc.getElementsByTagName(localTagName);
      if (fallbackNodes.getLength() > 0) {
        return fallbackNodes.item(0).getTextContent().trim();
      }
    } catch (Exception e) {
      log.error("Error extracting date from XML", e);
    }
    return null;
  }

  /**
   * Helper method to clean HTML content by removing unwanted elements and normalizing tags.
   *
   * @param htmlDoc The Jsoup HTML document to clean
   */
  private static void cleanHtmlContent(org.jsoup.nodes.Document htmlDoc) {
    // Remove images and iframes
    htmlDoc.select(XmlConstants.IMAGE_TAG).remove();
    htmlDoc.select(XmlConstants.IFRAME_TAG).remove();

    // Remove script-related tags
    htmlDoc.select("script").remove();
    htmlDoc.select("noscript").remove();

    // Unwrap nobr tags (preserve content but remove the tag)
    htmlDoc.select("nobr").forEach(Element::unwrap);

    // Remove deprecated Flash/embed tags
    htmlDoc.select("object").remove();
    htmlDoc.select("embed").remove();
    htmlDoc.select("param").remove();

    // Remove malformed custom tags like <http:...>
    htmlDoc.select("http\\:").remove();

    // Remove problematic URLs and image file links
    removeProblematicUrls(htmlDoc);
    removeImageFileLinks(htmlDoc);

    // Convert custom HBR tags to standard div tags
    convertCustomTagsToDiv(htmlDoc, "article-ideainbrief");
    convertCustomTagsToDiv(htmlDoc, "article-sidebar");
    convertCustomTagsToDiv(htmlDoc, "article-promo");
    convertCustomTagsToDiv(htmlDoc, "hbr-component");

    // Replace deprecated <strike> tags with <s> tags
    replaceStrikeTags(htmlDoc);

    // Remove image-related attributes from all elements
    htmlDoc.select("[data-image-representation-uri]").removeAttr("data-image-representation-uri");
  }

  /**
   * Helper method to remove links to image files.
   *
   * @param htmlDoc The Jsoup HTML document
   */
  private static void removeImageFileLinks(org.jsoup.nodes.Document htmlDoc) {
    Elements links = htmlDoc.select("a[href]");
    for (Element link : links) {
      String href = link.attr("href").toLowerCase();
      if (href.endsWith(".gif")
          || href.endsWith(".jpg")
          || href.endsWith(".jpeg")
          || href.endsWith(".png")
          || href.endsWith(".webp")
          || href.endsWith(".svg")) {
        link.remove();
      }
    }
  }

  /**
   * Helper method to process content nodes and clean their HTML.
   *
   * @param document The XML document
   */
  private static void processContentNodes(Document document) {
    NodeList contentNodes = document.getElementsByTagNameNS("*", XmlConstants.CONTENT_TAG);
    for (int i = 0; i < contentNodes.getLength(); i++) {
      Node contentNode = contentNodes.item(i);
      String html = contentNode.getTextContent();
      org.jsoup.nodes.Document htmlDoc = Jsoup.parse(html);

      cleanHtmlContent(htmlDoc);

      String cleanedHtml = htmlDoc.body().html();
      while (contentNode.hasChildNodes()) {
        contentNode.removeChild(contentNode.getFirstChild());
      }
      org.w3c.dom.CDATASection cdata = document.createCDATASection(cleanedHtml);
      contentNode.appendChild(cdata);
    }
  }

  /**
   * Helper method to remove all image-related XML tags from the document.
   *
   * @param document The XML document
   */
  private static void removeImageRelatedTags(Document document) {
    // Remove image tags
    NodeList imgNodes = document.getElementsByTagName(XmlConstants.IMAGE_TAG);
    while (imgNodes.getLength() > 0) {
      imgNodes.item(0).getParentNode().removeChild(imgNodes.item(0));
    }

    // Remove iframe tags
    removeNodesByTagName(document, XmlConstants.IFRAME_TAG);

    // Remove author image URI tags
    removeNodesByTagName(document, XmlConstants.URI_TAG);

    // Remove other image-related tags
    removeNodesByTagName(document, XmlConstants.HERO_RESOURCE_TYPE_TAG);
    removeNodesByTagName(document, XmlConstants.RETIRED_IMAGE_URI_TAG);
    removeNodesByTagName(document, XmlConstants.THUMBNAIL_IMAGE_URI_TAG);
    removeNodesByTagName(document, XmlConstants.RETIRED_IMAGE_TITLE_TAG);
    removeNodesByTagName(document, XmlConstants.FEATURE_IMAGE_URI_TAG);
    removeNodesByTagName(document, XmlConstants.FEATURE_IMAGE_TITLE_TAG);
    removeNodesByTagName(document, XmlConstants.FEATURE_IMAGE_CREDITS_TAG);
    removeNodesByTagName(document, XmlConstants.RETIRED_IMAGE_CREDITS_TAG);
  }

  /**
   * Processes XML content by removing images and cleaning HTML content.
   *
   * @param xmlBytes The XML content as a byte array
   * @return The cleaned XML content as a byte array, or empty array if error
   */
  public static byte[] processXml(byte[] xmlBytes) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      DocumentBuilderFactory namespaceAwareFactory = DocumentBuilderFactory.newInstance();
      namespaceAwareFactory.setNamespaceAware(true);
      DocumentBuilder builder = namespaceAwareFactory.newDocumentBuilder();
      Document document = builder.parse(bais);
      document.getDocumentElement().normalize();

      processContentNodes(document);
      removeImageRelatedTags(document);

      Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(APACHE_INDENT_AMOUNT_KEY, DEFAULT_INDENT_AMOUNT);
      transformer.transform(new DOMSource(document), new StreamResult(baos));
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Error processing XML file", e);
      return new byte[0];
    }
  }

  /**
   * Saves the given XML byte array to a file at the specified path.
   *
   * @param xmlBytes The XML content as a byte array
   * @param filePath The path (including filename) where the XML file will be saved
   * @return true if saved successfully, false otherwise
   */
  public static boolean saveXmlToFile(byte[] xmlBytes, String filePath) {
    try (FileOutputStream fos = new FileOutputStream(filePath)) {
      fos.write(xmlBytes);
      fos.flush();
      return true;
    } catch (Exception e) {
      log.error("Error saving XML to file: {}", filePath, e);
      return false;
    }
  }

  /**
   * Replaces the content of the name tag (with any namespace prefix like asset:name, p0:name, etc.)
   * with the provided title.
   *
   * @param xmlBytes The XML content as a byte array
   * @param title The new title to set in the name tag
   * @return The modified XML content as a byte array with the name tag content replaced, or empty
   *     array if error
   */
  public static byte[] replaceAssetNameTag(byte[] xmlBytes, String title) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(bais);
      document.getDocumentElement().normalize();

      // Replace name tag content with the provided title (matches any namespace prefix)
      NodeList nameNodes = document.getElementsByTagNameNS("*", "name");
      for (int i = 0; i < nameNodes.getLength(); i++) {
        Node nameNode = nameNodes.item(i);
        nameNode.setTextContent(title);
      }

      Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.transform(new DOMSource(document), new StreamResult(baos));
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Error replacing name tag content in XML", e);
      return new byte[0];
    }
  }
}
