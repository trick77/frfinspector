package com.trick77.inspector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Stack;

public class OdxHandler extends DefaultHandler {

    public Container container = new Container();

    public Stack<String> elementStack = new Stack<String>();

    public ArrayList<Block> blocks = new ArrayList<Block>();

    public ArrayList<FlashData> flashDatas = new ArrayList<FlashData>();

    private Stack<Object> objectStack  = new Stack<Object>();

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        elementStack.push(qName);

        if (elementStack.size() > 1 && currentElementParent().equals("DATABLOCKS") && currentElement().equals("DATABLOCK")) {
            Block block = new Block();
            block.setId(attributes.getValue(0));
            block.setType(attributes.getValue(1));
            blocks.add(block);
            objectStack.push(block);
        }

        if (elementStack.size() > 1 && currentElementParent().equals("SECURITYS") && currentElement().equals("SECURITY")) {
            Security security = new Security();
            objectStack.push(security);
        }

        if (elementStack.size() > 1 && currentElementParent().equals("FLASHDATAS") && currentElement().equals("FLASHDATA")) {
            FlashData flashData = new FlashData();
            if (attributes.getLength() > 1) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getLocalName(i).equalsIgnoreCase("id")) {
                        flashData.setId(attributes.getValue(i));
                        break;
                    }
                }
            } else {
                flashData.setId(attributes.getValue(0));
            }
            flashDatas.add(flashData);
            objectStack.push(flashData);
        }

    }

    public void endElement(String uri, String localName,
                           String qName) throws SAXException {
        this.elementStack.pop();
        if (qName.equals("DATABLOCK") || qName.equals("SECURITY") || qName.equals("FLASHDATA")) {
            objectStack.pop();
        }

    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
        if (elementStack.size() > 1 && currentElementParent().equals("FLASHDATA") && currentElement().equals("DATA")) {
            // Ignore all data, we can't decrypt it anyway
            return;
        }

        String value = new String(ch, start, length).trim();
        if (value.length() == 0) {
            return;
        }

        if (currentElementParent().equals("FLASH") && currentElement().equals("LONG-NAME")) {
            container.setName(value);
        }

        if (currentElementParent().equals("DOC-REVISION")) {
            if (currentElement().equals("DATE")) {
                container.setDate(value);
            }
            if (currentElement().equals("REVISION-LABEL")) {
                container.setRevision(value);
            }
        }

        if (currentElementParent().equals("IDENT-VALUES") && currentElement().equals("IDENT-VALUE")) {
            if (isNumeric(value)) {
                container.addVersionIdent(value);
            } else {
                container.addNameIdent(value);
            }
        }

        if (currentElementParent().equals("SECURITY")) {
            Security security = (Security) objectStack.peek();
            if (currentElement().equals("SECURITY-METHOD")) {
                security.setMethod(value);
            } else if (currentElement().equals("FW-SIGNATURE")) {
                security.setSignature(value);
                container.addSecurity(security);
            }
        }

        if (currentElementParent().equals("DATABLOCK") && currentElement().equals("LONG-NAME")) {
            Block block = (Block)objectStack.peek();
            block.setName(value);
        }

        if (currentElementParent().equals("SEGMENT") && currentElement().equals("COMPRESSED-SIZE")) {
            Block block = (Block)objectStack.peek();
            block.setCompressedSize(Integer.valueOf(value));
        }

        if (currentElementParent().equals("SEGMENT") && currentElement().equals("UNCOMPRESSED-SIZE")) {
            Block block = (Block)objectStack.peek();
            block.setUncompressedSize(Integer.valueOf(value));
        }

        if (currentElementParent().equals("FLASHDATA") && currentElement().equals("LONG-NAME")) {
            FlashData flashData = (FlashData) objectStack.peek();
            flashData.getName(value);
        }

        if (currentElementParent().equals("FLASHDATA") && currentElement().equals("ENCRYPT-COMPRESS-METHOD")) {
            FlashData flashData = (FlashData) objectStack.peek();
            flashData.setEncryptCompressMethod(value);
        }

    }

    private String currentElement() {
        if (this.elementStack.size() > 0) {
            return this.elementStack.peek();
        }
        return null;
    }

    private String currentElementParent() {
        if(this.elementStack.size() < 2) {
            return null;
        }
        return this.elementStack.get(this.elementStack.size() - 2);
    }


    public static boolean isNumeric(String strNum) {
        return strNum.matches("-?\\d+(\\.\\d+)?");
    }

}
