/*
 * Copyright (c) 2016, Bart Hanssens <bart.hanssens@fedict.be>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package be.fedict.lodtools.skosifier;

import com.google.common.base.Charsets;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.CharEncoding;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

/**
 * Converts a CSV to a SKOS file and various RDF files
 * 
 * @author Bart.Hanssens
 */
public class Main {	
	private static List<String[]> rows;
	private static String[] header;
		
	private static String baseURI;
	
	private static final ValueFactory F = SimpleValueFactory.getInstance();
	private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy");

	private static final String START = "https://schema.org/startDate";
	private static final String END = "https://schema.org/endDate";
	
	private static final List LANGS = Arrays.asList(new String[]{ "nl", "fr", "de", "en" });
	private static final Map<String, IRI> PROPS = new HashMap();
	static {
		PROPS.put(OWL.SAMEAS.getLocalName().toLowerCase(), OWL.SAMEAS);
		PROPS.put(SKOS.EXACT_MATCH.getLocalName().toLowerCase(), SKOS.EXACT_MATCH);
		PROPS.put(SKOS.EXACT_MATCH.getLocalName().toLowerCase(), SKOS.CLOSE_MATCH);
		PROPS.put(SKOS.BROAD_MATCH.getLocalName().toLowerCase(), SKOS.BROAD_MATCH);
		PROPS.put(SKOS.NARROW_MATCH.getLocalName().toLowerCase(), SKOS.NARROW_MATCH);
	}
	
	/**
	 * Read CSV input file (using ; as separator).
	 * 
	 * The first line must contain the following column headers:
	 * - "ID": unique ID
	 * - "parent": parent ID (optional)
	 * - language tag (e.g. "nl", "fr", "de", "en" .... one language per column)
	 * - start date
	 * - end date
	 * 
	 * @param f CSV file
	 * @throws FileNotFoundException
	 * @throws IOException 
	 */
	private static void readCSV(File f) throws FileNotFoundException, IOException {
		Reader reader = new InputStreamReader(new FileInputStream(f), CharEncoding.UTF_8);
		try (CSVReader r = new CSVReader(reader, ';')) {
			rows = r.readAll();
			header = rows.remove(0);
		}
	}

	/**
	 * Write a single file
	 * 
	 * @param fmt RDF format
	 * @param f file to write to
	 */
	private static void writeFile(RDFFormat fmt, File f, Model m) throws IOException {
		Writer out = new OutputStreamWriter(new FileOutputStream(f), Charsets.UTF_8);
		
		RDFWriter w = Rio.createWriter(fmt, out);
		w.set(BasicWriterSettings.PRETTY_PRINT, true);
		w.handleNamespace(SKOS.PREFIX, SKOS.NAMESPACE);
		w.startRDF();
		m.forEach(w::handleStatement);
		w.endRDF();
	}

	/**
	 * Create and ID from a string
	 * 
	 * @param s string
	 * @return identifier
	 */
	private static String id(String s) {
		return s + "#id";
	}
	
	/**
	 * Write SKOS files to a directory.
	 * 
	 * @param dir top level directory
	 * @param fmt format
	 * @param ext file extension
	 */
	private static void writeSkos(File dir, RDFFormat fmt, String ext) 
			throws IOException {
		Model M = new LinkedHashModel();

		String vocab = baseURI.endsWith("/") 
				? baseURI.substring(0, baseURI.length()-1)
				: baseURI;	
		
		IRI scheme = F.createIRI(id(vocab));
		M.add(scheme, RDF.TYPE, SKOS.CONCEPT_SCHEME);
		
		for(String[] row: rows) {
			IRI node = F.createIRI(baseURI, id(row[0]));
		
			// parent or not
			if (row[1].isEmpty()) {
				M.add(node, SKOS.TOP_CONCEPT_OF, scheme);
				M.add(scheme, SKOS.HAS_TOP_CONCEPT, node);
			} else { 
				IRI parent = F.createIRI(baseURI, id(row[1]));
				M.add(node, SKOS.BROADER, parent);
				M.add(parent, SKOS.NARROWER, node);
			}
			
			M.add(node, RDF.TYPE, SKOS.CONCEPT);
			M.add(node, SKOS.IN_SCHEME, scheme);
			M.add(node, SKOS.NOTATION, F.createLiteral(row[0]));
			
			for (int i = 2; i < header.length; i++) {
				if (row[i].isEmpty()) {
					continue;
				}
				// labels in different languages
				if (LANGS.contains(header[i])) {
					Literal label = F.createLiteral(row[i], header[i]);
					M.add(node, SKOS.PREF_LABEL, label);
				}
				
				// optional start / end date
				if (header[i].equals("start")) {
					try {
						Date start = df.parse(row[i]);
						Literal date = F.createLiteral(start);
						M.add(node, F.createIRI(START), date);
					} catch (ParseException pe) {
						//
					}
				}
				if (header[i].equals("end")) {
					try {
						Date end = df.parse(row[i]);
						Literal date = F.createLiteral(end);
						M.add(node,  F.createIRI(END), date);
					} catch (ParseException pe) {
					//
					}
				}
				
				// Check skos exact/narrow match etc
				for (String prop: PROPS.keySet()) {
					if (header[i].startsWith(prop)) {
						IRI ref = F.createIRI(row[i]);
						M.add(node, PROPS.get(prop), ref);	
					}
				}
			}
		}
		
		File index = new File(dir, "skos." + ext);
		writeFile(fmt, index, M);
	}
	
	/**
	 * Write SKOS files, in different formats, to a directory.
	 * 
	 * @param dir top level directory 
	 * @throws java.io.IOException 
	 */
	public static void writeSkos(File dir) throws IOException {
		writeSkos(dir, RDFFormat.NTRIPLES, "nt");
		writeSkos(dir, RDFFormat.TURTLE, "ttl");
		writeSkos(dir, RDFFormat.JSONLD, "json");
	}
	
	/**
	 * Main
	 * 
	 * @param args 
	 */
	public static void main(String[] args) {
		if (args.length < 3) {
			System.err.println("Usage: SKOSifier <input.csv> <outputdir> <baseURI>");
			System.exit(-1);
		}
		
		baseURI = args[2];
		
		File f = new File(args[0]);
		try {
			readCSV(f);
		} catch (IOException ex) {
			System.err.println("Failed to read input file " + ex.getMessage());
			System.exit(-2);
		}
		
		File dir = new File(args[1]);
		if (! dir.exists()) {
			dir.mkdir();
		}
		
		try {
			writeSkos(dir);
		//	writeHTML(dir);
		} catch (IOException ex) {
			System.err.println("Failed to write output");
			System.exit(-3);
		}
	}
}
