package com.hbr.cashmere.transfer_service.util;

import jakarta.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
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

  private XmlUtil() {}

  /**
   * Extracts the <title> value from the XML byte array as a String.
   * @param xmlBytes The XML content as a byte array
   * @return The title as a String, or null if not found
   */
  public static String extractTitle(byte[] xmlBytes) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes)) {
      DocumentBuilder builder = FACTORY.newDocumentBuilder();
      Document doc = builder.parse(bais);
      String title;
      doc.getDocumentElement().normalize();
      NodeList titleNodes = doc.getElementsByTagName("ns6:title");
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
   * @param xmlBytes The XML content as a byte array
   * @return The authors as a String[], or empty array if not found
   */
  public static String[] extractAuthors(byte[] xmlBytes) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes)) {
      DocumentBuilder builder = FACTORY.newDocumentBuilder();
      Document doc = builder.parse(bais);
      doc.getDocumentElement().normalize();
      NodeList authorNodes = doc.getElementsByTagName("ns6:author");
      String[] authors = new String[authorNodes.getLength()];
      for (int i = 0; i < authorNodes.getLength(); i++) {
        Node authorNode = authorNodes.item(i);
        String name = null;
        NodeList children = authorNode.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
          Node child = children.item(j);
          if (
            child.getNodeType() == Node.ELEMENT_NODE &&
            "ns6:name".equals(child.getNodeName())
          ) {
            name = child.getTextContent().trim();
            break;
          }
        }
        authors[i] = name != null ? name : "";
      }
      return authors;
    } catch (Exception e) {
      log.error("Error extracting authors from XML", e);
    }
    return new String[0];
  }

  /**
   * Extracts the <published> value from the XML byte array as a java.util.Date.
   * @param xmlBytes The XML content as a byte array
   * @return The published date as a Date, or null if not found or parse error
   */
  public static Date extractPublishedDate(byte[] xmlBytes) {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes)) {
      DocumentBuilder builder = FACTORY.newDocumentBuilder();
      Document doc = builder.parse(bais);
      doc.getDocumentElement().normalize();
      NodeList publishedNodes = doc.getElementsByTagName("ns6:published");
      if (publishedNodes.getLength() > 0) {
        String publishedText = publishedNodes.item(0).getTextContent().trim();
        // Try parsing ISO 8601 format (e.g., 2024-06-01T12:00:00Z)
        try {
          return DatatypeConverter.parseDateTime(publishedText).getTime();
        } catch (Exception e) {
          // fallback: try yyyy-MM-dd
          try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(publishedText);
          } catch (Exception ignored) {}
        }
      }
    } catch (Exception e) {
      log.error("Error extracting published date from XML", e);
    }
    return null;
  }

  /**
   * Removes all <img> tags from the given XML content (as byte[]) and returns the cleaned XML as a byte array.
   *
   * @param xmlBytes The XML content as a byte array
   * @return The cleaned XML content as a byte array, or null if error
   */
  public static byte[] removeImages(byte[] xmlBytes) {
    try (
      ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes);
      ByteArrayOutputStream baos = new ByteArrayOutputStream()
    ) {
      DocumentBuilder builder = FACTORY.newDocumentBuilder();
      Document document = builder.parse(bais);
      document.getDocumentElement().normalize();
      // Remove <img> tags from HTML inside <ns6:content> elements
      NodeList contentNodes = document.getElementsByTagName("ns6:content");
      for (int i = 0; i < contentNodes.getLength(); i++) {
        Node contentNode = contentNodes.item(i);
        String html = contentNode.getTextContent();
        org.jsoup.nodes.Document htmlDoc = Jsoup.parse(html);
        Elements images = htmlDoc.select("img");
        for (Element img : images) {
          img.remove();
        }
        String cleanedHtml = htmlDoc.body().html();
        while (contentNode.hasChildNodes()) {
          contentNode.removeChild(contentNode.getFirstChild());
        }
        org.w3c.dom.CDATASection cdata = document.createCDATASection(
          cleanedHtml
        );
        contentNode.appendChild(cdata);
      }

      NodeList imgNodes = document.getElementsByTagName("img");
      while (imgNodes.getLength() > 0) {
        imgNodes.item(0).getParentNode().removeChild(imgNodes.item(0));
      }

      Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(
        "{http://xml.apache.org/xslt}indent-amount",
        "2"
      );
      transformer.transform(new DOMSource(document), new StreamResult(baos));
      return baos.toByteArray();
    } catch (Exception e) {
      log.error("Error at removing images from XML file", e);
      return new byte[0];
    }
  }
}
