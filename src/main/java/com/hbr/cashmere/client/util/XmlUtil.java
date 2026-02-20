package com.hbr.cashmere.client.util;

import java.io.File;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XmlUtil {

  private XmlUtil() {}

  /**
   * Removes all <img> tags from the given XML content and returns the cleaned content as a string.
   * 
   * @param xmlFile The XML content as a file
   * @return The cleaned XML content with all <img> tags removed
   */
  public static Document removeImages(File xmlFile) {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document document = documentBuilder.parse(xmlFile);
      document.getDocumentElement().normalize();
      // Remove <img> tags from HTML inside <ns6:content> elements
      NodeList contentNodes = document.getElementsByTagName("ns6:content");
      for (int i = 0; i < contentNodes.getLength(); i++) {
        Node contentNode = contentNodes.item(i);
        String html = contentNode.getTextContent();
        // Clean HTML using Jsoup
        org.jsoup.nodes.Document htmlDoc = Jsoup.parse(html);
        Elements images = htmlDoc.select("img");
        for (Element img : images) {
          img.remove();
        }
        // Replace content in XML as CDATA
        String cleanedHtml = htmlDoc.body().html();
        // Remove all children of contentNode
        while (contentNode.hasChildNodes()) {
          contentNode.removeChild(contentNode.getFirstChild());
        }
        org.w3c.dom.CDATASection cdata = document.createCDATASection(cleanedHtml);
        contentNode.appendChild(cdata);
      }

      // Optionally, also remove <img> tags that are direct XML children
      NodeList imgNodes = document.getElementsByTagName("img");
      while (imgNodes.getLength() > 0) {
        imgNodes.item(0).getParentNode().removeChild(imgNodes.item(0));
      }

      return document;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }



  /**
   * Saves the given XML Document to a file at the specified destination path.
   * 
   * @param document The XML Document to save
   * @param destinationPath The path where the XML file should be saved
   */
  public static void saveDocumentToFile(Document document, Path destinationPath) {
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.transform(new DOMSource(document), new StreamResult(destinationPath.toFile()));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
