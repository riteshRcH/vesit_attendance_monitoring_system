package com.example.tryout;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.siemens.ct.exi.api.sax.EXIResult;
import com.siemens.ct.exi.api.sax.EXISource;

public class CodecXMLEXI
{
	static public void encodeXMLToEXISchemaLess(String xmlLocation, String exiLocation) throws Exception
	{
		// encode
		OutputStream exiOS = new FileOutputStream(exiLocation);
		EXIResult exiResult = new EXIResult();
		exiResult.setOutputStream(exiOS);
		
		ContentHandler ch = exiResult.getHandler();
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
		xmlReader.setContentHandler(ch);

		// parse xml file
		xmlReader.parse(new InputSource(xmlLocation));
		exiOS.close();
	}

	static public void decodeEXIToXMLSchemaLess(String exiLocation, String xmlLocation)throws Exception
	{
		XMLReader exiReader = new EXISource().getXMLReader();

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();

		InputStream exiIS = new FileInputStream(exiLocation);
		SAXSource exiSource = new SAXSource(new InputSource(exiIS));
		exiSource.setXMLReader(exiReader);

		StringWriter strWriter = new StringWriter();
		transformer.transform(exiSource, new StreamResult(strWriter));
		String xml = new String(strWriter.getBuffer());
		strWriter.close();
		
		FileWriter fw = new FileWriter(xmlLocation);
		fw.write(xml);
		fw.close();
	}
}
